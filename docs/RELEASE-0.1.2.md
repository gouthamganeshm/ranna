# N× Lab 0.1.2 — UPI app discovery and handoff

## Evidence and remaining uncertainty
Owner reports the same recipient can be paid directly in Amazon, while N×-initiated payment reaches PIN entry and then fails. This narrows investigation to differences in request context, external-app acceptance or the handoff; it does not prove which is responsible. The supplied 0.1.0 callbacks contain REPORTED_FAILURE without a usable reason code. The latest exact Amazon error and 0.1.2 request-shape log are still needed.

## Changes
- Package-targeted ACTION_VIEW handoff (`setPackage`) replaces pinning a particular activity component. This lets each receiving app resolve its supported UPI entry activity; matches Google's documented integration pattern.
- Discovery restricts to default, exported, enabled handlers the caller can invoke.
- Explicit Android package visibility for CRED, BHIM, Google Pay, PhonePe, Paytm and Amazon. Generic UPI handler discovery remains for other apps.
- New “Check installed UPI apps” button reports installed/enabled/link-handler availability for those six apps. It does not claim compatibility when an installed app exposes no matching handler. Work profiles and cloned apps may have separate visibility.
- REQUEST_SHAPE logs present/empty/absent flags for documented payment fields and QR context; no parameter values are recorded.
- Existing QR merchant codes, order references and mode/context fields are preserved, not fabricated or reclassified. These diagnostics will indicate missing metadata, not automatically supply merchant credentials.
- Removed the Amazon-only warning dialog from 0.1.1. All apps use the same explicit, user-selected flow.
- No PIN capture, automatic replay, N×, automatic retry or changed pending-payment recovery behaviour.

## Validation
35 payment-parser/metadata/status checks and 12 QR checks passed. APK signature, alignment and packaged manifest inspected. Same signer as 0.1.0/0.1.1; version code 3; API 26 minimum and target 35. CAMERA remains the only requested permission. No phone or live-bank test was performed. This release fixes integration weaknesses but does not claim Amazon's rejection has been resolved.

## Next phone test
Install over the existing version. Tap Check installed UPI apps; export the result. Resolve any prior debit only after checking actual payment history. Use the same merchant QR and small amount that worked directly in Amazon. Try one selected app at a time. Record the exact error shown after PIN entry, or actual receipt if successful. After resolving the first outcome, compare with BHIM or CRED if installed and listed. Send the exported log and error text; no PIN or OTP.

## Sources inspected
- https://developers.google.com/pay/india/api/android/in-app-payments
- https://play.google.com/store/apps/details?id=com.dreamplug.androidapp
- https://play.google.com/store/apps/details?id=in.org.npci.upiapp

Public Amazon documentation located during this check did not establish an India-specific explanation for this exact UPI rejection. Do not infer Amazon's private risk decision from the callback alone.
