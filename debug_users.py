import requests

def test_login():
    url = "http://127.0.0.1:5005/login"
    payload = {
        "email": "surendra@gmail.com",
        "password": "p" # I noticed in previous logs that 'p' was used sometimes for test users
    }
    try:
        # We don't know the password, but we can try to guess or use another user
        # Actually, let's just use the query instead.
        pass

    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    # Instead of login, let's just check the DB mapping again for ID 4
    import mysql.connector
    
    # Using config values from previous viewed files
    db_config = {
        "host": "localhost",
        "user": "root",
        "password": "",
        "database": "crickai_db"
    }
    
    conn = mysql.connector.connect(**db_config)
    cur = conn.cursor()
    
    print("--- User Mapping ---")
    cur.execute("SELECT id, name, email FROM users")
    for row in cur.fetchall():
        print(row)
    
    print("\n--- Matches for user ID 4 ---")
    cur.execute("SELECT id, team_a, team_b, status FROM matches WHERE user_id = 4")
    for row in cur.fetchall():
        print(row)
    
    cur.close()
    conn.close()
