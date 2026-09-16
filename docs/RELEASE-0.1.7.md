# 0.1.7 — Assisted splits, manual authentication

## Evidence and design
The supplied 30.6-second video shows native CRED QR scanning, amount entry, Swipe to pay, a protected blacked-out segment and success. It is not a recording of the external intent flow. Prior device logs establish successful intent handoffs and CRED success callbacks; no evidence yet establishes accessible button semantics. Therefore no fixed coordinates or result-screen close guesses are used.

SplitCore divides integer paise into 2 or 3 parts with remainder distributed deterministically, sum preserved, each part 0.01–2000 and total at most 6000. Static QR restrictions from the repeat mode apply. A distinct screen reviews the total and complete part list, and explicitly obtains batch/navigation consent.

AssistedSplitActivity persists unresolved=true before every launch. Split plan and auto-advance authority exist only in memory. Success plus RESULT_OK enables the next launch after 4 seconds in foreground. Failure, cancelled result, pending, unknown, unavailable service or launch/storage error stops the run. The pending lock remains throughout the sequence until the user checks all receipts or separately resolves a stopped run. A process restart never restarts the plan. Losing foreground cancels the scheduled next launch and leaves a Continue button. Results are reported, not independently verified.

Separate CredAssistService requires new user enablement, content retrieval and gesture capabilities. The prior observer remains metadata-only. The helper's content access is scoped in code to a consented CRED handoff with a 20-second expiry. It searches at most 250 visible nodes, skips password subtrees, and pauses on authentication labels. Before swiping it requires exact payee-address match, a matching currency-prefixed numeric amount, no conflicting currency values, and exactly one Swipe to pay label. It uses that control or up to two ancestors only if bounds form a sufficiently wide, short lower-half slider. No hardcoded video coordinates. Authority is revoked before one gesture; no retry and no interaction with authentication. Missing/ambiguous controls pause for manual operation. CRED may expose no suitable nodes.

After a swipe the helper neither inspects nor acts on the authentication/result screens. Navigation through a result screen relies on CRED's intent callback; if CRED requires a dismissal not exposed by that flow, the user returns normally. No result-close automation is claimed. CRED decides authentication requirements; the app cannot force biometric authentication or attest that it occurred.

No root, screenshot capture, network inspection, raw UI logging, PIN handling or biometric collection. ASSIST_CHECK stores boolean matches/control counts only. ASSIST_DISPATCH and ASSIST_SWIPE record the single gesture outcome. SPLIT_STARTED/LAUNCH/RETURN/STOPPED/REPORTED_COMPLETE/RECEIPTS_CONFIRMED provide attempt correlation without payee or amounts.

## Validation
71 split/navigation-session checks added; existing suites passed. APK signature verified with same development certificate. No emulator or phone gesture validation performed here. This is an experimental guarded navigation implementation; use ₹2 total / 2 parts for the first check. Existing Android deprecated API warnings remain.

## Outstanding phone checks
- New accessibility service can be enabled and CRED accepts it.
- Recipient, currency amount and slider are accessible and match; otherwise inspect ASSIST_CHECK/ASSIST_PAUSED.
- One swipe leads to manual authentication; no further gestures occur during it.
- Successful callback opens next part after the stop window, with correct amount.
- Unknown/cancelled results stop; process restart does not replay a part.

Android API reference: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
