import requests

def test_api():
    base_url = "http://127.0.0.1:5005"
    try:
        # Test Live Matches for User 3 (Surendra)
        print("--- Testing Live Matches for User 3 ---")
        res = requests.get(f"{base_url}/matches/live?user_id=3")
        print(f"Status: {res.status_code}")
        print(f"Response: {res.json()}")
        
        # Test Upcoming Matches for User 3
        print("\n--- Testing Upcoming Matches for User 3 ---")
        res = requests.get(f"{base_url}/matches/upcoming?user_id=3")
        print(f"Status: {res.status_code}")
        print(f"Response: {res.json()}")

        # Test Live Matches for User 4 (Krishna)
        print("\n--- Testing Live Matches for User 4 ---")
        res = requests.get(f"{base_url}/matches/live?user_id=4")
        print(f"Status: {res.status_code}")
        print(f"Response: {res.json()}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    test_api()
