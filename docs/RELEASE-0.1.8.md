# 0.1.8 — Recordable own screens

## Change
`UiFlags.ALLOW_SCREEN_RECORDING` (default true) gates the `FLAG_SECURE` call on all three of our own
windows: `MainActivity`, `ScanActivity` and `AssistedSplitActivity`. With the flag true those screens
omit `FLAG_SECURE`, so Android's screen recorder and screenshot capture work on them. Setting the
constant to false restores the previous blocking behaviour with no other edit.

No payment, split, navigation, consent, logging or persistence logic changed in this release.

## Why
Device validation of 0.1.7 is still open. The outstanding checks — accessible control semantics,
fingerprint landing, and whether a CRED success callback actually advances the next part — are
easier to evidence with a recording of our own screens alongside the exported TXT log.

## Scope and limits of the recording
Recording covers our screens only: QR review, split plan dialog, amount and consent UI, live console,
and the per-part state text. It does not defeat another application's protection. CRED sets
`FLAG_SECURE` on its own authentication surfaces, so a recorded split run shows our screen, then a
black segment through each authentication, then our screen again. The 30.6-second reference video
supplied by the owner shows exactly that black segment. `ASSIST_CHECK` and `ASSIST_PAUSED` events
remain the only view into what the navigation helper observed on those protected screens.

## Security note
This weakens a previously documented property: our screens can now be captured by any app or user
with recording permission, and a recording may include the payee name, payee address and amounts
shown on our review screens. Logs are unaffected and still exclude those fields. Turn the flag off
before any use outside owner device testing.

## Validation
All seven JVM suites pass (234 checks). APK rebuilt and signed with the same development key;
v2 and v3 signature schemes verify. No emulator or phone execution performed here. Screen-recording
behaviour itself is a device-level check and remains unverified until the owner records a run.

## Outstanding phone checks
Unchanged from 0.1.7, plus: confirm Android's recorder now captures our screens, and confirm the CRED
segment still records black.
