# N× Lab 0.1.3 — consistent send chooser

## Findings from phone log
- The owner confirms a ₹2 payment through SBI e₹; final event USER_CONFIRMED_RECEIVED. This is user-confirmed success, not independent bank verification. The e₹ callback had no recognised response and correctly remained UNKNOWN until the owner's check.
- Individual app probes showed CRED, BHIM, Google Pay, Paytm and Amazon resolving; PhonePe was absent. The send chooser listed only SBI e₹.
- Source review identifies inconsistent discovery: individual package resolution in availability, generic enumeration with additional checks in chooser. The availability probe also used the original QR rather than the final amount-bearing URI. Logs cannot establish which device-specific resolver/filter behaviour caused the divergence.
- QR image decoding succeeded for the 770×770 image. Other images were decoded but rejected by URI validation; those are not image-decoder failures. Camera delivered 42 frames and attempted six decodes before cancellation in about two seconds; live scanner success is not established.

## Changes
- One shared discover method for app availability and send chooser, both using the fully prepared URI.
- Resolve each known app by package, then merge valid results with generic handlers; deduplicate packages. No force-launch of unexported, disabled or permission-restricted activities.
- Explicit labels for CRED, BHIM, Google Pay, PhonePe, Paytm and Amazon when an accessible matching handler is available. Generic matches retain support for e₹ and other installed apps.
- Availability now requires scanning a QR and entering its amount, so it does not make promises based on a placeholder or different URI.
- Sanitised logs record individual handler checks, package match, generic/merged counts and elapsed handoff duration. No QR values, PINs, tokens or raw callbacks are recorded.
- Same app ID, same signing key, version code 4; install over prior build.

## Validation and limits
35 payment checks, 12 QR checks and 7 discovery regression scenarios passed. The discovery regression includes individually resolved apps missing from the generic list, deduplication and blocked/self/null/empty cases. This tests merge logic, not Vivo's PackageManager implementation. Build/signature/alignment and manifest are inspected. No new device test or real payment executed here.

## Next test
Install over 0.1.2. Scan the same accepted QR and enter a small amount. Compare “Check installed UPI apps” with the actual send chooser; they should use the same candidates. Test one app at a time, reconciling its outcome before another payment. Export logs, especially APP_TARGET_PROBE, APP_HANDLER, DISCOVERY_MERGE, APP_OPTION and PAYMENT_RETURN. If only e₹ remains, these checks will identify whether other handlers are missing or inaccessible rather than silently dropping them.

No N× or credential automation added. The fingerprint/PIN prompts belong to the selected payment app; this prototype neither suppresses nor captures them.
