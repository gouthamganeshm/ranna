# Verification — N× Lab 0.1.0

## Completed
- Compiled the actual Java sources against Android SDK 35.
- 35 pure Java checks passed: static/fixed QR parsing, metadata preservation, malformed/duplicate fields, unsupported currency/scheme, control characters, amount bounds/precision and conservative callback classification.
- Built DEX and packaged APK with ZXing 3.5.3.
- APK signature verified with v2 and v3 schemes; alignment check passed.
- Packaged manifest inspected: `in.nxprototype.app`, version code 1, min API 26, target API 35, CAMERA is the only requested permission.
- Source inspection: no PIN UI/storage, no raw callback/QR logging, no internet/SMS/accessibility/overlay permissions, no automatic next-payment path, persisted unresolved lock committed before handoff, app-private logs and backup disabled.
- Compilation reports use of deprecated Android Camera/activity-result interfaces. These are deliberate platform-only prototype choices; real-device compatibility remains to be tested.

## Not performed
- No emulator or physical-phone execution.
- No camera hardware test, bank payment, settlement check or merchant reconciliation.
- No Play Store submission, regulatory approval or single-PIN batch verification.

## Device test sequence (M1)
1. Install APK and launch. Console should show APP_OPEN / APP_RESUME.
2. Deny camera permission once: app should return to the payment screen with a diagnostic event. Test image import as fallback.
3. Import a non-QR image: expect an actionable decode-failure message.
4. Scan/import an agreeing merchant's QR. Confirm displayed address; use ₹1 for an editable QR.
5. Review, choose your UPI app, verify the recipient and amount there, and approve normally.
6. Return to N× Lab. Check callback status. If no result, it must remain unknown rather than claiming failure or success.
7. Check actual UPI history and merchant receipt. Record the checked outcome only when known.
8. On a separate attempt, cancel in the UPI app and verify our app does not silently retry.
9. If an attempt is unresolved, close/reopen N× Lab: the payment lock must remain. Do not uninstall/clear app data as a substitute for reconciliation.
10. Save/share the `.txt` console and attach it in the conversation. Describe any failed step and the real payment outcome.

Stop at M1. Repeat-payment convenience and N× execution are separate subsequent milestones.

## 0.1.1 verification
See RELEASE-0.1.1.md. 35 payment checks and 12 QR decoder checks passed; no physical-device or live-payment test.
