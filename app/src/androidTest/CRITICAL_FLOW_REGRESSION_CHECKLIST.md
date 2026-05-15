# Critical Flow Regression Checklist

This checklist is mandatory before any release and after any change touching sync, billing, reminders, security, or navigation.

## Sacred Customer + Billing Flows

- Add a customer and open customer ledger.
- Add `Given` entry and verify ledger total updates correctly.
- Add `Received` entry and verify ledger total updates correctly.
- Create bill from ledger and verify bill transaction is saved.
- Share bill via WhatsApp and verify send path returns success.

## Reminder Flows

- Manual reminder from customer ledger sends successfully.
- Customer reminder history shows the newly sent reminder.
- Reminder control toggles update state without app restart.

## Sync Durability

- Add entries offline, restart app, verify pending queue is still present.
- Trigger sync, verify pending decreases or explicit failed count appears.
- Confirm failed sync items are not dropped silently.

## Security and Data Integrity

- App opens database successfully after cold start.
- Set/verify security PIN flow completes without crash.
- Run migration tests for `36 -> 37` and `33 -> 37`.
- Verify backup toggle path and sync status string are visible in settings.

## Theme and UI Consistency (Non-functional)

- Switch all premium themes and verify customer list, dashboard, settings, and AI screens are readable.
- Validate that semantic colors remain clear for due amounts and status chips.

