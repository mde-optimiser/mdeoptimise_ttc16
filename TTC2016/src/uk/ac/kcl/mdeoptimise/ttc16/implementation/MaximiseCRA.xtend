package uk.ac.kcl.mdeoptimise.ttc16.implementation

import org.eclipse.emf.common.util.EList
import org.eclipse.emf.ecore.EObject

class MaximiseCRA extends AbstractModelQueryFitnessFunction {

	override double computeFitness(EObject model) {
		val cohesion = calculateCohesionRatio(model);
		val coupling = calculateCouplingRatio(model);

		println("Calculated CRA : " + (cohesion - coupling))

		return (cohesion - coupling)
	}

	def double calculateCohesionRatio(EObject model) {
		(model.getFeature("classes") as EList<EObject>).fold(0.0, [ acc, c |
			val methods = c.getClassFeatures("Method")
			val attributes = c.getClassFeatures("Attribute")

			acc + if (methods.empty) {
				0.0
			} else {
				if (methods.size == 1) {
					if (attributes.empty) {
						0.0
					} else {
						mai(c, c) / (methods.size * attributes.size)
					}
				} else {
					if (attributes.empty) {
						mmi(c, c) / (methods.size * (methods.size - 1))
					} else {
						val mai = mai(c, c)
						val mmi = mmi(c, c)
						val maCoupling = (methods.size * attributes.size)
						val mmCoupling = (methods.size * (methods.size - 1))

						mai / maCoupling + mmi / mmCoupling
					}
				}
			}
		])
	}

	def double calculateCouplingRatio(EObject model) {
		(model.getFeature("classes") as EList<EObject>).fold(0.0, [ acc, c |
			acc + calculateCouplingRatio(c, model)
		])
	}

	def double calculateCouplingRatio(EObject srcClass, EObject model) {
		val srcMethods = srcClass.getClassFeatures("Method")

		if (srcMethods.empty) {
			0.0
		} else {
			(model.getFeature("classes") as EList<EObject>).fold(0.0, [ acc, tgtClass |
				acc + if (srcClass == tgtClass) {
					0.0
				} else {
					val tgtMethods = tgtClass.getClassFeatures("Method")
					val tgtAttributes = tgtClass.getClassFeatures("Attribute")

					if (tgtMethods.size <= 1) {
						if (tgtAttributes.empty) {
							0.0
						} else {
							mai(srcClass, tgtClass) / (srcMethods.size * tgtAttributes.size)
						}
					} else {
						if (tgtAttributes.empty) {
							mmi(srcClass, tgtClass) / (srcMethods.size * (tgtMethods.size - 1))
						} else {
							// XTend slows down considerably if this formula is in line
							val mai = mai(srcClass, tgtClass)
							val mmi = mmi(srcClass, tgtClass)

							val maCoupling = (srcMethods.size * tgtAttributes.size)
							val mmCoupling = (srcMethods.size * (tgtMethods.size - 1))

							if (maCoupling > 0 && mmCoupling > 0) {
								mai / maCoupling + mmi / mmCoupling
							} else {
								0.0
							}
						}
					}
				}
			])
		}
	}

	/**
	 * Method--attribute dependencies between the two classes
	 */
	def double mai(EObject classSrc, EObject classTgt) {
		val tgtAttributes = classTgt.getClassFeatures("Attribute") as EList<EObject>
		val dependencies = classSrc.getClassFeatures("Method").map [ m |
			(m.getFeature("dataDependency") as EList<EObject>)
		].flatten.filter[a|tgtAttributes.contains(a)]

		dependencies.size
	}

	/**
	 * Method-method dependencies between the two classes
	 */
	def double mmi(EObject classSrc, EObject classTgt) {
		val tgtMethods = classTgt.getClassFeatures("Methods") as EList<EObject>
		val dependencies = classSrc.getClassFeatures("Method").map [ m |
			(m.getFeature("functionalDependency") as EList<EObject>)
		].flatten.filter[m|tgtMethods.contains(m)]

		dependencies.size
	}
}
