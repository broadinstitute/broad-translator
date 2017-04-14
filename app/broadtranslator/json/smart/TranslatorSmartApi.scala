package broadtranslator.json.smart

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.ModelId
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.vocabulary.XMLSchema
import org.eclipse.rdf4j.repository.Repository
import util.rdf.Rdf4jUtils

/**
  * broadtranslator
  * Created by oliverr on 4/14/2017.
  */
class TranslatorSmartApi(engine: TranslatorEngine, modelIriMaker: ModelId => IRI) {

  def getSmartApi(modelId: ModelId): Repository = {
    val apiSpecs = engine.getSmartSpecs(modelId)
    val repository = Rdf4jUtils.getNewMemoryRepository
    val conn = repository.getConnection
    val modelIri = modelIriMaker(modelId)
    conn.add(modelIri, SmartApiIris.name, Rdf4jUtils.valueFactory.createLiteral(apiSpecs.name, XMLSchema.STRING))
    conn.add(modelIri, SmartApiIris.accessPoint,
      Rdf4jUtils.valueFactory.createLiteral(apiSpecs.accessPoint.toString, XMLSchema.ANYURI))
    repository
  }

}
