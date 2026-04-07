import MySQLdb

def check_db():
    try:
        conn = MySQLdb.connect(host='127.0.0.1', user='root', passwd='', db='crickai_db')
        cur = conn.cursor()
        
        print("--- USERS ---")
        cur.execute("SELECT id, name, email FROM users")
        for u in cur.fetchall():
            print(f"ID: {u[0]}, Name: {u[1]}, Email: {u[2]}")
            
        print("\n--- MATCHES ---")
        cur.execute("SELECT id, team_a, team_b, status, user_id FROM matches")
        for m in cur.fetchall():
            print(f"MatchID: {m[0]}, {m[1]} vs {m[2]}, status: {m[3]}, user_id: {m[4]}")
            
        cur.close()
        conn.close()
    except Exception as e:
        print(f"DB Error: {e}")

if __name__ == "__main__":
    check_db()
