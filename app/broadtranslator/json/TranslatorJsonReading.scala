package broadtranslator.json

import broadtranslator.engine.api.{EvaluateRequest, ModelId, ModelSignatureRequest, VariableGroupId, VariablesByGroupRequest}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Reads}

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
object TranslatorJsonReading {

  implicit val modelSignatureRequestReads: Reads[ModelSignatureRequest] =
    (JsPath \ "model").read[String].map(ModelId).map(ModelSignatureRequest)

  implicit val variablesByGroupRequestReads: Reads[VariablesByGroupRequest] =
    ((JsPath \ "model").read[String].map(ModelId) and
      (JsPath \ "group").read[String].map(VariableGroupId)) (VariablesByGroupRequest)

  implicit val evaluateRequestReads: Reads[EvaluateRequest] = ???

}
