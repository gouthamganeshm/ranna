# 0.1.6 — Optional CRED window observer

User confirmed two receipts in 0.1.5; log ended QUEUE_COMPLETE confirmed=2 count=2, unresolved=false. Sequential handoff baseline is verified on the supplied vivo device.

This build adds an optional AccessibilityService, protected with BIND_ACCESSIBILITY_SERVICE and explicitly enabled by the user in Android settings. XML subscribes only to CRED TYPE_WINDOW_STATE_CHANGED. Content retrieval, gesture and screenshot capabilities are false; isAccessibilityTool is false.

Separate in-app consent arms one CRED handoff for at most ten minutes. No metadata is logged before that handoff. Session exists only in process memory, stops on return/explicit stop/interruption/destruction, and is lost on process death. Metadata is capped at 200 events. Timeout suppresses subsequent events even without a callback. Android service may remain enabled until the user disables it; enabled alone does not arm logging.

Logs include OBSERVER_SERVICE, OBSERVER_ARMED, OBSERVER_BEGIN, CRED_WINDOW and OBSERVER_STOP. CRED_WINDOW contains attempt id, sequence and a filtered class name. Wall-clock timestamps come from existing console. No event text, descriptions, source nodes, screenshots or input events are read. No taps, PIN entry or payment advancement is performed. Metadata does not classify success or authenticate a payment.

Limitations: same-window navigation may emit no useful transitions; protected events may be absent. Class names are not reliable screen labels. If CRED or Android prevents service use, stop and report the message; there is no bypass.

## Test on phone
1. Install over 0.1.5.
2. Observer settings → NX Lab CRED observer → enable.
3. Return; service should say connected. Tap Arm one CRED observation and accept the scope.
4. Import QR and perform one small CRED payment, authenticating normally.
5. Return, check actual receipt, record outcome and export log.
6. Disable observer in Android settings after testing.

Expected: OBSERVER_ARMED → OBSERVER_BEGIN → zero or more CRED_WINDOW → OBSERVER_STOP reason=payment_return, plus normal callback. Zero window events is a valid diagnostic result, not a payment failure.

Validation: build and signature verification; 20 observer unit checks; existing tests; configuration/API audit to exclude text, nodes, gestures and screenshots. No device/emulator observation test performed here.

Android configuration reference: https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo
