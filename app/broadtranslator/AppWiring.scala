package broadtranslator

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.mock.MockTranslatorEngine
import broadtranslator.json.TranslatorJsonApi
import broadtranslator.json.smart.TranslatorSmartApi
import util.rdf.TranslatorRdfContext

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
object AppWiring {

  val engine: TranslatorEngine = new MockTranslatorEngine
  val jsonApi = new TranslatorJsonApi(engine)
  val smartApi = new TranslatorSmartApi(engine, TranslatorRdfContext.getModelIri)

}
