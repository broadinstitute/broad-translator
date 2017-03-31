package broadtranslator

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.mock.MockTranslatorEngine
import broadtranslator.json.TranslatorJsonApi

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
object AppWiring {

  val engine : TranslatorEngine = new MockTranslatorEngine
  val jsonApi = new TranslatorJsonApi(engine)

}
