package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;
import uk.ac.kcl.MDEOptimiseStandaloneSetup;
import uk.ac.kcl.interpreter.OptimisationInterpreter;
import uk.ac.kcl.interpreter.algorithms.SimpleMO;
import uk.ac.kcl.mDEOptimise.Optimisation;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.CRAModelProvider;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.ModelLoadHelper;

@SuppressWarnings("all")
public class RunOptimisation {
  private final static Injector injector = new MDEOptimiseStandaloneSetup().createInjectorAndDoEMFRegistration();
  
  @Inject
  private ModelLoadHelper modelLoader;
  
  public void run() {
    SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("yyMMdd-HHmmss");
    Date _date = new Date();
    String _format = _simpleDateFormat.format(_date);
    final String pathPrefix = ("gen/models/ttc/" + _format);
    EObject _loadModel = this.modelLoader.loadModel("src/uk/ac/kcl/mdeoptimise/ttc16/opt_specs/ttc.mopt");
    final Optimisation model = ((Optimisation) _loadModel);
    final CRAModelProvider modelProvider = RunOptimisation.injector.<CRAModelProvider>getInstance(CRAModelProvider.class);
    SimpleMO _simpleMO = new SimpleMO(50, 10);
    final OptimisationInterpreter interpreter = new OptimisationInterpreter(model, _simpleMO, modelProvider);
    final Set<EObject> optimiserOutcome = interpreter.execute();
    final Function1<EObject, EList<EObject>> _function = (EObject cm) -> {
      Object _feature = this.getFeature(cm, "classes");
      return ((EList<EObject>) _feature);
    };
    Iterable<EList<EObject>> _map = IterableExtensions.<EObject, EList<EObject>>map(optimiserOutcome, _function);
    Iterable<EObject> _flatten = Iterables.<EObject>concat(_map);
    final Procedure2<EObject, Integer> _function_1 = (EObject cl, Integer i) -> {
      this.setFeature(cl, "name", ("NewClass" + i));
    };
    IterableExtensions.<EObject>forEach(_flatten, _function_1);
    modelProvider.storeModels(optimiserOutcome, (pathPrefix + "/final"));
  }
  
  public Object getFeature(final EObject o, final String feature) {
    EClass _eClass = o.eClass();
    EStructuralFeature _eStructuralFeature = _eClass.getEStructuralFeature(feature);
    return o.eGet(_eStructuralFeature);
  }
  
  public void setFeature(final EObject o, final String feature, final Object value) {
    EClass _eClass = o.eClass();
    EStructuralFeature _eStructuralFeature = _eClass.getEStructuralFeature(feature);
    o.eSet(_eStructuralFeature, value);
  }
  
  public static void main(final String[] args) {
    RunOptimisation _instance = RunOptimisation.injector.<RunOptimisation>getInstance(RunOptimisation.class);
    _instance.run();
  }
}
