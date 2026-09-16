package in.nxprototype.app;
import java.util.*;
/** Merge independently resolved package targets with generic matches. Fail closed on inaccessible targets. */
public final class AppChoices {
 // States match PackageManager COMPONENT_ENABLED_STATE_* (0 default, 1 enabled,
 // 2 disabled, 3 disabled by user, 4 disabled until used). Unknown fails closed.
 // Caller must resolve WITHOUT MATCH_DISABLED_COMPONENTS. Raw manifest flags
 // alone are not an effective runtime enablement check.
 public static boolean effectivelyEnabled(boolean resolved,int appState,int componentState){
  return resolved && (appState==0 || appState==1) && (componentState==0 || componentState==1);
 }
 public static final class Candidate {
  public final String pkg,label; public final boolean exported,enabled,permitted;
  public Candidate(String p,String l,boolean e,boolean n,boolean a){pkg=p;label=l;exported=e;enabled=n;permitted=a;}
 }
 public static List<Candidate> merge(List<Candidate> targeted,List<Candidate> generic,String own){
  LinkedHashMap<String,Candidate> out=new LinkedHashMap<>();
  for(List<Candidate> group:Arrays.asList(targeted,generic))for(Candidate c:group)
   if(c!=null && c.pkg!=null && !c.pkg.equals(own) && c.exported && c.enabled && c.permitted && !out.containsKey(c.pkg))out.put(c.pkg,c);
  return new ArrayList<>(out.values());
 }
}
