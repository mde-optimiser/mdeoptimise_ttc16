package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IteratorExtensions;
import uk.ac.kcl.interpreter.IModelProvider;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.ModelLoadHelper;

@SuppressWarnings("all")
public class CRAModelProvider implements IModelProvider {
  @Inject
  private ModelLoadHelper modelLoader;
  
  private String inputModelName;
  
  private final ResourceSet resourceSet = new ResourceSetImpl();
  
  @Override
  public Iterator<EObject> initialModels(final EPackage metamodel) {
    Iterator<EObject> _xblockexpression = null;
    {
      this.modelLoader.registerPackage(metamodel);
      EObject _loadModel = this.modelLoader.loadModel((("src/uk/ac/kcl/mdeoptimise/ttc16/models/" + this.inputModelName) + ".xmi"));
      _xblockexpression = Collections.<EObject>unmodifiableList(CollectionLiterals.<EObject>newArrayList(_loadModel)).iterator();
    }
    return _xblockexpression;
  }
  
  public EObject loadModel(final String path) {
    try {
      EObject _xblockexpression = null;
      {
        URI _createURI = URI.createURI(path);
        final Resource resource = this.resourceSet.createResource(_createURI);
        resource.load(Collections.EMPTY_MAP);
        TreeIterator<EObject> _allContents = resource.getAllContents();
        _xblockexpression = IteratorExtensions.<EObject>head(_allContents);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void writeModel(final EObject model, final String path) {
    try {
      URI _createURI = URI.createURI(path);
      final Resource resource = this.resourceSet.createResource(_createURI);
      boolean _isLoaded = resource.isLoaded();
      if (_isLoaded) {
        EList<EObject> _contents = resource.getContents();
        _contents.clear();
      }
      EList<EObject> _contents_1 = resource.getContents();
      _contents_1.add(model);
      resource.save(Collections.EMPTY_MAP);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void storeModel(final EObject model, final String pathPrefix) {
    int _hashCode = model.hashCode();
    String _format = String.format("%08X", Integer.valueOf(_hashCode));
    String _plus = ((pathPrefix + "/") + _format);
    String _plus_1 = (_plus + ".xmi");
    this.writeModel(model, _plus_1);
  }
  
  public void storeModelAndInfo(final EObject model, final String pathPrefix) {
    try {
      this.storeModel(model, pathPrefix);
      int _hashCode = model.hashCode();
      String _format = String.format("%08X", Integer.valueOf(_hashCode));
      String _plus = ((pathPrefix + "/") + _format);
      String _plus_1 = (_plus + ".xmi");
      final String info = this.runEvaluationJarAgainstBestModel(_plus_1);
      int _hashCode_1 = model.hashCode();
      String _format_1 = String.format("%08X", Integer.valueOf(_hashCode_1));
      String _plus_2 = ((pathPrefix + "/") + _format_1);
      String _plus_3 = (_plus_2 + ".txt");
      File f = new File(_plus_3);
      final PrintWriter pw = new PrintWriter(f);
      pw.println(("Initial model: " + this.inputModelName));
      pw.println("Evaluation output: ");
      pw.println(info);
      pw.close();
      System.out.println(info);
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
  
  public void storeModels(final List<EObject> models, final String pathPrefix) {
    final Consumer<EObject> _function = (EObject m) -> {
      this.storeModel(m, pathPrefix);
    };
    models.forEach(_function);
  }
  
  public String setInputModelName(final String inputModelName) {
    return this.inputModelName = inputModelName;
  }
}
