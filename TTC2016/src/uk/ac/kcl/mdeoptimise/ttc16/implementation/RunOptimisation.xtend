package uk.ac.kcl.mdeoptimise.ttc16.implementation

import com.google.inject.Inject
import com.google.inject.Injector
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
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

	private static class InputModelDesc {
		public String modelName
		public int generations
		public int populationSize

		new(String modelName, int generations, int populationSize) {
			this.modelName = modelName
			this.generations = generations
			this.populationSize = populationSize
		}
	}

	/**
	 * Run all experiments
	 */
	def run() {
		val optSpecs = #["ttc"]
		val inputModels = #[new InputModelDesc("TTC_InputRDG_A", 100, 20),
			new InputModelDesc("TTC_InputRDG_B", 100, 20), new InputModelDesc("TTC_InputRDG_C", 1000, 50),
			new InputModelDesc("TTC_InputRDG_D", 1000, 50), new InputModelDesc("TTC_InputRDG_E", 1000, 50)]

		optSpecs.forEach [ optSpec |
			inputModels.forEach [ inputDesc |
				val lResults = new LinkedList<ResultRecord>()
				(0 ..< 10).forEach [ idx |
					lResults.add(runOneExperiment(optSpec, inputDesc, idx))
				]

				// Write averaged results for this specification and model
				val File f = new File(
					"gen/models/ttc/" + optSpec + "/" + inputDesc.modelName + "/overall_results" +
						new SimpleDateFormat("yyMMdd-HHmmss").format(new Date()) + ".txt")
				val PrintWriter pw = new PrintWriter(f)
				pw.println("Overall results for this experiment")
				pw.println("===================================")
				pw.println
				pw.printf("Experiment with spec \"%s\" and model \"%s\".\n", optSpec, inputDesc.modelName)
				pw.printf("Running for %01d generations with a population size of %01d.\n", inputDesc.generations, inputDesc.populationSize)
				pw.println
				pw.printf("Average time taken: %02f milliseconds.\n",
					lResults.fold(0.0, [acc, r|acc + r.timeTaken]) / lResults.size)
				val bestResult = lResults.maxBy[maxCRA]
				pw.printf("Best CRA was %02f for model with hash code %08X. This model was %s.\n", bestResult.maxCRA,
					bestResult.bestModelHashCode, (if (bestResult.hasUnassignedFeatures) {
						"invalid"
					} else {
						"valid"
					}))
				pw.close
			]
		]
	}

	/**
	 * Run a single experiment and record its outcomes
	 */
	def ResultRecord runOneExperiment(String optSpecName, InputModelDesc inputDesc, int runIdx) {
		System.out.printf("Starting %01dth experiment run for specification \"%s\" with input model \"%s\".\n", runIdx,
			optSpecName, inputDesc.modelName)

		val pathPrefix = "gen/models/ttc/" + optSpecName + "/" + inputDesc.modelName + "/" + runIdx + "/" +
			new SimpleDateFormat("yyMMdd-HHmmss").format(new Date())

		val model = modelLoader.loadModel("src/uk/ac/kcl/mdeoptimise/ttc16/opt_specs/" + optSpecName +
			".mopt") as Optimisation

		val modelProvider = injector.getInstance(CRAModelProvider)
		modelProvider.setInputModelName(inputDesc.modelName)

		// Start measuring time
		val startTime = System.nanoTime

		val interpreter = new OptimisationInterpreter(model,
			new SimpleMO(inputDesc.generations, inputDesc.populationSize), modelProvider)
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
			results.bestModelHashCode = sortedResults.head.key.hashCode
			results.maxCRA = sortedResults.head.value
			results.hasUnassignedFeatures = false

			val fResults = new File(pathPrefix + "/final/results.txt")
			val pw = new PrintWriter(fResults)
			System.out.printf("Total time taken for this experiment: %02f milliseconds.\n", results.timeTaken)
			pw.printf ("Experiment using spec \"%s\" and model \"%s\". Running for %01d generations with a population size of %01d.\n\n",
				optSpecName, inputDesc.modelName, inputDesc.generations, inputDesc.populationSize)
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
