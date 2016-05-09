package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class ModelLoadHelper {
  @Inject
  private Provider<ResourceSet> resourceSetProvider;
  
  private ResourceSet rs = null;
  
  public ResourceSet getResourceSet() {
    ResourceSet _xblockexpression = null;
    {
      boolean _equals = Objects.equal(this.rs, null);
      if (_equals) {
        ResourceSet _get = this.resourceSetProvider.get();
        this.rs = _get;
      }
      _xblockexpression = this.rs;
    }
    return _xblockexpression;
  }
  
  public EObject loadModel(final String path) {
    EObject _xblockexpression = null;
    {
      ResourceSet _resourceSet = this.getResourceSet();
      URI _createURI = URI.createURI(path);
      final Resource resource = _resourceSet.getResource(_createURI, true);
      EList<EObject> _contents = resource.getContents();
      _xblockexpression = IterableExtensions.<EObject>head(_contents);
    }
    return _xblockexpression;
  }
  
  public Object registerPackage(final EPackage metamodel) {
    ResourceSet _resourceSet = this.getResourceSet();
    EPackage.Registry _packageRegistry = _resourceSet.getPackageRegistry();
    String _nsURI = metamodel.getNsURI();
    return _packageRegistry.put(_nsURI, metamodel);
  }
  
  public void writeModel(final EObject model, final String path) {
    try {
      ResourceSet _resourceSet = this.getResourceSet();
      URI _createURI = URI.createURI(path);
      final Resource resource = _resourceSet.createResource(_createURI);
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
  
  public void storeModels(final Collection<EObject> models, final String pathPrefix) {
    final Consumer<EObject> _function = (EObject m) -> {
      this.storeModel(m, pathPrefix);
    };
    models.forEach(_function);
  }
}
