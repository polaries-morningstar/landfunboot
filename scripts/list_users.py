import mysql.connector

try:
    conn = mysql.connector.connect(
        host="lfbgs.hbjk.com.cn",
        user="root",
        password="mysql@zhy2025!",
        port=13306,
        database="landfunboot",
    )
    cursor = conn.cursor()
    cursor.execute("SELECT id, username, email FROM sys_user")
    rows = cursor.fetchall()
    for row in rows:
        print(row)
    conn.close()
except Exception as e:
    print(f"Error: {e}")
