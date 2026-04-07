import requests

def test_creation_id():
    url = "http://127.0.0.1:5005/match/create"
    payload = {
        "user_id": 3,
        "team_a": "TEST_3_A",
        "team_b": "TEST_3_B",
        "format": "20",
        "venue": "Test Venue",
        "toss": "TEST_3_A",
        "toss_decision": "Batting",
        "pitch": "Balanced",
        "weather": "Normal"
    }
    
    try:
        response = requests.post(url, json=payload)
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
        
        # Verify in DB
        import MySQLdb
        db = MySQLdb.connect(host="127.0.0.1", user="root", passwd="", db="crickai_db")
        cur = db.cursor()
        cur.execute("SELECT id, user_id FROM matches WHERE team_a = 'TEST_3_A'")
        res = cur.fetchone()
        print(f"Verified Match Owner: {res}")
        db.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    test_creation_id()
