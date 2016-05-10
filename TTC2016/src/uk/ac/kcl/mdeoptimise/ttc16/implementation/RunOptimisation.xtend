package uk.ac.kcl.mdeoptimise.ttc16.implementation

import com.google.inject.Inject
import com.google.inject.Injector
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.LinkedList
import java.util.List
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

	private static class ResultRecord {
		public double timeTaken
		public double maxCRA
		public long bestModelHashCode
		public boolean hasUnassignedFeatures
	}

	/**
	 * Run all experiments
	 */
	def run() {
		val optSpecs = #["ttc"]
		val inputModels = #["TTC_InputRDG_A", "TTC_InputRDG_B", "TTC_InputRDG_C", "TTC_InputRDG_D", "TTC_InputRDG_E"]

		// pick up results from the experiments
		val resultCollector = new HashMap<Pair<String, String>, List<ResultRecord>>

		optSpecs.forEach [ optSpec |
			inputModels.forEach [ input |
				val lResults = new LinkedList<ResultRecord>()
				(0 ..< 10).forEach [ idx |
					lResults.add(runOneExperiment(optSpec, input, idx))
				]
				resultCollector.put(new Pair<String, String>(optSpec, input), lResults)
			]
		]

		// Write averaged results
		resultCollector.keySet.forEach [ experiment |
			val lResults = resultCollector.get(experiment)
			val File f = new File(
				"gen/models/ttc/" + experiment.key + "/" + experiment.value + "/overall_results" +
					new SimpleDateFormat("yyMMdd-HHmmss").format(new Date()) + ".txt")
			val PrintWriter pw = new PrintWriter(f)
			pw.println("Overall results for this experiment")
			pw.println("===================================")
			pw.println
			pw.printf("Average time taken: %02f milliseconds.\n", lResults.fold(0.0, [acc, r|acc + r.timeTaken])/lResults.size)
			val bestResult = lResults.maxBy[maxCRA]
			pw.printf("Best CRA was %02f for model with hash code %08X. This model was %s.\n", bestResult.maxCRA,
				bestResult.bestModelHashCode, (if (bestResult.hasUnassignedFeatures) {
					"invalid"
				} else {
					"valid"
				}))
			pw.close
		]
	}

	/**
	 * Run a single experiment and record its outcomes
	 */
	def ResultRecord runOneExperiment(String optSpecName, String inputModelName, int runIdx) {
		System.out.printf("Starting %01dth experiment run for specification \"%s\" with input model \"%s\".\n", runIdx,
			optSpecName, inputModelName)

		val pathPrefix = "gen/models/ttc/" + optSpecName + "/" + inputModelName + "/" + runIdx + "/" +
			new SimpleDateFormat("yyMMdd-HHmmss").format(new Date())

		val model = modelLoader.loadModel("src/uk/ac/kcl/mdeoptimise/ttc16/opt_specs/" + optSpecName +
			".mopt") as Optimisation

		val modelProvider = injector.getInstance(CRAModelProvider)
		modelProvider.setInputModelName(inputModelName)

		// Start measuring time
		val startTime = System.nanoTime

		val interpreter = new OptimisationInterpreter(model, new SimpleMO(50, 10), modelProvider)
		val optimiserOutcome = interpreter.execute()

		// Ensure all classes have unique names
		optimiserOutcome.map[cm|cm.getFeature("classes") as EList<EObject>].flatten.forEach [ cl, i |
			cl.setFeature("name", "NewClass" + i)
		]

		// End time measurement
		val endTime = System.nanoTime
		val totalTime = endTime - startTime

		// Store result models
		modelProvider.storeModels(optimiserOutcome, pathPrefix + "/final")

		// Output results
		val results = new ResultRecord
		val craComputer = new MaximiseCRA
		val featureCounter = new MinimiseClasslessFeatures

		results.timeTaken = totalTime / 1000000
		val sortedResults = optimiserOutcome.filter [ m |
			featureCounter.computeFitness(m) == 0
		].map [ m |
			new Pair<EObject, Double>(m, craComputer.computeFitness(m))
		].sortBy[-value]
		if (sortedResults.empty) {
			println("No valid results for this run")
		} else {
			val bestModel = sortedResults.head.key
			results.bestModelHashCode = bestModel.hashCode
			results.maxCRA = sortedResults.head.value
			results.hasUnassignedFeatures = (featureCounter.computeFitness(bestModel) != 0)

			val fResults = new File(pathPrefix + "/final/results.txt")
			val pw = new PrintWriter(fResults)
			System.out.printf("Total time taken for this experiment: %02f milliseconds.\n", results.timeTaken)
			pw.printf("Total time taken for this experiment: %02f milliseconds.\n", results.timeTaken)
			sortedResults.forEach [ p |
				System.out.printf("Result model %08X at CRA %02f.\n", p.key.hashCode, p.value)
				pw.printf("Result model %08X at CRA %02f.\n", p.key.hashCode, p.value)
			]
			pw.close

		}

		return results
	}

	def getFeature(EObject o, String feature) {
		o.eGet(o.eClass.getEStructuralFeature(feature))
	}

	def setFeature(EObject o, String feature, Object value) {
		o.eSet(o.eClass.getEStructuralFeature(feature), value)
	}
}
