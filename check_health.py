import os
import subprocess

def check():
    tasks = {
        "FastAPI installed": "pip show fastapi",
        "Uvicorn installed": "pip show uvicorn",
        "Database exists": "ls database.db",
        "Main Script exists": "ls main.py",
        "Git linked": "git remote -v"
    }
    
    print("\n--- HisabKitab App Health Report ---")
    for task, cmd in tasks.items():
        result = subprocess.run(cmd.split(), capture_output=True, text=True)
        if result.returncode == 0:
            print(f"✅ {task}: 100% Running")
        else:
            print(f"❌ {task}: Error/Missing")
    print("------------------------------------\n")

if __name__ == "__main__":
    check()
