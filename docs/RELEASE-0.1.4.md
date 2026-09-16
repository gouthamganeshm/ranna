# N× Lab 0.1.4

## Diagnosis
The supplied 0.1.3 log resolves CRED, BHIM, Google Pay, Paytm and Amazon for the prepared payment URI, but our combined raw activity/application enabled check removes all five. Only SBI e₹ survives. The log does not identify which raw flag is false or why. This is an eligibility filter problem; it does not establish a payment processing failure or an OEM defect.

## Change
Discovery queries still use MATCH_DEFAULT_ONLY, never MATCH_DISABLED_COMPONENTS. A normally resolved handler is eligible when application and component runtime settings are DEFAULT or ENABLED. Explicitly disabled and unknown states remain excluded. Export, permission, expected-package and self-exclusion checks remain. No app settings are changed. Package-targeted payment intents remain unchanged.

APP_HANDLER now logs raw activity/application flags, application/component runtime states and effective eligibility separately. States: 0 default, 1 enabled, 2 disabled, 3 disabled by user, 4 disabled until used, -1 unreadable. APP_STATE_ERROR records only exception class. No payment identifiers, credentials or private server traffic are captured.

Android reference: https://developer.android.com/reference/android/content/pm/PackageManager

## Verification
Build passed: 35 payment checks, 7 existing discovery scenarios, 72 runtime-state cases, one resolved-handler merge regression, and 12 QR checks. APK signature verification passed. Same development signing key retained for upgrade. No Android device execution performed here; the actual vivo chooser and bank acceptance remain unverified.

## Phone check
1. Install 0.1.4 over 0.1.3 without uninstalling.
2. Open a QR and enter an amount, then open the send list. No payment is required for this check.
3. Confirm which of CRED, BHIM, Google Pay, Paytm and Amazon appear. PhonePe is only offered if installed and resolvable. Other generic UPI handlers remain discoverable.
4. Export the debug log and return the new 0.1.4 section, particularly APP_HANDLER, APP_STATE_ERROR, DISCOVERY_MERGE and APP_OPTION.
5. Only if testing a payment, independently check the recipient and receipt; an unknown callback remains unresolved. This release does not claim to fix Amazon's earlier payment rejection.

N× automation and PIN replay are not included. No rooting, certificate interception or access to other apps' private logs is involved.
