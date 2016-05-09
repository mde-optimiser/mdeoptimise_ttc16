package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import uk.ac.kcl.interpreter.ModelProvider;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.ModelLoadHelper;

@SuppressWarnings("all")
public class CRAModelProvider implements ModelProvider {
  @Inject
  private ModelLoadHelper modelLoader;
  
  private String inputModelName;
  
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
  
  public void storeModels(final Set<EObject> objects, final String path) {
    this.modelLoader.storeModels(objects, path);
  }
  
  public String setInputModelName(final String inputModelName) {
    return this.inputModelName = inputModelName;
  }
}
