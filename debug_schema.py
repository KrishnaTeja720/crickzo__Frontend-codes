import MySQLdb

def check_schema():
    try:
        db = MySQLdb.connect(host="127.0.0.1", user="root", passwd="", db="crickai_db")
        cur = db.cursor()
        
        print("--- MATCHES TABLE SCHEMA ---")
        cur.execute("DESCRIBE matches")
        for r in cur.fetchall():
            print(r)
            
        print("\n--- MATCHES FOR USER 3 DETAILS ---")
        cur.execute("SELECT id, user_id, team_a, team_b, status FROM matches WHERE user_id = 3")
        for r in cur.fetchall():
            print(f"ID={r[0]}, USER_ID={r[1]}, TYPE={type(r[1])}, TEAM={r[2]} vs {r[3]}, STATUS={r[4]}")
            
        db.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_schema()
