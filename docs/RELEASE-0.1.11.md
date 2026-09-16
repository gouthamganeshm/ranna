# 0.1.11 — Ranna: name, tagline and a warmer surface

Third device run (0.1.10) behaved as intended: each part opened and landed straight on the
fingerprint prompt, and the owner confirmed the sequence end to end. This release is presentation
only.

## Name
The wrapper is now **Ranna**, after the classical Kannada poet, one of the three gems of old
Kannada literature. The launcher label, both accessibility service labels, the log export header
and the share subject all use it. The Android package id is unchanged at `in.nxprototype.app`, so
this still installs as an update over 0.1.10 with the same development key.

## Tagline and educational notice
Every screen — main, split and scanner — now carries the Kannada tagline
ಕನ್ನಡಿಗರಿಂದ ಭಾರತಕ್ಕೆ with a roman transliteration beneath it, and the notice
"For educational and research purposes only. Not a product, not a payment service." The tagline
appears near the masthead and again in the footer of the scrolling screens, and above the controls
on the scanner. Both accessibility service descriptions now name Ranna and state the educational
purpose, so the text Android shows in Accessibility settings matches.

## Surface
A warmer, simpler palette replaces the muted sage one: cream ground, marigold primary, leaf green
for the split action, teal for diagnostic sections, ink brown for body text. Buttons are rounded
and spaced, with the two primary actions filled and the rest outlined on the cream ground. The
live console keeps a dark card for legibility, now brown with amber text. The launcher icon is a
marigold hexagon with a stylised R.

New `Brand.java` holds the names, the palette and the view styling. It is presentation only: no
payment, navigation, validation or logging behaviour lives there, and nothing in the payment core,
queue, split planner or accessibility services reads it.

## What did not change
No behavioural code was touched. `PaymentCore`, `SplitCore`, `QueueCore`, `AssistSession`,
`CredAssistService`, `CredObserverService`, `QrDecoder`, `AppChoices` and `DebugLog` keep their
logic; the only edit outside presentation is the export header string. Guards, limits, consent
gating, the dismissal phase and the stop behaviour are all as shipped in 0.1.10.

## Validation
314 checks across seven suites, all passing, unchanged from 0.1.10. The Kannada tagline is
verified present in both `resources.arsc` and `classes.dex` of the built APK, so the UTF-8 text
survived compilation and packaging. Signed with the same development key; v2 and v3 verify.
Glyph rendering itself is device dependent and needs a look on the phone.

## Outstanding phone checks
- The Kannada tagline renders correctly rather than as tofu boxes.
- The launcher shows "Ranna" and the new icon.
- Accessibility settings show the two Ranna service names and the updated descriptions.
- Text stays legible against the cream ground in sunlight and in the device dark theme.
