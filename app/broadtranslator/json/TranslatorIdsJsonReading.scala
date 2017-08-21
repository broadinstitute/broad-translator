package broadtranslator.json

import broadtranslator.engine.api.id._
import play.api.libs.json._

object TranslatorIdsJsonReading {

  implicit val modelIdReads: Reads[ModelId] = implicitly[Reads[String]].map(ModelId)
  implicit val variableIdReads: Reads[VariableId] = implicitly[Reads[String]].map(VariableId)
  implicit val groupIdReads: Reads[VariableGroupId] = implicitly[Reads[String]].map(VariableGroupId)
}