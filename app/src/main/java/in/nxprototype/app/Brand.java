package in.nxprototype.app;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;

/** Presentation only: names, colours and view styling. No payment, navigation,
 *  validation or logging behaviour lives here, and nothing here is read by the
 *  payment core, the queue, the split planner or the accessibility services. */
public final class Brand {
 public static final String NAME="Ranna";
 /** Named for Ranna, one of the three gems of old Kannada poetry. */
 public static final String SUBTITLE="named after the classical Kannada poet";
 public static final String TAGLINE="ಕನ್ನಡಿಗರಿಂದ ಭಾರತಕ್ಕೆ";
 public static final String TAGLINE_ROMAN="Kannadigarinda Bharatakke · from Kannadigas to India";
 public static final String EDUCATIONAL="For educational and research purposes only. Not a product, not a payment service.";

 // A warm, simple palette: cream paper, marigold, leaf green, ink brown.
 public static final int BG=0xfffff6e6;
 public static final int CARD=0xffffffff;
 public static final int INK=0xff2f2013;
 public static final int MUTED=0xff8a7358;
 public static final int PRIMARY=0xffe8641c;
 public static final int SUN=0xfff5a524;
 public static final int LEAF=0xff12a150;
 public static final int SKY=0xff0e9aa7;
 public static final int LINE=0xffefdcbb;
 public static final int CONSOLE_BG=0xff2f2013;
 public static final int CONSOLE_INK=0xffffd79a;

 public static int dp(Context c,int x){return (int)(x*c.getResources().getDisplayMetrics().density);}

 private static GradientDrawable round(int fill,int stroke,int radius,int width,Context c){
  GradientDrawable g=new GradientDrawable();
  g.setColor(fill);g.setCornerRadius(dp(c,radius));
  if(width>0)g.setStroke(dp(c,width),stroke);
  return g;
 }

 /** Filled action button. */
 public static void primary(Button b,int fill){
  Context c=b.getContext();
  b.setAllCaps(false);b.setTextColor(0xffffffff);b.setTextSize(16);
  b.setTypeface(Typeface.DEFAULT_BOLD);
  b.setBackground(round(fill,0,14,0,c));
  b.setPadding(dp(c,18),dp(c,14),dp(c,18),dp(c,14));
  b.setStateListAnimator(null);
 }

 /** Outlined secondary button on the cream ground. */
 public static void quiet(Button b){
  Context c=b.getContext();
  b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(15);
  b.setBackground(round(CARD,LINE,14,1,c));
  b.setPadding(dp(c,18),dp(c,13),dp(c,18),dp(c,13));
  b.setStateListAnimator(null);
 }

 /** The Kannada tagline, shown on every screen. */
 public static TextView tagline(Context c,boolean onDark){
  TextView t=new TextView(c);
  t.setText(TAGLINE+"\n"+TAGLINE_ROMAN);
  t.setTextSize(15);
  t.setGravity(Gravity.CENTER);
  t.setLineSpacing(0,1.15f);
  t.setTextColor(onDark?CONSOLE_INK:PRIMARY);
  t.setBackground(round(onDark?0x22ffffff:0xfffde8c8,0,14,0,c));
  t.setPadding(dp(c,14),dp(c,10),dp(c,14),dp(c,10));
  return t;
 }

 /** The educational-use notice, shown on every screen. */
 public static TextView educational(Context c,boolean onDark){
  TextView t=new TextView(c);
  t.setText(EDUCATIONAL);
  t.setTextSize(13);
  t.setGravity(Gravity.CENTER);
  t.setTextColor(onDark?0xffe8c79a:MUTED);
  t.setPadding(dp(c,8),dp(c,8),dp(c,8),dp(c,8));
  return t;
 }

 /** Tagline plus educational notice as one block, for the foot of any screen. */
 public static LinearLayout footer(Context c,boolean onDark){
  LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);
  box.setPadding(0,dp(c,14),0,dp(c,4));
  box.addView(tagline(c,onDark));
  box.addView(educational(c,onDark));
  return box;
 }
}
