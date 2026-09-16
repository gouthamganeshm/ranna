# N× Lab — Android prototype plan

Version 0.1.0 • 16 September 2026

## Goal and iteration contract
Build and validate an Android payment wrapper one feature at a time, using the owner's phone and exported diagnostic logs. The intended progression is one QR payment, repeat payments, then N× orchestration. Single-PIN unattended execution is a research objective, not a capability established by this first build.

Each iteration delivers an APK with a distinct version, source snapshot, verification notes and phone test checklist. The user returns the exported log, phone/Android version, selected UPI app and an observation of the actual payment outcome. Fix the current milestone before moving to the next. Never infer bank settlement solely from a client callback or a user-interface timeout.

## EasyUpiPayment evaluation before implementation
Inspected upstream commit `53862e27505ef29365152129c4a583a6d900ba76`:
https://github.com/PatilShreyas/EasyUpiPayment-Android

`PaymentUiActivity.kt` constructs a `upi://pay` URI, opens a selected UPI app via `ACTION_VIEW` / `startActivityForResult`, and parses the `response` extra on return. Useful concepts are the app selector, Android package-visibility declaration, payment metadata and status listener.

It has no PIN retrieval, replay, reusable authentication token or N× mandate API. A repeated intent remains a fresh payment request handled by the receiving UPI app. Its parser assumes particular field spelling, maps absent status to failure, and interprets missing callback data as cancellation. We must not inherit those assumptions.

The repository is archived (November 2023), Apache-2.0 licensed. This prototype independently implements the generic Android handoff; it does not embed or copy the archived library. Current Google documentation requires PSP verification even for a submitted/succeeded client response:
https://developers.google.com/pay/india/api/android/in-app-payments

## Milestones

### M1 — one QR payment and diagnostics (current delivery)
- Android 8.0 / API 26 or newer; target API 35.
- Scan a QR using camera; import a QR image using the system picker; paste UPI QR text as a fallback.
- Decode entirely on-device; no image upload or internet permission.
- Accept INR `upi://pay` QRs with a valid payee address; reject duplicate fields, control characters, unsupported schemes and signed QRs.
- Preserve existing merchant/order metadata. Do not fabricate an order reference or change a fixed QR amount.
- First-build amount cap ₹2,000; amounts use decimal arithmetic, two decimal places, positive values only. Recommended first live test ₹1 with an agreeing merchant.
- Show QR name/address and amount for review. QR name is unverified; user checks the bank-resolved recipient in the UPI app.
- List installed applications that resolve the UPI intent. Explicit user selection and approval for every payment.
- Never collect a UPI PIN. PIN/biometric approval occurs in the chosen UPI app.
- Persist an unresolved-attempt lock before handoff; prevent concurrent/double-tap sends. Failure to persist means do not launch.
- Classify callbacks as reported success, reported failure, pending or unknown. Empty, malformed or duplicate-status callbacks are unknown.
- A return code, reference number, timeout or lack of callback is not settlement evidence.
- The user records a checked outcome to clear an attempt. This is labelled user observation, not verified settlement. QR must be scanned again for a new payment in M1.
- Survive process restart with the unresolved lock retained; never retry automatically.
- Live on-screen console with bounded local storage; share text, save `.txt`, clear logs. Logs survive restart.
- Log version, API level, event time, random local attempt ID, selected app package, lifecycle, known callback status/code and sanitised exception class/own stack frames.
- Do not log raw QRs, images, payee names/addresses, amounts, PINs, OTPs, bank account numbers, bank transaction references, raw responses or exception messages.
- Export header includes phone manufacturer/model for diagnosis. No serial number, phone number or advertising identifier.
- Console observes our app only. No overlay, AccessibilityService, SMS access or device-wide logcat harvesting.
- Camera denial/no app/malformed QR/image failure produce actionable messages.

M1 acceptance on phone: scan/import works; right merchant and amount appear in UPI app; user completes one approved payment; merchant confirms receipt; exported log has relevant events and no secrets; an unresolved attempt survives restart and cannot be resent silently.

### M2 — explicit repeat payments (not implemented)
Retain a validated static merchant QR; add a new reviewed payment with a new local attempt ID. Each debit still needs UPI-app approval. Show a persistent ledger of submitted, reported, user-confirmed and unresolved attempts. Do not reuse fixed order references for a different order. Test two small payments and cancel/pending/restart cases; no unattended loops.

