package uk.ac.kcl.mdeoptimise.ttc16.implementation

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

	def static void main(String[] args) {
		injector.getInstance(RunOptimisation).run()
	}

	@Inject
	private ModelLoadHelper modelLoader

	/**
	 * Run all experiments
	 */
	def run() {
		val optSpecs = #["ttc"]
		val inputModels = #["TTC_InputRDG_A", "TTC_InputRDG_B", "TTC_InputRDG_C", "TTC_InputRDG_D", "TTC_InputRDG_E"]
		
		optSpecs.forEach[optSpec |
			inputModels.forEach[input |
				runOneExperiment(optSpec, input)
			]
		]
	}

	/**
	 * Run a single experiment and record its outcomes
	 */
	def runOneExperiment(String optSpecName, String inputModelName) {
		println("Starting experiment run for specification \"" + optSpecName + "\" with input model \"" + inputModelName + "\"")
		
		val pathPrefix = "gen/models/ttc/" + optSpecName + "/" + inputModelName + "/" +
			new SimpleDateFormat("yyMMdd-HHmmss").format(new Date())

		val model = modelLoader.loadModel("src/uk/ac/kcl/mdeoptimise/ttc16/opt_specs/" + optSpecName +
			".mopt") as Optimisation

		val modelProvider = injector.getInstance(CRAModelProvider)
		modelProvider.setInputModelName (inputModelName)
		
		// Start measuring time
		val startTime = System.nanoTime
		
		val interpreter = new OptimisationInterpreter(model, new SimpleMO(50, 10), modelProvider)
		val optimiserOutcome = interpreter.execute()

		// Ensure all classes have unique names
		optimiserOutcome.map[cm|cm.getFeature("classes") as EList<EObject>].flatten.forEach [ cl, i |
			cl.setFeature("name", "NewClass" + i)
		]

		// End time measurement and print result
		val endTime = System.nanoTime
		val totalTime = endTime - startTime
		println("Total time taken for this experiment: " + totalTime/1000000 + " milliseconds")
		
		// Store result models
		modelProvider.storeModels(optimiserOutcome, pathPrefix + "/final")
	}

	def getFeature(EObject o, String feature) {
		o.eGet(o.eClass.getEStructuralFeature(feature))
	}

	def setFeature(EObject o, String feature, Object value) {
		o.eSet(o.eClass.getEStructuralFeature(feature), value)
	}
}
