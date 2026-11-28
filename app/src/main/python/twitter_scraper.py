import sys
import os
import json
import re
import html
import urllib.request
import urllib.parse
import xml.etree.ElementTree as ET
from types import ModuleType

# ==============================================================================
# Chaquopy Multiprocessing Compatibility Patch for Android
# ==============================================================================

if "_multiprocessing" not in sys.modules:
    _mp = ModuleType("_multiprocessing")
    _mp.SEM_VALUE_MAX = 2147483647
    sys.modules["_multiprocessing"] = _mp

try:
    import multiprocessing.dummy as dummy

    def android_cpu_count():
        try:
            return os.cpu_count() or 4
        except Exception:
            return 4

    dummy.cpu_count = android_cpu_count
    dummy.get_context = lambda *a, **k: dummy
    dummy.get_all_start_methods = lambda: ['fork']
    dummy.get_start_method = lambda *a, **k: 'fork'
    dummy.set_start_method = lambda *a, **k: None
    dummy.freeze_support = lambda: None
    dummy.TimeoutError = TimeoutError

    sys.modules["multiprocessing"] = dummy
    sys.modules["multiprocessing.pool"] = dummy
except Exception:
    pass

def get_ssl_context():
    try:
        import ssl
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        return ctx
    except Exception:
        return None

def clean_tweet_text(raw_text):
    if not raw_text:
        return ""
    # Strip HTML tags
    text = re.sub(r'<[^>]+>', '', raw_text)
    # Unescape HTML entities
    text = html.unescape(text)
    # Clean up whitespace
    text = re.sub(r'\s+', ' ', text).strip()
    return text

def fetch_tweets_via_rss2json(username, count=1000):
    """
    Fetches public tweets via RSS-to-JSON service for Nitter / RSSHub streams.
    """
    rss_urls = [
        f"https://nitter.privacydev.net/{username}/rss",
        f"https://nitter.poast.org/{username}/rss",
        f"https://rsshub.app/twitter/user/{username}",
    ]
    ctx = get_ssl_context()
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
    }

    for rss_u in rss_urls:
        api_url = f"https://api.rss2json.com/v1/api.json?rss_url={urllib.parse.quote(rss_u)}"
        try:
            req = urllib.request.Request(api_url, headers=headers)
            with urllib.request.urlopen(req, context=ctx, timeout=7) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                if data.get("status") == "ok":
                    feed = data.get("feed", {})
                    avatar_url = feed.get("image") or f"https://unavatar.io/twitter/{username}"
                    items = data.get("items", [])
                    tweets = []
                    for item in items:
                        raw = item.get("description") or item.get("title") or ""
                        cleaned = clean_tweet_text(raw)
                        if cleaned and cleaned not in tweets and not cleaned.startswith("RT by @"):
                            tweets.append(cleaned)
                    if tweets:
                        return {"avatar": avatar_url, "tweets": tweets[:count]}
        except Exception:
            continue
    return None

def fetch_tweets_from_rss_direct(username, count=1000):
    """
    Directly fetches XML RSS from public mirrors.
    """
    rss_urls = [
        f"https://nitter.privacydev.net/{username}/rss",
        f"https://nitter.poast.org/{username}/rss",
        f"https://nitter.freedit.eu/{username}/rss",
        f"https://nitter.cz/{username}/rss",
    ]
    ctx = get_ssl_context()
    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}

    for url in rss_urls:
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, context=ctx, timeout=6) as response:
                xml_data = response.read().decode('utf-8', errors='ignore')

            root = ET.fromstring(xml_data)
            channel = root.find('channel')
            if channel is None:
                continue

            tweet_list = []
            avatar_url = ""

            image = channel.find('image')
            if image is not None:
                url_elem = image.find('url')
                if url_elem is not None and url_elem.text:
                    avatar_url = url_elem.text

            for item in channel.findall('item'):
                desc = item.find('description')
                title = item.find('title')
                raw_text = desc.text if (desc is not None and desc.text) else (title.text if title is not None else "")
                if raw_text:
                    clean_text = clean_tweet_text(raw_text)
                    if clean_text and clean_text not in tweet_list:
                        tweet_list.append(clean_text)

            if tweet_list:
                return {"avatar": avatar_url, "tweets": tweet_list[:count]}
        except Exception:
            continue
    return None

def fetch_avatar_from_fxtwitter(username):
    url = f"https://api.fxtwitter.com/{username}"
    ctx = get_ssl_context()
    headers = {"User-Agent": "Mozilla/5.0"}
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, context=ctx, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            if data.get("code") == 200:
                user = data.get("user", {})
                avatar = user.get("avatar_url") or f"https://unavatar.io/twitter/{username}"
                return avatar
    except Exception:
        pass
    return f"https://unavatar.io/twitter/{username}"

def get_user_tweets(username, count=1000):
    """
    Fetches public tweets for a given username.
    First tries RSS-to-JSON service, then direct XML RSS, then returns fallback status.
    """
    clean_username = username.strip().lstrip('@')
    if not clean_username:
        clean_username = "Fact"

    req_count = int(count) if count and int(count) > 0 else 1000

    try:
        # Method 1: RSS2JSON
        res = fetch_tweets_via_rss2json(clean_username, count=req_count)
        if res and res.get("tweets"):
            return res

        # Method 2: Direct RSS XML
        res = fetch_tweets_from_rss_direct(clean_username, count=req_count)
        if res and res.get("tweets"):
            return res

        # Method 3: Return avatar & error message
        avatar = fetch_avatar_from_fxtwitter(clean_username)
        return {"avatar": avatar, "tweets": [f"No tweets found for @{clean_username}."]}
    except Exception as e_run:
        avatar = f"https://unavatar.io/twitter/{clean_username}"
        return {"avatar": avatar, "tweets": [f"Error fetching tweets: {str(e_run)}"]}
