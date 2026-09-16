# 0.1.10 — Lands on authentication, and a Rs 50,000 ceiling

Second device run (0.1.9) completed a three-part split; all three parts were received and the
reward banner and result screen were dismissed without help. The remaining manual step was
pressing Pay now once per part.

## Each part now lands on authentication
The helper presses CRED's **Pay now** control itself, so a part opens and arrives at the
fingerprint or UPI PIN prompt with no tap in between. It presses Pay now only when, on the same
screen: the amount matches this part's planned amount exactly, no conflicting currency value is
visible, the recipient matches by bank-resolved name or by payee address, exactly one Pay now
control is present, and no authentication marker is visible. Authority is revoked before the
press. There is no retry.

The intent-flow payment screen shows the bank-resolved name and no payee address, so the
recipient is matched on either signal. `ASSIST_CHECK` now logs `payee_vpa_match` and
`payee_name_match` separately, plus `pay_controls` and `pay_node`, so a part that pauses can be
diagnosed from the export.

### What this changes about approval
Pressing Pay now is navigation to the authentication prompt; it authorises nothing. The debit
still requires the fingerprint or UPI PIN, that stays with the human, and CRED and the PSP
enforce it - this app cannot supply, replay or bypass it. But it does remove a review step:
previously the human saw the recipient and amount on the CRED screen before pressing Pay now,
and now the first thing they see is the authentication prompt, which shows the amount but not
the recipient. The recipient check has therefore moved into the guard above, and into the split
plan the user approves before the run. Declining authentication still stops the whole sequence.
Claim now is still never pressed.

## Limit raised to Rs 50,000
Per-payment and per-split-total ceilings both move from Rs 2,000 / Rs 6,000 to Rs 50,000; parts
stay at 2 or 3. Amount patterns widen from four to five integer digits in `PaymentCore.money`,
`SplitCore.parts`, `SplitCore.amountMatches` and the helper's on-screen amount pattern, so a
five-digit amount is matched rather than ignored. This is a prototype ceiling, not a claim about
what CRED, the account's bank or UPI will allow for a single transfer or in a day.

Larger amounts make the guards matter more, not less: a mismatched amount or recipient pauses
the run for manual navigation rather than pressing anything.

## Validation
149 split and navigation-session checks; 314 across seven suites, all passing. Signed with the
same development key; v2 and v3 verify. No emulator or phone run here. Re-test at Rs 2 total
before any larger amount.

## Outstanding phone checks
- Pay now is exposed as a clickable node and is pressed once per part; read `ASSIST_CHECK` for
  `pay_controls=1` and `pay_node=true`.
- The recipient matches by name on the device; if `payee_name_match=false` the run will pause.
- A part whose amount does not match pauses instead of pressing.
- Declining authentication on part 1 leaves later parts unsent.
- A five-digit part amount is matched by the helper, not treated as a conflict.
