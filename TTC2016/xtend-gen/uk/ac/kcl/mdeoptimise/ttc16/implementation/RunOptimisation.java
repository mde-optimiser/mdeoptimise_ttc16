package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.DoubleExtensions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;
import uk.ac.kcl.MDEOptimiseStandaloneSetup;
import uk.ac.kcl.interpreter.OptimisationInterpreter;
import uk.ac.kcl.interpreter.algorithms.SimpleMO;
import uk.ac.kcl.mDEOptimise.Optimisation;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.CRAModelProvider;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.MaximiseCRA;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.ModelLoadHelper;

@SuppressWarnings("all")
public class RunOptimisation {
  private final static Injector injector = new MDEOptimiseStandaloneSetup().createInjectorAndDoEMFRegistration();
  
  public static void main(final String[] args) {
    RunOptimisation _instance = RunOptimisation.injector.<RunOptimisation>getInstance(RunOptimisation.class);
    _instance.run();
  }
  
  @Inject
  private ModelLoadHelper modelLoader;
  
  /**
   * Run all experiments
   */
  public void run() {
    final List<String> optSpecs = Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("ttc"));
    final List<String> inputModels = Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("TTC_InputRDG_A", "TTC_InputRDG_B", "TTC_InputRDG_C", "TTC_InputRDG_D", "TTC_InputRDG_E"));
    final Consumer<String> _function = (String optSpec) -> {
      final Consumer<String> _function_1 = (String input) -> {
        this.runOneExperiment(optSpec, input);
      };
      inputModels.forEach(_function_1);
    };
    optSpecs.forEach(_function);
  }
  
  /**
   * Run a single experiment and record its outcomes
   */
  public void runOneExperiment(final String optSpecName, final String inputModelName) {
    try {
      InputOutput.<String>println((((("Starting experiment run for specification \"" + optSpecName) + "\" with input model \"") + inputModelName) + "\""));
      SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("yyMMdd-HHmmss");
      Date _date = new Date();
      String _format = _simpleDateFormat.format(_date);
      final String pathPrefix = ((((("gen/models/ttc/" + optSpecName) + "/") + inputModelName) + "/") + _format);
      EObject _loadModel = this.modelLoader.loadModel((("src/uk/ac/kcl/mdeoptimise/ttc16/opt_specs/" + optSpecName) + 
        ".mopt"));
      final Optimisation model = ((Optimisation) _loadModel);
      final CRAModelProvider modelProvider = RunOptimisation.injector.<CRAModelProvider>getInstance(CRAModelProvider.class);
      modelProvider.setInputModelName(inputModelName);
      final long startTime = System.nanoTime();
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
      final long endTime = System.nanoTime();
      final long totalTime = (endTime - startTime);
      modelProvider.storeModels(optimiserOutcome, (pathPrefix + "/final"));
      final File fResults = new File((pathPrefix + "/final/results.txt"));
      final PrintWriter pw = new PrintWriter(fResults);
      System.out.printf("Total time taken for this experiment: %02d milliseconds.\n", Long.valueOf((totalTime / 1000000)));
      pw.printf("Total time taken for this experiment: %02d milliseconds.\n", Long.valueOf((totalTime / 1000000)));
      final MaximiseCRA craComputer = new MaximiseCRA();
      final Function1<EObject, Pair<EObject, Double>> _function_2 = (EObject m) -> {
        double _computeFitness = craComputer.computeFitness(m);
        return new Pair<EObject, Double>(m, Double.valueOf(_computeFitness));
      };
      Iterable<Pair<EObject, Double>> _map_1 = IterableExtensions.<EObject, Pair<EObject, Double>>map(optimiserOutcome, _function_2);
      final Function1<Pair<EObject, Double>, Double> _function_3 = (Pair<EObject, Double> it) -> {
        Double _value = it.getValue();
        return Double.valueOf(DoubleExtensions.operator_minus(_value));
      };
      List<Pair<EObject, Double>> _sortBy = IterableExtensions.<Pair<EObject, Double>, Double>sortBy(_map_1, _function_3);
      final Consumer<Pair<EObject, Double>> _function_4 = (Pair<EObject, Double> p) -> {
        EObject _key = p.getKey();
        int _hashCode = _key.hashCode();
        Double _value = p.getValue();
        System.out.printf("Result model %08X at CRA %02f.\n", Integer.valueOf(_hashCode), _value);
        EObject _key_1 = p.getKey();
        int _hashCode_1 = _key_1.hashCode();
        Double _value_1 = p.getValue();
        pw.printf("Result model %08X at CRA %02f.\n", Integer.valueOf(_hashCode_1), _value_1);
      };
      _sortBy.forEach(_function_4);
      pw.close();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
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
}
