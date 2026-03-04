# app/services/scheduler_service.py

from apscheduler.schedulers.background import BackgroundScheduler
from datetime import datetime
from zoneinfo import ZoneInfo


class SchedulerService:

    def __init__(self):
        # Explicit timezone to avoid Termux timezone errors
        self.scheduler = BackgroundScheduler(timezone=ZoneInfo("UTC"))

    def start(self):
        if not self.scheduler.running:
            self.scheduler.start()
            print("Scheduler started")

    def shutdown(self):
        if self.scheduler.running:
            self.scheduler.shutdown()
            print("Scheduler stopped")

    def add_test_job(self):
        """
        Test job to confirm scheduler is working
        """
        self.scheduler.add_job(
            self.test_task,
            "interval",
            seconds=30,
            id="test_job",
            replace_existing=True
        )

    @staticmethod
    def test_task():
        print(f"[Scheduler Running] Time: {datetime.utcnow()}")
