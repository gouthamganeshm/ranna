# 0.1.12 — Grouped amounts, and release preparation

## Fix: parts above Rs 999 never got pressed
Splitting Rs 5,000 into 3 produced Rs 1,666.67 / Rs 1,666.67 / Rs 1,666.66, and the navigation
helper stopped pressing Pay now. Small amounts kept working, which is what made it look like a
regression in 0.1.11 rather than a limit that had simply never been crossed.

CRED renders any amount over a thousand with Indian digit grouping: **Rs 1,666.67**, not
Rs 1666.67. Both amount patterns were digits-and-dot only, so a grouped label failed the
"looks like an amount" test, was never compared, left `amount_match=false`, and the guard correctly
refused to press anything. The guard was right; the pattern was wrong.

`SplitCore.amountMatches` now strips grouping separators - comma, non-breaking space, narrow
no-break space, plain space - before comparing. The comparison itself is unchanged and still exact,
so a genuinely different amount still fails and still pauses the run. The on-screen amount pattern
moved out of `CredAssistService` into `SplitCore.looksLikeAmount`, which accepts grouping and is now
unit tested rather than living as an untested literal inside the service.

This was present from 0.1.10, when Pay now pressing was added, and was masked by testing at Rs 1
and Rs 2 only.

## Fix: a screen too large to read failed silently
The node walk stopped after 250 nodes and returned with no log at all, so an unverified screen and a
mismatched screen looked identical in the export. The cap is now 800 - CRED's payment screen carries
a bank list and promotional rows - and hitting it logs `ASSIST_PAUSED reason=screen_not_fully_read`
with the node count. Behaviour is unchanged: a screen that cannot be fully read is never acted on.

## Release preparation
- `README.md` rewritten for a public repository: educational-purpose framing, an explicit statement
  that nothing was reverse engineered and that only public documented Android APIs are used, an
  explicit no-compliance-claim and use-at-your-own-risk section, required phone settings including
  the Android 13+ "Allow restricted settings" step that otherwise blocks Accessibility for a
  sideloaded app, the fingerprint prerequisite, and full build-from-source instructions.
- `.gitignore` excludes `dist/`, `build/` and all keystores. **No APK is committed or released.**
  Anyone who wants to run it builds from source with their own key.
- `build-windows.sh` added: the Windows build actually used throughout development, now portable
  rather than carrying absolute paths.
- The scanner self-test QR moved from `dist/` to `docs/assets/`, since `dist/` is now ignored.

## Validation
336 checks across seven suites, all passing, including twenty new cases covering grouped amounts,
the exact Rs 5,000-in-3 plan, and rejection of a grouped amount that does not match the plan.
Verified from a clean tree with `dist/` and `build/` deleted. Signed; v2 and v3 verify.

## Outstanding phone checks
- Rs 5,000 in 3 parts: each part now presses Pay now and lands on authentication.
- A part whose amount does not match still pauses rather than pressing.
- `ASSIST_CHECK` shows `amount_match=true` for a grouped amount such as Rs 1,666.67.
