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

    print("--- User Table Columns ---")
    cursor.execute("DESCRIBE sys_user")
    for row in cursor.fetchall():
        print(row)

    print("\n--- User Data (id, username, email, is_active, delete_time) ---")
    cursor.execute("SELECT id, username, email, is_active, delete_time FROM sys_user")
    for row in cursor.fetchall():
        print(row)

    conn.close()
except Exception as e:
    print(f"Error: {e}")
