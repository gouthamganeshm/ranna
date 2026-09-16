# N× Lab 0.1.1 — scan and payment diagnostics

## Evidence from the first phone test
Device vivo I2203, Android API 34. Camera sessions ended with user cancellation; the old log had no frame/focus evidence. Two image imports failed QR detection. Three explicit payment handoffs to Amazon returned REPORTED_FAILURE without a recognised response code. Android result -1 means RESULT_OK for the activity, not that the payment succeeded. The final payment remains unresolved in the supplied log. No claim is made about its settlement.

## Changes
- QR decoding tries adaptive and global thresholds, normal and inverted polarity, and bounded crops of screenshots.
- Transparent imported image pixels are composited onto white before luminance conversion.
- Camera screen includes Refocus and Torch buttons and visible frame/scan counters.
- Logs include camera dimensions, preview format, focus mode, periodic frame/pass counts, image dimensions/sample size, and sanitised image error type.
- Decoding the supplied `nx-scanner-test.png` shows a scanner-test success dialog without preparing any payment.
- App selector displays app names and package names and records offered packages. Amazon selection offers an explicit comparison with another app.
- Callback diagnostics record response presence/length and allowlisted field-presence flags, never raw values. QR diagnostics record only whether merchant-name, reference, merchant-code and currency fields are present.
- Log writes are synchronised across scanner and payment screens.
- No automatic retry, changed merchant/order data, PIN handling or N× execution added.

## What is not established
The user's original images were not supplied. Better decoding is implemented and fixture-tested, but the exact cause of those images failing is unknown. Amazon did return a failure status, but did not supply a usable reason code in the prior log. This release does not claim to fix Amazon's payment rejection. No phone, camera hardware or live payment test was performed here.

## Installation and test sequence
1. Install `nx-lab-0.1.1.apk` over 0.1.0; same application ID/signing certificate, version code increased to 2. Do not uninstall or clear app data to bypass a pending payment.
2. Import `nx-scanner-test.png`. Expect: “Scanner test passed. This QR cannot initiate a payment.” This works even while an earlier payment is unresolved.
3. Display that test QR on another screen and scan it. If scanning stalls, try Refocus; note whether the frame count increases. Export the log after about 15 seconds rather than waiting minutes.
4. Once earlier payments are checked and resolved, scan the actual merchant QR. If there is another UPI app listed, choose it for a small approved test.
5. If Amazon fails again, record its exact on-screen error text. Separately note whether the same merchant QR can be paid using that app's own scanner. Do not send PIN/OTP screens.
6. Send the 0.1.1 debug log and whether the test QR worked through import and camera.

## Validation
35 payment logic checks plus 12 QR checks pass (including actual PNG round-trip, four rotations, inversion, off-centre screenshot, transparent background and blank input). APK compiled, signature verified, package/version/permission metadata inspected. Camera remains the only requested permission; min SDK 26, target SDK 35.
