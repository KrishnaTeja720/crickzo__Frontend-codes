import requests

def test_endpoints():
    ports = [5000, 5005]
    for port in ports:
        url = f"http://127.0.0.1:{port}/match/scorecard/124"
        try:
            r = requests.get(url)
            print(f"Port {port}: /match/scorecard/124 -> status {r.status_code}")
            print(f"Body: {r.text[:200]}")
        except Exception as e:
            print(f"Port {port}: /match/scorecard/124 -> ERROR: {e}")

if __name__ == "__main__":
    test_endpoints()
