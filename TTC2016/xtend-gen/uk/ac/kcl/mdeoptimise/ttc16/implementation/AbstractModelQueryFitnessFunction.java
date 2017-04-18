package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import uk.ac.kcl.interpreter.IGuidanceFunction;

@SuppressWarnings("all")
public abstract class AbstractModelQueryFitnessFunction implements IGuidanceFunction {
  /**
   * Helper function getting the value of the named feature (if it exists) for the given EObject.
   */
  public Object getFeature(final EObject o, final String feature) {
    EClass _eClass = o.eClass();
    EStructuralFeature _eStructuralFeature = _eClass.getEStructuralFeature(feature);
    return o.eGet(_eStructuralFeature);
  }
  
  /**
   * Helper method returning true if the given EObject is an instance of the named EClass.
   */
  public boolean isOfClass(final EObject o, final String className) {
    EClass _eClass = o.eClass();
    String _name = _eClass.getName();
    return _name.equals(className);
  }
  
  /**
   * Helper method which returns the named encapsulated features for a class.
   */
  public Iterable<EObject> getClassFeatures(final EObject classObject, final String element) {
    Iterable<EObject> _xblockexpression = null;
    {
      Object _feature = this.getFeature(classObject, "encapsulates");
      final EList<EObject> features = ((EList<EObject>) _feature);
      final Function1<EObject, Boolean> _function = (EObject feature) -> {
        EClass _eClass = feature.eClass();
        String _name = _eClass.getName();
        return Boolean.valueOf(_name.equals(element));
      };
      _xblockexpression = IterableExtensions.<EObject>filter(features, _function);
    }
    return _xblockexpression;
  }
}