### M3 — N× coordinator (not implemented)
Plan total and instalments using integer paise. Bind user consent to payee, total, maximum count, expiry and payment route. One in-flight debit at a time. Persist intent before submission, stop on uncertain outcome or credential error, never automatically replace an uncertain debit. Show paid/pending/not-started separately. Cancellation stops future debits and does not recall a submitted transfer.

The execution adapter initially remains interactive. Display the actual number of approvals required. Single-PIN batch execution is not promised by N× planning.

### M4 — single-authorisation feasibility gate (research only)
Evaluate an approved mandate/provider route and, separately, OffPay-style USSD compatibility. Establish daily amount/count limits, merchant classification, confirmation access, SIM support, provider terms and applicable distribution requirements. OffPay PIN replay is not a bank-issued batch mandate. No PIN-replay feature goes into the current APK. If a viable mechanism cannot be established, keep N× interactive and report the limitation.

Fee savings remain a separate hypothesis. Do not advertise MDR avoidance until the actual payment route, applicable rules and acquiring-bank treatment have been verified.

## Architecture

`ScanActivity / image picker → ZXing decoder → PaymentCore.Request → review UI → UPI Intent adapter → receiving UPI app → callback classifier → unresolved-attempt store → user reconciliation`

`DebugLog` receives only controlled event fields and never the raw payment objects.

- `PaymentCore`: Android-independent URI/amount validation and callback classification, plain-Java tests.
- `MainActivity`: screen, QR review, application selector, Android handoff, persisted lock and controlled logging.
- `ScanActivity`: camera preview with background QR decoding; releases camera on pause, no frame persistence.
- `DebugLog`: app-private bounded event file (~48 KB), manually exported. No network.
- Storage: private preferences retain only attempt ID, lock and status. Temporary QR state is held in activity state; no backup.
- Authentication boundary: receiving UPI app. No credential field in our UI or data model.
- UI security: own screens omit FLAG_SECURE while `UiFlags.ALLOW_SCREEN_RECORDING` is true, so owner device tests can be screen-recorded; set it false to restore screenshot/recording blocking. CRED's own authentication screens stay protected regardless. No overlay/SMS permissions. CAMERA is requested only for scanning; accessibility services are separately user-enabled.
- Build: native Android Java UI, Android SDK 35, ZXing core 3.5.3. Direct SDK build script avoids archived Gradle/AndroidX dependency constraints. No native libraries.
- Signing: development-only prototype key retained with source for repeatable updates. This is not a production signing identity; never use it for a public product.

## State transitions

READY → REVIEW → CHOOSE_APP → AWAITING_RESULT

Persist AWAITING_RESULT before launching. A callback changes only the reported outcome, leaving the unresolved lock in place. A process restart preserves the lock. An explicit user check clears it and discards the QR for M1. No transition schedules another payment.

## Ordered task list

1. [x] Inspect EasyUpiPayment implementation; identify authentication and callback boundaries.
2. [x] Write requirements/design/milestone acceptance criteria.
3. [x] Implement strict QR/amount logic and conservative callback classifier.
4. [x] Implement camera, image import and paste input.
5. [x] Implement review, UPI app selector, payment handoff and durable duplicate-send lock.
6. [x] Implement sanitised live console, share/save and crash metadata.
7. [x] Run logic tests; build/sign/inspect APK; package source and device checklist.
8. [ ] Owner tests M1 on phone and supplies log plus actual outcome.
9. [ ] Resolve M1 issues from logs; release version 0.1.x as needed.
10. [ ] Implement and phone-test M2 repeat-payment ledger.
11. [ ] Implement and phone-test M3 N× planning with honest authentication behaviour.
12. [ ] Complete M4 feasibility gate before any single-authorisation claim.

## Debugging loop

For each report provide: APK version, phone model and Android version, UPI app name/version, scan/import mode, test steps, visible error, actual debit/merchant receipt and exported `.txt` file. Do not send a PIN, OTP, bank statement or full payment screenshot. Export contains no exact amount; provide a test amount separately only if useful.

