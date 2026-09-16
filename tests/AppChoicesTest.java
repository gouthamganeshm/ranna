package in.nxprototype.app;
import java.util.*;
public class AppChoicesTest {
 public static void main(String[] args){
  AppChoices.Candidate cred=new AppChoices.Candidate("cred","CRED",true,true,true),bhim=new AppChoices.Candidate("bhim","BHIM",true,true,true),erupee=new AppChoices.Candidate("erupee","e₹",true,true,true);
  List<AppChoices.Candidate> merged=AppChoices.merge(Arrays.asList(cred,bhim),Arrays.asList(erupee,cred),"own");
  if(merged.size()!=3 || !merged.get(0).pkg.equals("cred") || !merged.get(1).pkg.equals("bhim") || !merged.get(2).pkg.equals("erupee"))throw new AssertionError("Package-targeted apps must survive an incomplete generic list; duplicates removed");
  for(AppChoices.Candidate blocked:Arrays.asList(new AppChoices.Candidate("x","X",false,true,true),new AppChoices.Candidate("x","X",true,false,true),new AppChoices.Candidate("x","X",true,true,false),new AppChoices.Candidate("own","Own",true,true,true),null))
   if(!AppChoices.merge(Arrays.asList(blocked),Collections.emptyList(),"own").isEmpty())throw new AssertionError("Inaccessible target accepted");
  if(!AppChoices.merge(Collections.emptyList(),Collections.emptyList(),"own").isEmpty())throw new AssertionError();
  int stateChecks=0;
  for(int app=-1;app<=4;app++)for(int component=-1;component<=4;component++)for(boolean resolved:new boolean[]{false,true}){
   boolean expected=resolved && (app==0 || app==1) && (component==0 || component==1);
   if(AppChoices.effectivelyEnabled(resolved,app,component)!=expected)throw new AssertionError("Runtime enablement state matrix");
   stateChecks++;
  }
  // Reproduce the device case: raw flags false, but normal resolver returns the
  // handler and runtime settings do not explicitly disable it. Raw flags must
  // not independently veto eligibility.
  AppChoices.Candidate runtimeApp=new AppChoices.Candidate("cred","CRED",true,AppChoices.effectivelyEnabled(true,0,1),true);
  if(AppChoices.merge(Arrays.asList(runtimeApp),Arrays.asList(erupee),"own").size()!=2)throw new AssertionError("Runtime-enabled handler hidden");
  System.out.println("PASS: "+stateChecks+" runtime state cases and resolved-handler merge regression");
  System.out.println("PASS: 7 discovery regression scenarios (targeted-only apps, deduplication, inaccessible/self/null/empty targets)");
 }
}
