# 0.1.9 — Post-payment dismissal, and a real stop

First successful device run of the split sequence (0.1.8, two parts of Rs 1, both received).
The owner recording shows the sequence works but needs three manual taps per part after
authentication, and it exposed one wrong assumption.

## Finding: the intent flow has no Swipe to pay
The 0.1.7 navigation helper was designed from a recording of CRED's own in-app QR flow, which
ends in a "Swipe to pay" slider. The flow this app actually uses is the external `upi://pay`
intent, and its payment screen is different: recipient, amount, a "get assured rewards with
CRED UPI" line, a RECOMMENDED METHODS account list, and a black **Pay now** button. There is no
slider. The pre-authentication swipe therefore never matched and never fired in the device run;
the owner pressed Pay now manually. That code path is retained unchanged but should be treated
as unexercised until a device log shows `ASSIST_CHECK` with `swipe_controls=1`.

Pay now is the human approval point and stays manual by design.

## Added: post-payment dismissal
After a payment completes CRED shows, in order: a success screen ("PAID SUCCESSFULLY TO", close
control), a "claim your reward" sheet with a "Claim now" button, and a transient
"good news: you've earned a reward!" banner. The sequence stalled there until dismissed by hand.

`AssistSession` now carries a second, independent phase. The pre-authentication phase is still
revoked before the swipe is dispatched; the dismissal phase is armed when the part launches,
survives the authentication screen, and expires after 180 seconds or 4 actions, whichever is
first. Revoking one never grants or withdraws the other.

While that phase is live the helper may do exactly two things, each once, at least 1.2 s apart,
and only when a success marker is visible and no authentication marker is:
- flick the reward banner up and off screen, using the banner's own bounds;
- click a close control, matched only on explicit close semantics (text or description "close"
  or "dismiss" or a close glyph, or a view id ending in a close name).

It cannot approve anything. `SplitCore.offerControl` excludes "Pay now", "Swipe to pay", "Claim"
and "Claim now" from ever being treated as dismissal targets, and a bare "x" is not accepted as
a close glyph. Both classifiers are pure Java and unit tested.

## Fixed: declining authentication now actually stops the run
Previously, if CRED returned no result at all the activity stayed in `awaiting` indefinitely.
`onActivityResult` always precedes `onResume`, so a part still awaiting when the screen resumes
means the human declined, backed out, or dismissed CRED without completing. That now records
status `NO_CALLBACK`, revokes both assistance phases and stops the whole sequence. Refusing to
authenticate is a supported way to stop, and it stops everything, not just the current part.

A cancelled or failed result already stopped the run and still does. Stop never recalls a
payment already sent.

## Validation
121 split and navigation-session checks (up from 71); 284 checks across seven suites, all
passing. Signed with the same development key; v2 and v3 verify. No emulator or phone run here.

## Outstanding phone checks
- Reward banner and close control are exposed as accessibility nodes; read `ASSIST_RESULT` for
  `success_marker`, `banner` and `close_control` before assuming either is reachable.
- Dismissal fires once per part, never during authentication, and never presses Claim now.
- After dismissal CRED returns an intent result so the next part opens on its own.
- Declining authentication on part 1 stops the sequence and leaves part 2 unsent.
- The unexercised Swipe to pay path: confirm whether any CRED flow reached from this app shows it.
