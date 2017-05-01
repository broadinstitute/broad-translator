package broadtranslator.json

import broadtranslator.engine.api.VarValueSet.{NumberInterval, NumberList, StringList}
import broadtranslator.engine.api.{EntityId, EvaluateResult, GroupWithProbabilities, ModelListResult, ModelSignatureResult, ProbabilityDistribution, VarValueSet, VariableGroup, VariableWithProbabilities, VariablesByGroupResult, ValueType}
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString, JsValue, Json, Writes}
import util.MatchNumber

/**
  * broadtranslator
  * Created by oliverr on 4/5/2017.
  */
object TranslatorJsonWriting {

  implicit val entityIdWrites: Writes[EntityId] = new Writes[EntityId] {
    override def writes(entityId: EntityId): JsString = JsString(entityId.string)
  }

  implicit val modelListResultWrites: Writes[ModelListResult] = new Writes[ModelListResult] {
    override def writes(result: ModelListResult): JsObject = Json.obj(
      "models" -> result.modelIds
    )
  }

  implicit val valueTypeWrites: Writes[ValueType] = new Writes[ValueType] {
    override def writes(valueType: ValueType): JsString = JsString(valueType.name)
  }

  implicit val valueSetWrites: Writes[VarValueSet] = new Writes[VarValueSet] {
    override def writes(valueSet: VarValueSet): JsObject = {
      val jsonType = Json.obj("type" -> valueSet.valueType)
      val jsonValues = valueSet match {
        case StringList(values) => Json.obj("values" -> values)
        case NumberList(values) => Json.obj("values" -> values)
        case NumberInterval(min, max) => Json.obj("min" -> min, "max" -> max)
        case _ => Json.obj()
      }
      jsonType ++ jsonValues
    }
  }

  implicit val variableGroupWrites: Writes[VariableGroup] = new Writes[VariableGroup] {
    override def writes(group: VariableGroup): JsObject = {
      val jsonCore = JsObject(List(
        "id" -> JsString(group.id.string))++
        group.authorityURL.map(_.uri).map("authorityURL" -> JsString(_))++
        List("asConstraints" -> JsBoolean(group.asConstraints),
        "asOutputs" -> JsBoolean(group.asOutputs))
      )
      val jsonValues = valueSetWrites.writes(group.valueSet).asInstanceOf[JsObject]
      jsonCore ++ jsonValues
    }
  }

  implicit val modelSignatureResultWrites: Writes[ModelSignatureResult] = new Writes[ModelSignatureResult] {
    override def writes(result: ModelSignatureResult): JsObject = Json.obj(
      "model" -> result.modelId,
      "groups" -> result.groups.values
    )
  }

  implicit val variablesByGroupResultWrites: Writes[VariablesByGroupResult] = new Writes[VariablesByGroupResult] {
    override def writes(result: VariablesByGroupResult): JsObject =
      variableGroupWrites.writes(result.group).asInstanceOf[JsObject] ++ Json.obj("variables" -> result.variableIds)
  }

  implicit val variableWithProbabilitiesWrites: Writes[VariableWithProbabilities] =
    new Writes[VariableWithProbabilities] {
      override def writes(varWithProbs: VariableWithProbabilities): JsValue = {
        val varJson = Json.obj("variable" -> varWithProbs.variableId)
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
                "value" -> valueJson,
                "probability" -> probability
              )
            })
            Json.obj("probabilities" -> probsJson)
          case ProbabilityDistribution.Gaussian(mean, sigma) => Json.obj(
            "mean" -> mean,
            "sigma" -> sigma
          )
        }
        varJson ++ probJson
      }
    }

  implicit val groupWithProbabilitiesWrites: Writes[GroupWithProbabilities] = new Writes[GroupWithProbabilities] {
    override def writes(groupWithProbs: GroupWithProbabilities): JsValue = Json.obj(
      "group" -> groupWithProbs.groupId,
      "probabilities" -> groupWithProbs.varsWithProbs
    )
  }

  implicit val evaluateResultWrites: Writes[EvaluateResult] = new Writes[EvaluateResult] {
    override def writes(result: EvaluateResult): JsValue = Json.obj(
      "probabilities" -> result.groupsWithProbs
    )
  }

}
