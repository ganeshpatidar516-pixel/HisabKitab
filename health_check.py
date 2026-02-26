import os
import sqlite3
import requests

def check_status():
    print("\n=== 📋 HISABKITAB PROJECT HEALTH REPORT ===\n")

    # 1. File Structure Check
    required_files = ['main.py', 'hisabkitab.db', 'requirements.txt', '.env']
    print("--- [1] File Status ---")
    for file in required_files:
        if os.path.exists(file):
            print(f"✅ {file}: CLEAR")
        else:
            print(f"❌ {file}: MISSING (UNCLEAR)")

    # 2. Database Integrity Check
    print("\n--- [2] Database Status ---")
    try:
        conn = sqlite3.connect('hisabkitab.db')
        cursor = conn.cursor()
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='bills';")
        table_exists = cursor.fetchone()
        if table_exists:
            cursor.execute("SELECT COUNT(*) FROM bills;")
            count = cursor.fetchone()[0]
            print(f"✅ Table 'bills': CLEAR (Total Records: {count})")
        else:
            print("❌ Table 'bills': NOT FOUND (UNCLEAR)")
        conn.close()
    except Exception as e:
        print(f"❌ Database Error: {e}")

    # 3. Server API Check
    print("\n--- [3] Live Server Status ---")
    try:
        response = requests.get("http://127.0.0.1:8000/", timeout=2)
        if response.status_code == 200:
            print("✅ FastAPI Server: LIVE & CLEAR")
        else:
            print(f"⚠️ FastAPI Server: RESPONDING WITH CODE {response.status_code}")
    except:
        print("❌ FastAPI Server: NOT RUNNING (Run 'uvicorn main:app' in Session 1)")

    print("\n==========================================\n")

if __name__ == "__main__":
    check_status()
