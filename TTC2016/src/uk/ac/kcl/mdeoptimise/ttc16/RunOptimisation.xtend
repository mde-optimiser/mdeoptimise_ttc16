package uk.ac.kcl.mdeoptimise.ttc16

import com.google.inject.Inject
import com.google.inject.Injector
import java.text.SimpleDateFormat
import java.util.Date
import org.eclipse.emf.common.util.EList
import org.eclipse.emf.ecore.EObject
import uk.ac.kcl.MDEOptimiseStandaloneSetup
import uk.ac.kcl.interpreter.OptimisationInterpreter
import uk.ac.kcl.interpreter.algorithms.SimpleMO
import uk.ac.kcl.mDEOptimise.Optimisation

class RunOptimisation {
	
	static val Injector injector = new MDEOptimiseStandaloneSetup().createInjectorAndDoEMFRegistration()
	
	@Inject
	private ModelLoadHelper modelLoader
	
	def run() {
		val pathPrefix = "gen/models/ttc/" + new SimpleDateFormat("yyMMdd-HHmmss").format(new Date())
		
		val model = modelLoader.loadModel("src/uk/ac/kcl/mdeoptimise/ttc16/ttc.mopt") as Optimisation

		val modelProvider = injector.getInstance(CRAModelProvider)
		val interpreter = new OptimisationInterpreter(model, new SimpleMO(50, 10), modelProvider)
		val optimiserOutcome = interpreter.execute()

		// Ensure all classes have unique names
		optimiserOutcome.map[cm | cm.getFeature("classes") as EList<EObject>].flatten.forEach[cl, i |
			cl.setFeature ("name", "NewClass" + i)
		]

		modelProvider.storeModels (optimiserOutcome, pathPrefix + "/final")
	}
	
	def getFeature(EObject o, String feature) {
		o.eGet (o.eClass.getEStructuralFeature(feature))
	}

	def setFeature(EObject o, String feature, Object value) {
		o.eSet (o.eClass.getEStructuralFeature(feature), value)
	}
	
	def static void main(String[] args) {
		injector.getInstance(RunOptimisation).run()
	}
}