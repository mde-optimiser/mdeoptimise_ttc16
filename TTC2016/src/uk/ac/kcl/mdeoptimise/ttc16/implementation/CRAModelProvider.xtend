package uk.ac.kcl.mdeoptimise.ttc16.implementation

import com.google.inject.Inject
import java.util.Set
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import uk.ac.kcl.interpreter.ModelProvider

class CRAModelProvider implements ModelProvider {

	@Inject
	private ModelLoadHelper modelLoader

	private String inputModelName
	
	override initialModels(EPackage metamodel) {
		modelLoader.registerPackage(metamodel)

		#[modelLoader.loadModel("src/uk/ac/kcl/mdeoptimise/ttc16/models/" + inputModelName + ".xmi")].iterator
	}
	
	def storeModels(Set<EObject> objects, String path) {
		modelLoader.storeModels (objects, path)
	}
	
	def setInputModelName(String inputModelName) {
		this.inputModelName = inputModelName
	}
}