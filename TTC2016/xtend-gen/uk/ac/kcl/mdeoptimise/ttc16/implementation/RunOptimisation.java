package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.DoubleExtensions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.ExclusiveRange;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.IteratorExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;
import org.sidiff.common.logging.LogUtil;
import uk.ac.kcl.MDEOptimiseStandaloneSetup;
import uk.ac.kcl.interpreter.OptimisationInterpreter;
import uk.ac.kcl.mdeoptimise.Optimisation;
import uk.ac.kcl.mdeoptimise.OptimisationSpec;
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
    
    public String bestModelPath;
  }
  
  private static class InputModelDesc {
    public String modelName;
    
    public int generations;
    
    public int populationSize;
    
    public InputModelDesc(final String modelName, final int generations, final int populationSize) {
      this.modelName = modelName;
      this.generations = generations;
      this.populationSize = populationSize;
    }
  }
  
  private final static Injector injector = new MDEOptimiseStandaloneSetup().createInjectorAndDoEMFRegistration();
  
  public static void main(final String[] args) {
    final RunOptimisation app = RunOptimisation.injector.<RunOptimisation>getInstance(RunOptimisation.class);
    LogUtil.setLogEvents("MESSAGE,WARNING,ERROR");
    boolean _isEmpty = ((List<String>)Conversions.doWrapArray(args)).isEmpty();
    if (_isEmpty) {
      app.run();
    } else {
      String _get = args[0];
      final int specIdx = Integer.parseInt(_get);
      String _get_1 = args[1];
      final int modelIdx = Integer.parseInt(_get_1);
      SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("yyMMdd-HHmmss");
      Date _date = new Date();
      final String batchStartTime = _simpleDateFormat.format(_date);
      String _get_2 = RunOptimisation.optSpecs.get(specIdx);
      RunOptimisation.InputModelDesc _get_3 = RunOptimisation.inputModels.get(modelIdx);
      app.runBatchForSpecAndModel(_get_2, _get_3, batchStartTime, 0);
    }
  }
  
  @Inject
  private ModelLoadHelper modelLoader;
  
  /**
   * Defining the experiments
   */
  private final static List<String> optSpecs = Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("ttc"));
  
  private final static List<RunOptimisation.InputModelDesc> inputModels = Collections.<RunOptimisation.InputModelDesc>unmodifiableList(CollectionLiterals.<RunOptimisation.InputModelDesc>newArrayList(new RunOptimisation.InputModelDesc("TTC_InputRDG_A", 100, 40), new RunOptimisation.InputModelDesc("TTC_InputRDG_B", 100, 40), new RunOptimisation.InputModelDesc("TTC_InputRDG_C", 100, 40), new RunOptimisation.InputModelDesc("TTC_InputRDG_D", 100, 40), new RunOptimisation.InputModelDesc("TTC_InputRDG_E", 100, 40), new RunOptimisation.InputModelDesc("TTC_InputRDG_A", 500, 40), new RunOptimisation.InputModelDesc("TTC_InputRDG_B", 500, 40), new RunOptimisation.InputModelDesc("TTC_InputRDG_C", 500, 40), new RunOptimisation.InputModelDesc("TTC_InputRDG_D", 500, 40), new RunOptimisation.InputModelDesc("TTC_InputRDG_E", 500, 40)));
  
  /**
   * Run all experiments
   */
  public void run() {
    SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("yyMMdd-HHmmss");
    Date _date = new Date();
    final String batchStartTime = _simpleDateFormat.format(_date);
    final Consumer<String> _function = (String optSpec) -> {
      final Procedure2<RunOptimisation.InputModelDesc, Integer> _function_1 = (RunOptimisation.InputModelDesc inputDesc, Integer index) -> {
        this.runBatchForSpecAndModel(optSpec, inputDesc, batchStartTime, (index).intValue());
      };
      IterableExtensions.<RunOptimisation.InputModelDesc>forEach(RunOptimisation.inputModels, _function_1);
    };
    RunOptimisation.optSpecs.forEach(_function);
  }
  
  /**
   * Run a batch of experiments for the given spec and model, recording overall outcomes in a separate file.
   */
  public void runBatchForSpecAndModel(final String optSpec, final RunOptimisation.InputModelDesc inputDesc, final String batchStartTime, final int batchId) {
    try {
      final LinkedList<RunOptimisation.ResultRecord> lResults = new LinkedList<RunOptimisation.ResultRecord>();
      ExclusiveRange _doubleDotLessThan = new ExclusiveRange(0, 10, true);
      final Consumer<Integer> _function = (Integer idx) -> {
        RunOptimisation.ResultRecord _runOneExperiment = this.runOneExperiment(optSpec, inputDesc, batchStartTime, batchId, (idx).intValue());
        lResults.add(_runOneExperiment);
      };
      _doubleDotLessThan.forEach(_function);
      final Function2<Double, RunOptimisation.ResultRecord, Double> _function_1 = (Double acc, RunOptimisation.ResultRecord r) -> {
        return Double.valueOf(((acc).doubleValue() + r.timeTaken));
      };
      Double _fold = IterableExtensions.<RunOptimisation.ResultRecord, Double>fold(lResults, Double.valueOf(0.0), _function_1);
      int _size = lResults.size();
      final double averageTimeMiliseconds = ((_fold).doubleValue() / _size);
      long _longValue = Double.valueOf(averageTimeMiliseconds).longValue();
      Date date = new Date(_longValue);
      SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
      TimeZone _timeZone = TimeZone.getTimeZone("UTC");
      formatter.setTimeZone(_timeZone);
      long _time = date.getTime();
      String result = formatter.format(Long.valueOf(_time));
      final File f = new File(
        (((((((("gen/models/ttc/" + optSpec) + "/") + batchStartTime) + "/") + inputDesc.modelName) + "-configuration-") + Integer.valueOf(batchId)) + "/overall_results.txt"));
      final PrintWriter pw = new PrintWriter(f);
      pw.println("Overall results for this experiment");
      pw.println("===================================");
      pw.println();
      pw.printf("Experiment with spec \"%s\" and model \"%s\".\n", optSpec, inputDesc.modelName);
      pw.printf("Running for %01d generations with a population size of %01d.\n", Integer.valueOf(inputDesc.generations), 
        Integer.valueOf(inputDesc.populationSize));
      pw.println();
      pw.printf("Average time taken: %02f milliseconds (%s).\n", Double.valueOf(averageTimeMiliseconds), result);
      final Function1<RunOptimisation.ResultRecord, Double> _function_2 = (RunOptimisation.ResultRecord it) -> {
        return Double.valueOf(it.maxCRA);
      };
      final RunOptimisation.ResultRecord bestResult = IterableExtensions.<RunOptimisation.ResultRecord, Double>maxBy(lResults, _function_2);
      boolean _equals = Objects.equal(bestResult.bestModelPath, null);
      if (_equals) {
        pw.printf("No valid solutions found for this experiment.");
      } else {
        String _xifexpression = null;
        if (bestResult.hasUnassignedFeatures) {
          _xifexpression = "invalid";
        } else {
          _xifexpression = "valid";
        }
        pw.printf("Best CRA was %s for model with hash code %08X. This model was %s.\n", Double.valueOf(bestResult.maxCRA), 
          Long.valueOf(bestResult.bestModelHashCode), _xifexpression);
        pw.println();
        pw.println("Evaluation: CRAIndexCalculator.jar");
        pw.println("===================================");
        pw.println();
        pw.printf("Model path: %s\n", bestResult.bestModelPath);
        String _runEvaluationJarAgainstBestModel = this.runEvaluationJarAgainstBestModel(bestResult.bestModelPath);
        pw.print(_runEvaluationJarAgainstBestModel);
      }
      pw.close();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public String runEvaluationJarAgainstBestModel(final String modelPath) {
    try {
      String _xblockexpression = null;
      {
        Runtime _runtime = Runtime.getRuntime();
        Process evaluatorJar = _runtime.exec(("java -jar evaluation/CRAIndexCalculator.jar " + modelPath));
        InputStream _inputStream = evaluatorJar.getInputStream();
        InputStreamReader _inputStreamReader = new InputStreamReader(_inputStream);
        BufferedReader _bufferedReader = new BufferedReader(_inputStreamReader);
        Stream<String> _lines = _bufferedReader.lines();
        Stream<String> _parallel = _lines.parallel();
        Collector<CharSequence, ?, String> _joining = Collectors.joining("\n");
        String output = _parallel.collect(_joining);
        int _length = output.length();
        boolean _equals = (_length == 0);
        if (_equals) {
          String _output = output;
          InputStream _errorStream = evaluatorJar.getErrorStream();
          InputStreamReader _inputStreamReader_1 = new InputStreamReader(_errorStream);
          BufferedReader _bufferedReader_1 = new BufferedReader(_inputStreamReader_1);
          Stream<String> _lines_1 = _bufferedReader_1.lines();
          Stream<String> _parallel_1 = _lines_1.parallel();
          Collector<CharSequence, ?, String> _joining_1 = Collectors.joining("\n");
          String _collect = _parallel_1.collect(_joining_1);
          output = (_output + _collect);
        }
        _xblockexpression = output;
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * Run a single experiment and record its outcomes
   */
  public RunOptimisation.ResultRecord runOneExperiment(final String optSpecName, final RunOptimisation.InputModelDesc inputDesc, final String batchStartTime, final int batchId, final int runIdx) {
    try {
      System.out.printf("Starting %01dth experiment run for specification \"%s\" with input model \"%s\".\n", Integer.valueOf(runIdx), optSpecName, inputDesc.modelName);
      final String pathPrefix = ((((((((("gen/models/ttc/" + optSpecName) + "/") + batchStartTime) + "/") + inputDesc.modelName) + "-configuration-") + Integer.valueOf(batchId)) + "/") + Integer.valueOf(runIdx));
      final String serializedRulesPrefix = (pathPrefix + "/rules/");
      EObject _loadModel = this.modelLoader.loadModel((("src/uk/ac/kcl/mdeoptimise/ttc16/opt_specs/" + optSpecName) + 
        ".mopt"));
      final Optimisation model = ((Optimisation) _loadModel);
      OptimisationSpec _optimisation = model.getOptimisation();
      _optimisation.setAlgorithmEvolutions((inputDesc.generations * inputDesc.populationSize));
      OptimisationSpec _optimisation_1 = model.getOptimisation();
      _optimisation_1.setAlgorithmPopulation(inputDesc.populationSize);
      final CRAModelProvider modelProvider = RunOptimisation.injector.<CRAModelProvider>getInstance(CRAModelProvider.class);
      modelProvider.setInputModelName(inputDesc.modelName);
      final long startTime = System.nanoTime();
      final OptimisationInterpreter interpreter = new OptimisationInterpreter(model, modelProvider, serializedRulesPrefix);
      Iterator<EObject> _execute = interpreter.execute();
      final List<EObject> optimiserOutcome = IteratorExtensions.<EObject>toList(_execute);
      final Function1<EObject, EList<EObject>> _function = (EObject cm) -> {
        Object _feature = this.getFeature(cm, "classes");
        return ((EList<EObject>) _feature);
      };
      List<EList<EObject>> _map = ListExtensions.<EObject, EList<EObject>>map(optimiserOutcome, _function);
      Iterable<EObject> _flatten = Iterables.<EObject>concat(_map);
      final Procedure2<EObject, Integer> _function_1 = (EObject cl, Integer i) -> {
        this.setFeature(cl, "name", ("NewClass" + i));
      };
      IterableExtensions.<EObject>forEach(_flatten, _function_1);
      final long endTime = System.nanoTime();
      final long totalTime = (endTime - startTime);
      final Consumer<EObject> _function_2 = (EObject m) -> {
        modelProvider.storeModelAndInfo(m, (pathPrefix + "/final"));
      };
      optimiserOutcome.forEach(_function_2);
      final RunOptimisation.ResultRecord results = new RunOptimisation.ResultRecord();
      final MaximiseCRA craComputer = new MaximiseCRA();
      final MinimiseClasslessFeatures featureCounter = new MinimiseClasslessFeatures();
      results.timeTaken = (totalTime / 1000000);
      long _longValue = Double.valueOf(results.timeTaken).longValue();
      Date date = new Date(_longValue);
      SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
      TimeZone _timeZone = TimeZone.getTimeZone("UTC");
      formatter.setTimeZone(_timeZone);
      long _time = date.getTime();
      String result = formatter.format(Long.valueOf(_time));
      List<EObject> _list = IterableExtensions.<EObject>toList(optimiserOutcome);
      final Function1<EObject, Boolean> _function_3 = (EObject m) -> {
        double _computeFitness = featureCounter.computeFitness(m);
        return Boolean.valueOf((_computeFitness == 0));
      };
      Iterable<EObject> _filter = IterableExtensions.<EObject>filter(_list, _function_3);
      final Function1<EObject, Pair<EObject, Double>> _function_4 = (EObject m) -> {
        double _computeFitness = craComputer.computeFitness(m);
        double _multiply = (_computeFitness * (-1));
        return new Pair<EObject, Double>(m, Double.valueOf(_multiply));
      };
      Iterable<Pair<EObject, Double>> _map_1 = IterableExtensions.<EObject, Pair<EObject, Double>>map(_filter, _function_4);
      final Function1<Pair<EObject, Double>, Double> _function_5 = (Pair<EObject, Double> it) -> {
        Double _value = it.getValue();
        return Double.valueOf(DoubleExtensions.operator_minus(_value));
      };
      final List<Pair<EObject, Double>> sortedResults = IterableExtensions.<Pair<EObject, Double>, Double>sortBy(_map_1, _function_5);
      boolean _isEmpty = sortedResults.isEmpty();
      if (_isEmpty) {
        InputOutput.<String>println("No valid results for this run");
      } else {
        Pair<EObject, Double> _head = IterableExtensions.<Pair<EObject, Double>>head(sortedResults);
        int _hashCode = _head.hashCode();
        results.bestModelHashCode = _hashCode;
        Pair<EObject, Double> _head_1 = IterableExtensions.<Pair<EObject, Double>>head(sortedResults);
        Double _value = _head_1.getValue();
        results.maxCRA = (_value).doubleValue();
        results.hasUnassignedFeatures = false;
        Pair<EObject, Double> _head_2 = IterableExtensions.<Pair<EObject, Double>>head(sortedResults);
        EObject _key = _head_2.getKey();
        Resource _eResource = _key.eResource();
        URI _uRI = _eResource.getURI();
        String _string = _uRI.toString();
        results.bestModelPath = _string;
        final File fResults = new File((pathPrefix + "/final/results.txt"));
        final PrintWriter pw = new PrintWriter(fResults);
        pw.printf(
          "Experiment using spec \"%s\" and model \"%s\". Running for %01d generations with a population size of %01d.\n\n", optSpecName, inputDesc.modelName, Integer.valueOf(inputDesc.generations), Integer.valueOf(inputDesc.populationSize));
        pw.printf("Total time taken for this experiment: %02f milliseconds (%s).\n", Double.valueOf(results.timeTaken), result);
        final Consumer<Pair<EObject, Double>> _function_6 = (Pair<EObject, Double> p) -> {
          EObject _key_1 = p.getKey();
          int _hashCode_1 = _key_1.hashCode();
          Double _value_1 = p.getValue();
          pw.printf("Result model %08X at CRA %02f.\n", Integer.valueOf(_hashCode_1), _value_1);
        };
        sortedResults.forEach(_function_6);
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
