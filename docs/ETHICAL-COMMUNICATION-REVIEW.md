# UPI communication: what this prototype can observe

## Publicly documented structure, not captured traffic
NPCI operates UPI routing/processing/settlement for participating members. PSP banks connect to UPI and TPAPs participate through PSPs. A phone payment app is therefore not evidence of a simple direct phone-to-NPCI connection. Its app/backend/PSP arrangement depends on the provider.
Source: https://pay.google.com/intl/en_in/about/external/npci/
NPCI overview: https://www.npci.org.in/product/upi

The publicly documented Android handoff uses a `upi://pay` URI to request payment in another app and an activity result for the client response. A successful or submitted response still requires PSP verification in a merchant integration.
Source: https://developers.google.com/pay/india/api/android/in-app-payments

These sources describe interfaces and participant responsibilities. They do not provide this user's private bank traffic, Amazon's rejection reason, or a publicly usable unauthenticated NPCI transaction API.

## Ethical evidence available now
- Owner-provided debug exports and confirmation of the payment outcome.
- Our own outgoing request field-presence flags and Android intent route.
- Installed matching handlers and reason flags for excluding inaccessible handlers.
- Timing, callback structure, recognised payment status and safe response code.
- Public NPCI/PSP documentation and published SDK examples.

For deeper backend diagnosis, an authorised bank/PSP test integration or provider-supplied diagnostic trace is needed. Provider cooperation and access are not assumed; no provider was contacted or message sent.

## Limits of phone inspection
Android isolates applications. Our APK cannot read another app's private logs or decrypted network payloads through ordinary application permissions. Android Studio's Network Inspector inspects an app process and supported network libraries; it is not a general decoder of all installed banking apps.
Sources:
- https://developer.android.com/privacy
- https://developer.android.com/studio/debug/network-profiler

No production UPI/NPCI server communication was captured during this work. The APK retains CAMERA as its only requested permission and has no network capture, root, VPN interception, certificate installation, certificate-pinning bypass, hooking, app modification or credential interception feature. We must not label the local handoff logs as server traces.

## Export and iteration
Use Save debug log (.txt) or Share debug log in N× Lab. These exports contain version/device information, app-local events, controlled diagnostic fields and user-confirmation labels. They do not establish settlement independently. Provide the exact payment-app error text separately if needed, omitting credentials. The next build is focused on confirming app discovery before further payment automation.
