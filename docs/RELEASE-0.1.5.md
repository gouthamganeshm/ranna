# 0.1.5 — CRED sequential payments

## Implemented
- Two or three equal payments to one static-QR recipient, with total review.
- CRED-only queue; existing single-payment chooser preserved.
- User reviews and opens each payment. Receipt confirmation advances progress; callbacks alone never advance it.
- Atomic private persistence of attempt lock before launch. Queue survives restart without automatically launching anything. Stop preserves unresolved attempt lock.
- Confirmed not paid ends the queue; uncertain outcomes remain locked.
- Queue URI deleted on completion/stop. Backup remains disabled. No PIN handling, accessibility, screen recording, network interception or new permissions.
- Repeat QR allowlist: pa, pn, cu, mc. Amount/order/reference/other metadata rejected for repeat mode. Single payments retain existing QR support.
- Logs: QUEUE_CREATED, QUEUE_PAYMENT_LAUNCH, QUEUE_RESTORED, QUEUE_READY, QUEUE_COMPLETE, QUEUE_STOPPED, QUEUE_PAUSED, alongside existing handoff events. No raw payment data in logs.

## Evidence and limitations
User's 0.1.4 device log showed all six installed handlers and a CRED REPORTED_SUCCESS callback after 25.5 seconds. User reported successful payment. Device log also confirmed a camera decode. This does not independently verify settlement.

0.1.5 adds 17 pure Java queue checks to existing payment/discovery/QR checks. Compilation and APK signature verification passed. Device UI, persisted restart behaviour and two actual sequential payments still need user validation. Tests do not emulate Android lifecycle or CRED. Existing deprecated Android APIs produce compiler warnings.

This is a user-stepped queue, not unattended N×, PIN-once authentication, screen observation or an MDR exemption claim.

## Device acceptance
1. Install over 0.1.4; verify 0.1.5 in header.
2. Resolve earlier payment only after receipt check.
3. Import basic static QR, enter ₹1, choose two CRED payments (₹2 total).
4. Approve first in CRED, return; next must stay disabled until Received is recorded.
5. Tap Review next CRED payment, approve second, confirm receipt; queue must complete.
6. Export TXT log. Report whether two separate receipts exist.
7. Optional without further payment: create a queue then restart app; it should offer review, never auto-launch. Stop it to clear.
