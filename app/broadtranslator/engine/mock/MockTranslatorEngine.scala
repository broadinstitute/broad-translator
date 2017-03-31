package broadtranslator.engine.mock

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.ModelId

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class MockTranslatorEngine extends TranslatorEngine {
  override def getAvailableModelIds: Seq[ModelId] = Seq("ModelOne", "ModelTwo", "ModelRed", "ModelTwo").map(ModelId)
}
