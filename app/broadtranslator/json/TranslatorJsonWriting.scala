package broadtranslator.json

import broadtranslator.engine.api._
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString, JsArray, JsValue, Json, Writes}
import util.MatchNumber

/**
  * broadtranslator
  * Created by oliverr on 4/5/2017.
  */
object TranslatorJsonWriting {
  
  val EMPTY = Json.obj()
  
  /*
   * We encode None of an option as empty object and use this filter call to remove them from a sequence
   */
  private def filterOptions(fields: (String, Json.JsValueWrapper)*): Seq[(String, Json.JsValueWrapper)] = {
    fields.filter {case (key, value) => value != Json.toJsFieldJsValueWrapper(EMPTY) }
  }

  implicit val entityIdWrites: Writes[EntityId] = new Writes[EntityId] {
    override def writes(entityId: EntityId): JsString = JsString(entityId.string)
  }

  implicit val modelListResultWrites: Writes[ModelListResult] = new Writes[ModelListResult] {
    override def writes(result: ModelListResult): JsObject = Json.obj(
      "modelID" -> result.modelId
    )
  }

  implicit val valueTypeWrites: Writes[Option[ValueType]] = new Writes[Option[ValueType]] {
    override def writes(valueType: Option[ValueType]): JsValue = valueType match {
      case None => EMPTY
      case Some(valueType) => JsString(valueType.name)
    }
  }

  implicit val valueListWrites: Writes[Option[ValueList]] = new Writes[Option[ValueList]] {
    override def writes(valueList: Option[ValueList]): JsValue = valueList match {
      case None                => EMPTY
      case Some(ValueList.StringList(values)) => JsArray(values.map(JsString(_)))
      case Some(ValueList.NumberList(values)) => JsArray(values.map(JsNumber(_)))
      case Some(unmatched)             => println("value list not matched: "+unmatched); EMPTY
    }
  }
  

  implicit val variableGroupWrites: Writes[GroupSignature] = new Writes[GroupSignature] {
    override def writes(group: GroupSignature): JsObject = 
      Json.obj(filterOptions(
        "variableGroupID" -> group.groupId,
        "authorityURL" -> group.authorityURL,
        "asInput" -> group.asInput,
        "asOutput" -> group.asOutput,
        "groupType" -> group.valueType,
        "groupValue" -> group.groupValue
      ): _*)
  }

  implicit val modelSignatureResultWrites: Writes[ModelSignatureResult] = new Writes[ModelSignatureResult] {
    override def writes(result: ModelSignatureResult): JsObject = Json.obj(
      "modelID" -> result.modelId,
      "variableGroup" -> result.variableGroup.keys.map(_.string).toSeq.sorted.map(str => result.variableGroup(VariableGroupId(str)))
    )
  }

  implicit val variablesByGroupResultWrites: Writes[GroupSignatureResult] = new Writes[GroupSignatureResult] {
    override def writes(result: GroupSignatureResult): JsObject =
      Json.obj(
        "modelID" -> result.modelId,
        "variableGroupID" -> result.groupId,
        "modelVariable" -> result.modelVariable)
  }
  
  implicit val uriWrites: Writes[Option[VariableURI]] = new Writes[Option[VariableURI]] {
    override def writes(result: Option[VariableURI]): JsValue = result match {
      case None => EMPTY
      case Some(uri) => JsString(uri.uri)
    }
  }
  
  implicit val variableSignatureWrites: Writes[ModelVariableSignature] = new Writes[ModelVariableSignature] {
    override def writes(result: ModelVariableSignature): JsObject ={
      Json.obj(filterOptions(
        "variableID" -> result.variableId,
        "authorityURI" -> result.authorityURI,
        "variableType" -> result.variableType,
        "variableValue" -> result.variableValue
      ): _*)
  }
}
  implicit val variableWithProbabilitiesWrites: Writes[ModelVariable] =
    new Writes[ModelVariable] {
      override def writes(varWithProbs: ModelVariable): JsValue = {
        val varJson = Json.obj("variableID" -> varWithProbs.variableId)
        val probJson = varWithProbs.probabilityDistribution match {
          case ProbabilityDistribution.Discrete(probabilities) =>
            val probsJson = probabilities.map({ case (value, probability) =>
              val valueJson = value match {
                case string: String => JsString(string)
                case double: Double => JsNumber(double)
                case true => JsNumber(1)
                case false => JsNumber(0)
                case other => JsString(other.toString)
              }
              Json.obj(
                "variableValue" -> valueJson,
                "posteriorProbability" -> probability
              )
            })
            Json.obj("posteriorDistribution" -> probsJson)
          case ProbabilityDistribution.Gaussian(mean, sigma) => Json.obj(
            "mean" -> mean,
            "sigma" -> sigma
          )
        }
        varJson ++ probJson
      }
    }

  implicit val groupWithProbabilitiesWrites: Writes[VariableGroup] = new Writes[VariableGroup] {
    override def writes(groupWithProbs: VariableGroup): JsValue = Json.obj(
      "variableGroupID" -> groupWithProbs.groupId,
      "modelVariable" -> groupWithProbs.modelVariable
    )
  }

  implicit val evaluateResultWrites: Writes[EvaluateModelResult] = new Writes[EvaluateModelResult] {
    override def writes(result: EvaluateModelResult): JsValue = Json.obj(
      "posteriorProbabilities" -> result.posteriorProbabilities
    )
  }

}
