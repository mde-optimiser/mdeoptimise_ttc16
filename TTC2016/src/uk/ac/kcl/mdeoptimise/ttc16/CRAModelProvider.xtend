package uk.ac.kcl.mdeoptimise.ttc16

import com.google.inject.Inject
import java.util.List
import org.eclipse.emf.ecore.EPackage
import uk.ac.kcl.interpreter.ModelProvider
import java.util.Set
import org.eclipse.emf.ecore.EObject

class CRAModelProvider implements ModelProvider {

	@Inject
	private ModelLoadHelper modelLoader

	override initialModels(EPackage metamodel) {
		modelLoader.registerPackage(metamodel)

		modelPaths.map [ p |
			modelLoader.loadModel(p)
		].iterator
	}

	def List<String> getModelPaths() {
		#["src/uk/ac/kcl/mdeoptimise/ttc16/models/TTC_InputRDG_A.xmi"]
	}
	
	def storeModels(Set<EObject> objects, String path) {
		modelLoader.storeModels (objects, path)
	}
	
}