We classify failures as QR capture, URI validation, app discovery, external-app rejection, missing/malformed callback, lifecycle recovery, or actual payment-network issue. A log cannot establish receipt of money; user/merchant confirmation remains necessary in this prototype.

## Current limitations

No independent PSP status lookup, no unattended debits, no merchant onboarding integration, no guarantee that every UPI app accepts a third-party intent. No device/emulator execution is claimed unless separately recorded in VERIFICATION.md. Camera/device compatibility and live bank behaviour require the owner's phone test. SDK tests do not prove settlement, distribution approval, MDR savings or single-PIN feasibility.

## Iteration 0.1.1
First device log received: camera stalled, two image failures, three Amazon-reported failures. M1 remains open. Diagnostic and QR-decoder improvements are delivered; see RELEASE-0.1.1.md. M2/M3 are not started.

## Iteration 0.1.2
Added explicit CRED/BHIM/Google Pay/PhonePe/Paytm/Amazon discovery and app-availability report, package-targeted handoff and request-shape diagnostics. Amazon post-PIN failure is still unconfirmed as resolved. M1 remains in device validation; see RELEASE-0.1.2.md.

## Iteration 0.1.3
Owner confirmed ₹2 via SBI e₹; ordinary UPI app discovery still needs device validation. Shared package-targeted plus generic discovery replaces divergent checks. Ethical communication review and sanitised discovery/timing logs added. See RELEASE-0.1.3.md and ETHICAL-COMMUNICATION-REVIEW.md.


## 0.1.4 iteration: runtime handler enablement
- [x] Replace raw enabled-flag veto with runtime settings and normal resolver evidence.
- [x] Preserve explicit-disable, export and permission safeguards.
- [x] Log raw flags and runtime states independently.
- [x] Add runtime state regression coverage and build signed APK.
- [ ] Verify chooser on user device; no payment needed.
- [ ] Verify payment acceptance separately after chooser succeeds.


## Milestone 2 / 0.1.5
Requirements: two or three user-approved equal payments through CRED; total review; freeze recipient/amount during queue; no automatic retry; receipt gate; durable pending lock; restart recovery; stop queue; sanitized live diagnostics.
Design: persist queue_uri, queue_count, queue_done in app-private session preferences. Persist unresolved before external handoff. A callback updates reported status only. User receipt confirmation atomically clears lock and advances count, or clears queue after last receipt. Confirmed not paid ends queue. Stop removes queue details but retains pending payment state. No payment is sent on app startup.
Tasks:
- [x] Static QR eligibility and exact-decimal total.
- [x] Queue review and per-payment CRED handoff.
- [x] Receipt gating, persistence, completion and stop.
- [x] Live queue diagnostics and export.
- [x] Pure Java queue tests; build and verify signed APK.
- [ ] Phone validation: two payments, restart recovery, cancellation.
- [ ] Separate future investigation: non-sensitive CRED screen observation.
- [ ] Future one-click approach feasibility; no credential replay implemented.


## Milestone 3 / 0.1.6 — CRED metadata observation
- [x] Optional user-enabled service restricted to CRED window-state events.
- [x] Separate bounded in-app consent, one handoff, memory-only session.
- [x] Filtered class/timestamp/attempt diagnostics; no screen contents or actions.
- [x] Explicit stop and callback stop.
- [x] Unit tests, capability audit and signed build.
- [ ] Device test: usefulness of exposed CRED window metadata.
- [ ] Future automation decisions after evaluating observed transitions.


## 0.1.7 revised scope: human authentication on every part
User parked full automation/PIN-once work. Current requirement: split a TOTAL, automate supported navigation around authentication, leave fingerprint to the user, advance after clear CRED reported success.
- [x] Separate split UI with exact paise plan and batch consent.
- [x] Foreground success-only advance with stop window; uncertainty lock.
- [x] New separately enabled navigation service; one recipient/amount-verified swipe.
- [x] Password-node exclusion; no raw text in logs; no authentication actions.
- [x] Expiry and process-death revocation, no replay/retry.
- [x] Unit checks and signed build.
- [ ] Device validation of accessible control semantics and fingerprint landing.
- [ ] Result-screen dismissal automation if a later supported mapping is established. Current build relies on intent return or manual return.


