package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import uk.ac.kcl.interpreter.ModelProvider;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.ModelLoadHelper;

@SuppressWarnings("all")
public class CRAModelProvider implements ModelProvider {
  @Inject
  private ModelLoadHelper modelLoader;
  
  @Override
  public Iterator<EObject> initialModels(final EPackage metamodel) {
    Iterator<EObject> _xblockexpression = null;
    {
      this.modelLoader.registerPackage(metamodel);
      List<String> _modelPaths = this.getModelPaths();
      final Function1<String, EObject> _function = (String p) -> {
        return this.modelLoader.loadModel(p);
      };
      List<EObject> _map = ListExtensions.<String, EObject>map(_modelPaths, _function);
      _xblockexpression = _map.iterator();
    }
    return _xblockexpression;
  }
  
  public List<String> getModelPaths() {
    return Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("src/uk/ac/kcl/mdeoptimise/ttc16/models/TTC_InputRDG_A.xmi"));
  }
  
  public void storeModels(final Set<EObject> objects, final String path) {
    this.modelLoader.storeModels(objects, path);
  }
}
