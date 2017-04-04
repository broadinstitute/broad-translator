package broadtranslator.json

import broadtranslator.engine.api.{ModelId, ModelSignatureRequest}
import play.api.libs.json.{JsPath, Reads}

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
object TranslatorJsonReading {

  implicit val modelSignatureRequestReads: Reads[ModelSignatureRequest] =
    (JsPath \ "model").read[String].map(ModelId).map(ModelSignatureRequest)

}
