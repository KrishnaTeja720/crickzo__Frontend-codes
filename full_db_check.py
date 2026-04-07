import MySQLdb

def check_db():
    try:
        # Standard configuration for the user's project
        conn = MySQLdb.connect(
            host='127.0.0.1',
            user='root',
            passwd='',
            db='crickai_db'
        )
        cur = conn.cursor()
        cur.execute("SELECT id, team_a, team_b, status, user_id FROM matches")
        rows = cur.fetchall()
        print(f"Total Matches in DB: {len(rows)}")
        for r in rows:
            print(f"MatchID: {r[0]}, {r[1]} vs {r[2]}, status: {r[3]}, user_id: {r[4]}")
        cur.close()
        conn.close()
    except Exception as e:
        print(f"DB Error: {e}")

if __name__ == "__main__":
    check_db()
