package uk.ac.kcl.mdeoptimise.ttc16.implementation

import org.eclipse.emf.common.util.EList
import org.eclipse.emf.ecore.EObject

class MinimiseEmptyClasses extends AbstractModelQueryFitnessFunction {

	override computeFitness(EObject model) {
		-1.0 * (model.getFeature("classes") as EList<EObject>).filter [class |
			(class.getFeature("encapsulates") as EList<EObject>).empty
		].size
	}
}
