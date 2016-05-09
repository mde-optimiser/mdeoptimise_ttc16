package uk.ac.kcl.mdeoptimise.ttc16.implementation

import com.google.inject.Inject
import com.google.inject.Provider
import java.util.Collection
import java.util.Collections
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet

class ModelLoadHelper {
	@Inject
	private Provider<ResourceSet> resourceSetProvider

	private ResourceSet rs = null

	def getResourceSet() {
		if (rs == null) {
			rs = resourceSetProvider.get()
		}
		rs
	}

	def loadModel(String path) {
		val Resource resource = resourceSet.getResource(URI.createURI(path), true)

		resource.contents.head
	}

	def registerPackage(EPackage metamodel) {
		resourceSet.packageRegistry.put(metamodel.nsURI, metamodel)
	}

	def writeModel(EObject model, String path) {
		val resource = resourceSet.createResource(URI.createURI(path))
		if (resource.loaded) {
			resource.contents.clear
		}
		resource.contents.add(model)
		resource.save(Collections.EMPTY_MAP)
	}

	def storeModel(EObject model, String pathPrefix) {
		model.writeModel(
			pathPrefix + "/" + String.format("%08X", model.hashCode) + ".xmi"
		)
	}

	def storeModels(Collection<EObject> models, String pathPrefix) {
		models.forEach[m|m.storeModel(pathPrefix)]
	}
}