## 0.1.8 — recordable own screens
Device validation of 0.1.7 remains open. To make that evidence easier to capture, our own windows no
longer set FLAG_SECURE by default.
- [x] Gate FLAG_SECURE on all three own activities behind `UiFlags.ALLOW_SCREEN_RECORDING` (default true).
- [x] Correct the architecture note that asserted unconditional FLAG_SECURE.
- [x] Release note recording the weakened capture property and how to restore it.
- [x] Rebuild and sign; all seven JVM suites pass unchanged.
- [ ] Device check: Android recorder captures our screens; CRED segment still records black.
- [ ] All 0.1.7 device checks remain outstanding and unchanged.
No payment, split, navigation, consent, logging or persistence behaviour changed.


## 0.1.9 — post-payment dismissal and a real stop
First successful device run (0.1.8): two parts of Rs 1, both received. Device evidence showed the
external intent flow ends in a **Pay now** button, not the "Swipe to pay" slider the 0.1.7 helper
was built for, so the pre-authentication swipe never fired. Pay now is the approval point and
stays manual.
- [x] Second, independent dismissal phase on `AssistSession`, bounded to 180 s and 4 actions.
- [x] Dismiss the CRED reward banner by flicking its own bounds off screen.
- [x] Click a close control matched on explicit close semantics only.
- [x] `SplitCore.offerControl` bars Pay now / Swipe to pay / Claim now from ever being dismissal targets.
- [x] Dismissal never runs while an authentication marker is visible.
- [x] Returning with no callback records NO_CALLBACK and stops the entire sequence.
- [x] Consent text, service description and unit tests updated to match actual behaviour.
- [ ] Device check: banner and close control are exposed as nodes; dismissal fires once per part.
- [ ] Device check: declining authentication on part 1 leaves part 2 unsent.
- [ ] Decide whether to retire or repoint the unexercised Swipe to pay path.


## 0.1.10 — lands on authentication, Rs 50,000 ceiling
Second device run (0.1.9) completed three parts; dismissal worked unaided. Only the per-part
Pay now tap remained manual.
- [x] Press Pay now under the existing recipient/amount guard, authority revoked before the press.
- [x] Match the recipient by bank-resolved name or payee address; the intent screen shows no address.
- [x] Log payee_vpa_match, payee_name_match, pay_controls and pay_node for diagnosis.
- [x] Raise per-payment and split-total ceilings to Rs 50,000; widen amount patterns to five digits.
- [x] Consent, service description and plan dialog restated: the fingerprint is the approval.
- [ ] Device check: Pay now pressed once per part; recipient matches by name.
- [ ] Device check: mismatched amount or recipient pauses instead of pressing.
- [ ] Device check: a five-digit part amount is matched, not treated as a currency conflict.
- [ ] Open question: whether to keep the unexercised Swipe to pay path at all.


## 0.1.11 — Ranna branding and UI
Third device run (0.1.10) confirmed each part lands on authentication unaided. Presentation-only
release; no behavioural code touched.
- [x] Rename the wrapper to Ranna after the classical Kannada poet; package id unchanged.
- [x] Kannada tagline and educational-use notice on every screen and in both service descriptions.
- [x] Warm palette, rounded buttons, new launcher icon; `Brand.java` holds presentation only.
- [x] Verify the tagline survives into resources and dex.
- [ ] Device check: Kannada glyphs render; launcher name and icon correct; contrast holds.


## 0.1.12 — grouped amounts and release preparation
Rs 5,000 split in 3 did not press Pay now; small amounts did. CRED renders amounts over a thousand
with Indian digit grouping, and both amount patterns were digits-and-dot only, so the guard never
saw a matching amount. Present since 0.1.10, masked by testing only at Rs 1 and Rs 2.
- [x] Strip grouping separators before the exact amount comparison.
- [x] Move the on-screen amount pattern into `SplitCore.looksLikeAmount` and unit test it.
- [x] Log `screen_not_fully_read` instead of returning silently; raise the node cap to 800.
- [x] Public README: educational purpose, no reverse engineering, no compliance claim, own risk.
- [x] `.gitignore` excludes dist/, build/ and keystores; no APK is ever committed or released.
- [x] Portable `build-windows.sh`; scanner test QR moved to docs/assets.
- [ ] Device check: Rs 5,000 in 3 parts presses Pay now for every part.
- [ ] Device check: a mismatched amount still pauses.
