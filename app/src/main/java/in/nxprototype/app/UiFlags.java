package in.nxprototype.app;
/** Build-time UI switches for owner device testing. */
public final class UiFlags {
 /** true lets Android screenshot and screen-record our own screens by omitting FLAG_SECURE.
  *  Enabled to capture device-test evidence. CRED's screens stay protected either way:
  *  its authentication surfaces set FLAG_SECURE themselves and record as black. */
 public static final boolean ALLOW_SCREEN_RECORDING=true;
}
