package uk.ac.kcl.mdeoptimise.ttc16.implementation;

import com.google.common.base.Objects;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import uk.ac.kcl.mdeoptimise.ttc16.implementation.AbstractModelQueryFitnessFunction;

@SuppressWarnings("all")
public class MinimiseClasslessFeatures extends AbstractModelQueryFitnessFunction {
  @Override
  public double computeFitness(final EObject model) {
    Object _feature = this.getFeature(model, "features");
    final Function1<EObject, Boolean> _function = (EObject feature) -> {
      Object _feature_1 = this.getFeature(feature, "isEncapsulatedBy");
      return Boolean.valueOf(Objects.equal(_feature_1, null));
    };
    Iterable<EObject> _filter = IterableExtensions.<EObject>filter(((EList<EObject>) _feature), _function);
    int _size = IterableExtensions.size(_filter);
    return ((-1.0) * _size);
  }
}
