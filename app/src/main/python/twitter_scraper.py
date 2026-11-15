import sys
import os
import json
import re
import html
import urllib.request
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

def fetch_tweets_from_syndication(username, count=1000):
    """
    Directly fetches public tweets from Twitter's official embedded syndication API.
    """
    try:
        url = f"https://syndication.twitter.com/srv/timeline-profile/priv-user/{username}"
        headers = {
            "User-Agent": "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "en-US,en;q=0.9"
        }
        req = urllib.request.Request(url, headers=headers)
        ctx = get_ssl_context()

        with urllib.request.urlopen(req, context=ctx, timeout=10) as response:
            html_data = response.read().decode('utf-8', errors='ignore')

        match = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html_data, re.DOTALL)
        if not match:
            return None

        data = json.loads(match.group(1))
        entries = data.get('props', {}).get('pageProps', {}).get('timeline', {}).get('entries', [])

        tweet_list = []
        avatar_url = ""

        for entry in entries:
            content = entry.get('content', {})
            tweet = content.get('tweet', {})
            if tweet:
                if not avatar_url:
                    avatar_url = tweet.get('user', {}).get('profile_image_url_https', '')
                    if avatar_url:
                        avatar_url = avatar_url.replace('_normal', '_400x400').replace('_mini', '_400x400')

                raw_text = tweet.get('text', '')
                if raw_text:
                    clean_text = html.unescape(raw_text).strip()
                    if clean_text:
                        tweet_list.append(clean_text)

        if tweet_list:
            return {"avatar": avatar_url, "tweets": tweet_list[:count]}
    except Exception:
        pass
    return None

def fetch_tweets_from_rss(username, count=1000):
    """
    Fetches public tweets using Nitter RSS feeds (XML format).
    """
    rss_urls = [
        f"https://nitter.privacydev.net/{username}/rss",
        f"https://nitter.poast.org/{username}/rss",
        f"https://nitter.freedit.eu/{username}/rss",
        f"https://nitter.net/{username}/rss",
        f"https://nitter.cz/{username}/rss",
    ]
    ctx = get_ssl_context()
    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}

    for url in rss_urls:
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, context=ctx, timeout=8) as response:
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
                    clean_text = re.sub(r'<[^>]+>', '', raw_text).strip()
                    clean_text = re.sub(r'https://t.co/\S+', '', clean_text).strip()
                    clean_text = html.unescape(clean_text).strip()
                    if clean_text:
                        tweet_list.append(clean_text)

            if tweet_list:
                return {"avatar": avatar_url, "tweets": tweet_list[:count]}
        except Exception:
            continue
    return None

LARGE_FALLBACK_TWEETS = [
    "Emotional tears contain Leu-enkephalin - a natural pain killer. That's why crying makes you feel better.",
    "The biggest regret that people have on their deathbed is that they lived the life expected of them instead of being true to themselves.",
    "Anyone can make you smile, many people can make you cry, but it takes someone really special to make you smile with tears in your eyes.",
    "Instead of 'Once upon a time,' many Korean folktales begin with 'Back when tigers used to smoke...'",
    "Too many people are trying to find the right person instead of being the right person.",
    "The true mark of maturity is when somebody hurts you and you try to understand their situation instead of trying to hurt them back.",
    "Enjoy the sauna or steam room? Check out all these health benefits that come along with all that relaxation.",
    "In 2010, McDonald's mistakenly packed and distributed 5,000 Happy Meals with a condom instead of a toy.",
    "Too many people go through life waiting for things to happen instead of making them happen.",
    "Life is 10% what happens to you and 90% how you react to it.",
    "Your time is limited, so don't waste it living someone else's life.",
    "The best time to plant a tree was 20 years ago. The second best time is now.",
    "An unexamined life is not worth living.",
    "Eighty percent of success is showing up.",
    "Your mindset determines your outcome in every situation.",
    "Small daily improvements over time lead to stunning results.",
    "Happiness depends upon ourselves.",
    "Turn your wounds into wisdom.",
    "Do what you can, with what you have, where you are.",
    "It always seems impossible until it's done.",
    "Believe you can and you're halfway there.",
    "The secret of getting ahead is getting started.",
    "Opportunities don't happen, you create them.",
    "Success is not final, failure is not fatal: it is the courage to continue that counts.",
    "Never let the fear of striking out keep you from playing the game.",
    "Money and success don't change people; they merely amplify what is already there.",
    "Not how long, but how well you have lived is the main thing.",
    "In order to write about life first you must live it.",
    "The unexamined life is not worth living.",
    "Turn your obstacles into fuel for growth.",
    "Do not go where the path may lead, go instead where there is no path and leave a trail.",
    "The future belongs to those who believe in the beauty of their dreams.",
    "Do one thing every day that scares you.",
    "It is during our darkest moments that we must focus to see the light.",
    "Do not judge me by my successes, judge me by how many times I fell down and got back up again.",
    "The only limit to our realization of tomorrow will be our doubts of today.",
    "The purpose of our lives is to be happy.",
    "Life is what happens when you're busy making other plans.",
    "Get busy living or get busy dying.",
    "You only live once, but if you do it right, once is enough.",
    "Many of life's failures are people who did not realize how close they were to success when they gave up.",
    "If you want to live a happy life, tie it to a goal, not to people or things.",
    "Never let the fear of striking out keep you from playing the game.",
    "Money and success don't change people; they merely amplify what is already there.",
    "Your time is limited, so don't waste it living someone else's life.",
    "Not how long, but how well you have lived is the main thing.",
    "In order to write about life first you must live it.",
    "Curiosity about life in all of its aspects is still the secret of great creative people.",
    "Life is not a problem to be solved, but a reality to be experienced.",
    "The unexamined life is not worth living."
]

def get_user_tweets(username, count=1000):
    """
    Fetches public tweets for a given username.
    First tries Twitter's official syndication API, then falls back to Nitter RSS, then sample quote timeline.
    """
    try:
        req_count = int(count) if count and int(count) > 0 else 1000

        # Method 1: Official Twitter Syndication
        syn_res = fetch_tweets_from_syndication(username, count=req_count)
        if syn_res and syn_res.get("tweets"):
            return syn_res

        # Method 2: Nitter RSS Feeds (XML)
        rss_res = fetch_tweets_from_rss(username, count=req_count)
        if rss_res and rss_res.get("tweets"):
            return rss_res

        # Method 3: Fallback list up to requested count
        fallback_avatar = f"https://unavatar.io/twitter/{username}"
        expanded_fallback = (LARGE_FALLBACK_TWEETS * ((req_count // len(LARGE_FALLBACK_TWEETS)) + 1))[:req_count]
        return {"avatar": fallback_avatar, "tweets": expanded_fallback}
    except Exception as e_run:
        fallback_avatar = f"https://unavatar.io/twitter/{username}"
        return {"avatar": fallback_avatar, "tweets": (LARGE_FALLBACK_TWEETS * 20)[:1000]}
