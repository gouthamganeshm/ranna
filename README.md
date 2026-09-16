# Ranna

**ಕನ್ನಡಿಗರಿಂದ ಭಾರತಕ್ಕೆ**
*Kannadigarinda Bharatakke — from Kannadigas to India*

Named after **Ranna**, one of the *ratnatraya*, the three gems of classical Kannada poetry.

An Android study of how the documented UPI intent handoff behaves on a real phone: scan a merchant
QR, review it, hand it to an installed UPI app, and honestly classify whatever comes back.

---

## ⚠️ Read this first

> ### For educational and research purposes only.
>
> Ranna is a personal learning prototype. It is **not a product**, **not a payment service**, and
> **not affiliated with, endorsed by, or connected to** CRED, NPCI, UPI, any bank, or any payment
> service provider. Those names appear only to describe what was tested.
>
> ### Use at your own risk.
>
> This software moves **real money** through **real payment rails**. A mistake costs actual rupees.
> There is no undo, no refund path, and no support. The authors accept **no liability whatsoever**
> for financial loss, account restriction, app or account suspension, data loss, or any other
> consequence of running this. If you are not willing to lose the amount you enter, do not run it.
>
> ### No APK is distributed.
>
> There are no releases, no attached binaries, no download links — deliberately. If you want to run
> Ranna, **build it yourself from this source**, on your own machine, with your own signing key, and
> read the code first. See [Build from source](#build-from-source).

---

## What this is

Ranna wraps the ordinary, publicly documented Android UPI handoff:

```
scan / import QR → validate → review → upi://pay intent → your UPI app → callback → you reconcile
```

Its guiding rule is that **a client callback is not settlement**. A UPI app reporting "success" is a
report, not proof that money arrived. Everything follows from taking that seriously: an
unresolved-attempt lock is committed to disk *before* any handoff, nothing retries automatically,
process death drops all in-memory state, and only *you* can mark an attempt resolved after checking
the actual receipt.

### Modes

| Mode | What it does |
|---|---|
| **Single payment** | One reviewed QR, one amount, one approval in your UPI app. |
| **Repeat queue** | 2–3 identical payments to the same static QR; you confirm receipt between each. |
| **Split a TOTAL** | Divides a total into 2–3 exact parts in integer paise and walks them in sequence. |

Split mode uses an optional, separately enabled AccessibilityService that reads the visible CRED
screen to check the recipient and amount, presses **Pay now** to reach authentication, and afterwards
dismisses CRED's reward banner and result screen so the next part can open.

**You authenticate every single part yourself.** The service cannot authorise a debit — the
fingerprint or UPI PIN is the approval, it stays with you, and CRED and the PSP enforce it.
Declining or cancelling authentication stops the entire sequence.

---

## What this explicitly does *not* do

This matters more than the feature list, so it is stated precisely.

**No reverse engineering.** No app was decompiled, disassembled, patched, repacked, or otherwise
reverse engineered in building this. No proprietary protocol was reconstructed. No private or hidden
API is called. Ranna uses two public, documented Android mechanisms and nothing else:

- the `upi://pay` deep link with `ACTION_VIEW` / `startActivityForResult`, as documented by Google
  for [India in-app payments](https://developers.google.com/pay/india/api/android/in-app-payments);
- the standard [`AccessibilityService`](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
  API — the same public interface every screen reader uses — reading only what is already drawn on
  screen for the user to see.

**No credential handling of any kind.** Ranna has no PIN field, no password field, no biometric
integration, and no data model that could hold a credential. It cannot capture, store, replay, or
bypass a UPI PIN or a fingerprint. Password nodes are skipped outright, and all assistance halts the
moment an authentication screen is visible.

**No interception, no tampering.** No root, no traffic capture, no VPN interception, no certificate
installation, no certificate-pinning bypass, no hooking or instrumentation of other apps, no
modification of any other app, no screenshots, no SMS or OTP access, no device-wide log harvesting.

**No network access at all.** Ranna requests exactly one permission — `CAMERA`, for scanning QR
codes — and has no `INTERNET` permission. QR decoding is entirely on-device. Nothing is uploaded
anywhere, ever. Logs stay on the phone until you export them by hand.

**No claims about money.** Ranna never asserts that a payment settled. It reports what a UPI app
*told* it, labelled as such, and asks you to verify the receipt yourself.

You do not have to take any of this on trust — it is roughly 1,100 lines of plain Java with no
dependency except ZXing for QR decoding. Read it.

---

## Legal, terms, and your responsibility

Please read this carefully rather than skimming it.

**No compliance claim is made or implied.** This project has **not** been reviewed against the terms
of service of CRED, NPCI, UPI, any bank, any payment service provider, or Google Play, and no legal
review of any kind has been performed. Nothing here should be read as an assurance that using it
complies with any agreement, regulation, or policy that binds you.

**Automating another application's interface may conflict with that application's terms.** Many
apps — payment apps especially — restrict automated interaction, accessibility-driven control, or
unattended operation. Whether that applies to your use, your account, and your jurisdiction is a
question only you can answer, and you should answer it *before* running this.

**You are solely responsible** for how you use this software: for reading the relevant terms, for
obtaining any permission you need, for compliance with applicable law, and for every consequence,
including a restricted or suspended account.

**Intended use is your own device and your own money**, for learning. Using it against anyone else's
account, or in any way that misleads a merchant or payee, is neither supported nor endorsed.

If you are unsure about any of the above, the correct action is to read the source for understanding
and **not run it**.

---

## Requirements

### Device

- Android 8.0 (API 26) or newer.
- A UPI app installed and set up — **CRED** (`com.dreamplug.androidapp`) for split mode, the only
  app whose screens the navigation helper understands.
- A bank account already linked in that UPI app, with a working UPI PIN.
- A basic **static** UPI QR to test against. Ranna rejects QRs carrying a fixed amount, order
  reference, or other payment metadata in repeat and split modes, so it can never replay an order.

### Fingerprint must be enabled

Split mode is built around landing you directly on the **fingerprint prompt** for each part. For
that to work:

1. **Enrol a fingerprint on the phone** — *Settings → Security (or Biometrics) → Fingerprint* — and
   add at least one finger.
2. **Turn on biometric payment authentication inside your UPI app.** In CRED this sits in the
   profile/settings area, usually worded like *Use fingerprint to pay* or *Biometric
   authentication*. Exact wording and location change between CRED versions.

If biometrics are not enabled, CRED asks for the **UPI PIN** instead. Ranna still works — the helper
pauses at any authentication screen either way — but you will type a PIN for every part rather than
touching the sensor, and the "lands straight on the fingerprint" behaviour is not what you will see.

Ranna never sees, supplies, or verifies your fingerprint. The biometric prompt belongs to CRED and
the Android framework.

---

## Phone settings you must change

All on **your** phone, all reversible, and all to be **undone when you finish testing**.

| # | Setting | Where | Why |
|---|---|---|---|
| 1 | **Install unknown apps** | Settings → Apps → *(your file manager or browser)* → Install unknown apps → Allow | You are sideloading an APK you built; Android blocks this by default. |
| 2 | **Allow restricted settings** | Settings → Apps → Ranna → ⋮ (top right) → **Allow restricted settings** | **Android 13+ only, and easy to miss.** A sideloaded app cannot be granted Accessibility until you do this. Without it, step 3 is greyed out or silently refuses. |
| 3 | **Accessibility → Ranna CRED navigation** | Settings → Accessibility → Downloaded apps → **Ranna CRED navigation** → On | Required for split mode. This is the service that checks recipient and amount, presses Pay now, and dismisses the result screen. |
| 4 | **Camera permission** | Granted on first scan, or Settings → Apps → Ranna → Permissions | Only needed to scan a QR with the camera. Import or paste works without it. |
| 5 | **Battery → Unrestricted** | Settings → Apps → Ranna → Battery → Unrestricted | Aggressive battery management can kill the accessibility service mid-sequence. If the service dies Ranna revokes its own authority and stops — safe, but the run ends. |
| 6 | **Screen recording** *(optional)* | Your phone's normal screen recorder | Ranna's own screens are recordable on purpose, for capturing test evidence. CRED's authentication screens are protected by CRED and record as black. |

> **Not required, and not recommended:** *Ranna CRED observer*. An older diagnostic service that
> records only window class names and timing. It cannot navigate. Leave it off.

### When you are done testing

1. Turn **off** *Ranna CRED navigation* (step 3).
2. Revoke *Install unknown apps* (step 1).
3. Optionally uninstall Ranna — but **resolve any pending attempt first**. Uninstalling to clear a
   stuck lock destroys the record of a payment you may not have reconciled.

---

## Build from source

No binaries are published. You build it.

### Prerequisites

| Tool | Version | Notes |
|---|---|---|
| **JDK** | 17 | Microsoft OpenJDK, Temurin, or any JDK 17. |
| **Android SDK** | Platform 35, Build-Tools 35.0.0 | Plus `platform-tools`. |
| **Python** | 3.x | Inserts the DEX files into the APK. |
| **bash** | any | Git Bash on Windows. |

No Gradle, no Android Studio, no AndroidX. The build calls `aapt2`, `javac`, `d8`, `zipalign` and
`apksigner` directly, which is why it stays reproducible with very few moving parts.

### 1. Install the Android SDK command-line tools

Download **command-line tools** from the
[Android Studio downloads page](https://developer.android.com/studio#command-line-tools-only) and
unzip so that `sdkmanager` sits at `$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/`.

> ⚠️ **Choose your cmdline-tools version carefully.** Bundles from `16111833` onward replace the
> real `sdkmanager` with a shim that delegates to a new `android` CLI and **fails to parse
> semicolon-style package names** like `platforms;android-35`, reporting `Package platforms not
> found`. Use an older bundle that still ships the classic `sdkmanager` —
> **`commandlinetools-*-13114758_latest.zip`** is known to work.

### 2. Install the SDK packages

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"     # wherever you want the SDK

yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" --licenses
"$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Accepting those licences is Google's requirement, not this project's.

### 3. Build

**Linux / macOS**

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
./build.sh
```

**Windows** — use **Git Bash**, not PowerShell or `cmd`:

```bash
export ANDROID_SDK_ROOT="C:/Users/you/AppData/Local/Android/Sdk"
./build-windows.sh
```

`build-windows.sh` is the same build with three environmental fixes that are not needed elsewhere:
`d8` and `apksigner` are `.bat` files bash will not resolve without the extension, `python3` is
usually just `python`, and `javac` cannot read MSYS-style `/c/...` paths, so the SDK root is
normalised to Windows form.

### What the build does

1. Compiles and runs **7 pure-Java test suites — 336 checks** — and **stops if any fail**.
2. Compiles resources with `aapt2`, the app with `javac`, converts to DEX with `d8`.
3. Packages, zipaligns, and signs the APK.
4. Writes `dist/ranna-<version>.apk` and `dist/SHA256SUMS.txt`, then verifies the signature.

`dist/` and `build/` are git-ignored. Nothing built is ever committed.

### Signing key — keep it

On first build the script generates a local development keystore at `build/keys/prototype.jks`.
It is **git-ignored and never published**; it is yours alone.

**Do not delete it.** Android only installs an update over an app signed with the *same* key. If you
lose it, your next build will not install over the one on your phone — you must uninstall Ranna
first, and **resolve any pending attempt before uninstalling**. It is a development key: never use
it for anything public.

### Running the tests alone

The logic core is deliberately Android-free and runs on a plain JVM:

```bash
javac -d build/tests app/src/main/java/in/nxprototype/app/PaymentCore.java tests/PaymentCoreTest.java
java -cp build/tests in.nxprototype.app.PaymentCoreTest
```

---

## Using it

1. Install your APK and open Ranna. Grant camera access if you want to scan.
2. **Scan**, **import**, or **paste** a static UPI QR. Check the name and address shown — then check
   the bank-resolved recipient again inside your UPI app, which is the only trustworthy one.
3. Start with **₹1** to a payee who can confirm receipt.
4. For split mode: enable the accessibility service (table above), tap **Split a TOTAL**, enter the
   total, choose 2 or 3 parts, read and tick the consent box, then review the full plan before
   starting.
5. Authenticate each part yourself. To stop at any point, simply **decline authentication** — that
   halts the whole sequence.
6. Check **every** receipt in your UPI app, then record your observation in Ranna to clear the lock.
7. Export the log (**Save** or **Share**) if you are diagnosing something.
8. Turn the accessibility service **off**.

Limits: **₹0.01 – ₹50,000** per payment and per split total; 2 or 3 parts. These are this
prototype's own ceilings and say nothing about what your bank, your UPI app, or UPI itself will
permit per transaction or per day.

---

## Privacy

Everything stays on the device.

- **No `INTERNET` permission.** The app cannot phone home. QR decoding is fully on-device.
- **Logs are an allow-list**, not a filter. `DebugLog` accepts only named event fields. It never
  receives raw QR payloads, payee names or addresses, amounts, PINs, OTPs, account numbers, bank
  references, raw callbacks, or exception messages.
- **Accessibility reads nothing into logs.** The navigation service records booleans and counts —
  *did the amount match, was exactly one Pay now control present* — never screen text.
- **Stored state** is a local attempt id, a lock, and a status in app-private preferences. Backup is
  disabled. The split plan lives only in memory and never survives a restart.
- **Exports** carry the app version, Android API level, and phone model, for diagnosis. No serial
  number, phone number, or advertising id. You choose when to share one.

---

## Repository layout

```
app/src/main/java/in/nxprototype/app/
  PaymentCore.java        URI/amount validation, callback classification   (pure Java, tested)
  SplitCore.java          paise-exact splitting, screen-text classifiers   (pure Java, tested)
  QueueCore.java          repeat-queue rules                               (pure Java, tested)
  AppChoices.java         UPI app discovery, fails closed                  (pure Java, tested)
  QrDecoder.java          ZXing passes: crops, inversion, binarizers       (pure Java, tested)
  ObserverSession.java    consent window for the diagnostic observer       (pure Java, tested)
  AssistSession.java      two-phase navigation authority                   (pure Java, tested)
  MainActivity.java       main screen, discovery, handoff, lock, console
  AssistedSplitActivity.java   split planning, consent, sequencing
  ScanActivity.java       camera scanner, no frame persistence
  CredAssistService.java  guarded navigation + post-payment dismissal
  CredObserverService.java  metadata-only diagnostic observer
  DebugLog.java           bounded, allow-listed local event log
  Brand.java              presentation only: names, palette, styling
  UiFlags.java            build-time UI switches
tests/                    plain-JVM test suites, run by the build
docs/                     design plan, per-release notes, verification
```

`docs/REQUIREMENTS-DESIGN-TASKS.md` is the working design document and the fullest account of what
is and is not established. `docs/RELEASE-*.md` record each iteration, including what failed.

---

## Limitations, stated plainly

- **Settlement is never verified.** There is no PSP status lookup. Only your receipt confirms money moved.
- **Split mode understands CRED's screens only**, as they looked during development. Any CRED UI
  change can break the matching — by design it then **pauses for manual navigation** rather than
  guessing. A real example: amounts above ₹999 render with Indian digit grouping (`₹1,666.67`), and
  until that was handled every larger part paused.
- **Accessibility matching is text-based** and therefore fragile. It is guarded so that failure is
  inaction, not a wrong action.
- **Stop cannot recall a payment** already handed to a UPI app.
- **Not distribution-ready.** No Play Store review, no production signing identity, no compliance
  work. Do not publish it.
- **Tested on one phone, by one person.** Pure-Java tests prove logic, not device behaviour.

---

## Licence

Apache License 2.0 — see [`LICENSE-APACHE-2.0.txt`](LICENSE-APACHE-2.0.txt) and
[`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md). ZXing core is Apache-2.0.

Provided **"as is", without warranty of any kind**, express or implied. See the licence for the full
disclaimer of warranty and limitation of liability.

---

<div align="center">

**ಕನ್ನಡಿಗರಿಂದ ಭಾರತಕ್ಕೆ**

*Built to learn. Read the code before you run it.*

</div>
