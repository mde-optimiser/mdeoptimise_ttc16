package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.eclipse.xtext.xbase.lib.ExclusiveRange;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
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
import uk.ac.kcl.mdeoptimise.ttc16.implementation.MinimiseClasslessFeatures;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.ModelLoadHelper;

@SuppressWarnings("all")
public class RunOptimisation {
  private static class ResultRecord {
    public double timeTaken;
    
    public double maxCRA;
    
    public long bestModelHashCode;
    
    public boolean hasUnassignedFeatures;
  }
  
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
    final List<String> inputModels = Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("TTC_InputRDG_C", "TTC_InputRDG_D", "TTC_InputRDG_E"));
    final HashMap<Pair<String, String>, List<RunOptimisation.ResultRecord>> resultCollector = new HashMap<Pair<String, String>, List<RunOptimisation.ResultRecord>>();
    final Consumer<String> _function = (String optSpec) -> {
      final Consumer<String> _function_1 = (String input) -> {
        final LinkedList<RunOptimisation.ResultRecord> lResults = new LinkedList<RunOptimisation.ResultRecord>();
        ExclusiveRange _doubleDotLessThan = new ExclusiveRange(0, 10, true);
        final Consumer<Integer> _function_2 = (Integer idx) -> {
          RunOptimisation.ResultRecord _runOneExperiment = this.runOneExperiment(optSpec, input, (idx).intValue());
          lResults.add(_runOneExperiment);
        };
        _doubleDotLessThan.forEach(_function_2);
        Pair<String, String> _pair = new Pair<String, String>(optSpec, input);
        resultCollector.put(_pair, lResults);
      };
      inputModels.forEach(_function_1);
    };
    optSpecs.forEach(_function);
    Set<Pair<String, String>> _keySet = resultCollector.keySet();
    final Consumer<Pair<String, String>> _function_1 = (Pair<String, String> experiment) -> {
      try {
        final List<RunOptimisation.ResultRecord> lResults = resultCollector.get(experiment);
        String _key = experiment.getKey();
        String _plus = ("gen/models/ttc/" + _key);
        String _plus_1 = (_plus + "/");
        String _value = experiment.getValue();
        String _plus_2 = (_plus_1 + _value);
        String _plus_3 = (_plus_2 + "/overall_results");
        SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("yyMMdd-HHmmss");
        Date _date = new Date();
        String _format = _simpleDateFormat.format(_date);
        String _plus_4 = (_plus_3 + _format);
        String _plus_5 = (_plus_4 + ".txt");
        final File f = new File(_plus_5);
        final PrintWriter pw = new PrintWriter(f);
        pw.println("Overall results for this experiment");
        pw.println("===================================");
        pw.println();
        final Function2<Double, RunOptimisation.ResultRecord, Double> _function_2 = (Double acc, RunOptimisation.ResultRecord r) -> {
          return Double.valueOf(((acc).doubleValue() + r.timeTaken));
        };
        Double _fold = IterableExtensions.<RunOptimisation.ResultRecord, Double>fold(lResults, Double.valueOf(0.0), _function_2);
        int _size = lResults.size();
        double _divide = ((_fold).doubleValue() / _size);
        pw.printf("Average time taken: %02f milliseconds.\n", Double.valueOf(_divide));
        final Function1<RunOptimisation.ResultRecord, Double> _function_3 = (RunOptimisation.ResultRecord it) -> {
          return Double.valueOf(it.maxCRA);
        };
        final RunOptimisation.ResultRecord bestResult = IterableExtensions.<RunOptimisation.ResultRecord, Double>maxBy(lResults, _function_3);
        String _xifexpression = null;
        if (bestResult.hasUnassignedFeatures) {
          _xifexpression = "invalid";
        } else {
          _xifexpression = "valid";
        }
        pw.printf("Best CRA was %02f for model with hash code %08X. This model was %s.\n", Double.valueOf(bestResult.maxCRA), 
          Long.valueOf(bestResult.bestModelHashCode), _xifexpression);
        pw.close();
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    _keySet.forEach(_function_1);
  }
  
  /**
   * Run a single experiment and record its outcomes
   */
  public RunOptimisation.ResultRecord runOneExperiment(final String optSpecName, final String inputModelName, final int runIdx) {
    try {
      System.out.printf("Starting %01dth experiment run for specification \"%s\" with input model \"%s\".\n", Integer.valueOf(runIdx), optSpecName, inputModelName);
      SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("yyMMdd-HHmmss");
      Date _date = new Date();
      String _format = _simpleDateFormat.format(_date);
      final String pathPrefix = ((((((("gen/models/ttc/" + optSpecName) + "/") + inputModelName) + "/") + Integer.valueOf(runIdx)) + "/") + _format);
      EObject _loadModel = this.modelLoader.loadModel((("src/uk/ac/kcl/mdeoptimise/ttc16/opt_specs/" + optSpecName) + 
        ".mopt"));
      final Optimisation model = ((Optimisation) _loadModel);
      final CRAModelProvider modelProvider = RunOptimisation.injector.<CRAModelProvider>getInstance(CRAModelProvider.class);
      modelProvider.setInputModelName(inputModelName);
      final long startTime = System.nanoTime();
      SimpleMO _simpleMO = new SimpleMO(1000, 50);
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
      final RunOptimisation.ResultRecord results = new RunOptimisation.ResultRecord();
      final MaximiseCRA craComputer = new MaximiseCRA();
      final MinimiseClasslessFeatures featureCounter = new MinimiseClasslessFeatures();
      results.timeTaken = (totalTime / 1000000);
      final Function1<EObject, Boolean> _function_2 = (EObject m) -> {
        double _computeFitness = featureCounter.computeFitness(m);
        return Boolean.valueOf((_computeFitness == 0));
      };
      Iterable<EObject> _filter = IterableExtensions.<EObject>filter(optimiserOutcome, _function_2);
      final Function1<EObject, Pair<EObject, Double>> _function_3 = (EObject m) -> {
        double _computeFitness = craComputer.computeFitness(m);
        return new Pair<EObject, Double>(m, Double.valueOf(_computeFitness));
      };
      Iterable<Pair<EObject, Double>> _map_1 = IterableExtensions.<EObject, Pair<EObject, Double>>map(_filter, _function_3);
      final Function1<Pair<EObject, Double>, Double> _function_4 = (Pair<EObject, Double> it) -> {
        Double _value = it.getValue();
        return Double.valueOf(DoubleExtensions.operator_minus(_value));
      };
      final List<Pair<EObject, Double>> sortedResults = IterableExtensions.<Pair<EObject, Double>, Double>sortBy(_map_1, _function_4);
      boolean _isEmpty = sortedResults.isEmpty();
      if (_isEmpty) {
        InputOutput.<String>println("No valid results for this run");
      } else {
        Pair<EObject, Double> _head = IterableExtensions.<Pair<EObject, Double>>head(sortedResults);
        EObject _key = _head.getKey();
        int _hashCode = _key.hashCode();
        results.bestModelHashCode = _hashCode;
        Pair<EObject, Double> _head_1 = IterableExtensions.<Pair<EObject, Double>>head(sortedResults);
        Double _value = _head_1.getValue();
        results.maxCRA = (_value).doubleValue();
        results.hasUnassignedFeatures = false;
        final File fResults = new File((pathPrefix + "/final/results.txt"));
        final PrintWriter pw = new PrintWriter(fResults);
        System.out.printf("Total time taken for this experiment: %02f milliseconds.\n", Double.valueOf(results.timeTaken));
        pw.printf("Total time taken for this experiment: %02f milliseconds.\n", Double.valueOf(results.timeTaken));
        final Consumer<Pair<EObject, Double>> _function_5 = (Pair<EObject, Double> p) -> {
          EObject _key_1 = p.getKey();
          int _hashCode_1 = _key_1.hashCode();
          Double _value_1 = p.getValue();
          System.out.printf("Result model %08X at CRA %02f.\n", Integer.valueOf(_hashCode_1), _value_1);
          EObject _key_2 = p.getKey();
          int _hashCode_2 = _key_2.hashCode();
          Double _value_2 = p.getValue();
          pw.printf("Result model %08X at CRA %02f.\n", Integer.valueOf(_hashCode_2), _value_2);
        };
        sortedResults.forEach(_function_5);
        pw.close();
      }
      return results;
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
