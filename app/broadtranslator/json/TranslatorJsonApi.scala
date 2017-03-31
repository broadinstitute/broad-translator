package broadtranslator.json

import broadtranslator.engine.TranslatorEngine
import play.api.libs.json.Writes.StringWrites
import play.api.libs.json.{JsObject, Json}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class TranslatorJsonApi(engine: TranslatorEngine) {

  def getModelList: JsObject = {
    val modelIdStrings = engine.getAvailableModelIds.map(_.string)
    Json.obj("models" -> modelIdStrings)
  }

}
