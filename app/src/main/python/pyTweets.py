from apify_client import ApifyClient


def getTweets(uname):


run_input = {
    "searchTerms": [
        "Cricket"
    ],
    "maxItems": 50,  # Maximum number of items to retrieve
    "sort": "Latest",  # Sort results by latest tweets
    "tweetLanguage": "en",  # Filter by English tweets
}


print("startedApify")
run = client.actor("apidojo/tweet-scraper").call(run_input=run_input)
print("endApify")

for item in client.dataset(run["defaultDatasetId"]).iterate_items():
    print(f"Tweet Text: {item.get('full_text')}")
print(f"Author: {item.get('author_screen_name')}")
print(f"Likes: {item.get('favorite_count')}")
print("-" * 30)

return item