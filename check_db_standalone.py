import mysql.connector

try:
    conn = mysql.connector.connect(
        host="127.0.0.1",
        user="root",
        password="",
        database="crickai_db"
    )
    cursor = conn.cursor()
    
    print("--- MATCHES ---")
    cursor.execute("SELECT id, team_a, team_b, status, user_id FROM matches")
    for r in cursor.fetchall():
        print(r)
        
    print("\n--- USERS ---")
    cursor.execute("SELECT id, name, email FROM users")
    for u in cursor.fetchall():
        print(u)
        
    conn.close()
except Exception as e:
    print(f"Error: {e}")
