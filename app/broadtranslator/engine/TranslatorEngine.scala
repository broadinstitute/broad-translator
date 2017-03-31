package broadtranslator.engine

import broadtranslator.engine.api.ModelId

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
trait TranslatorEngine {

  def getAvailableModelIds: Seq[ModelId]

}
