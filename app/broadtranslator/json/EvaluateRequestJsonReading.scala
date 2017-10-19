package broadtranslator.json

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.evaluate._
import TranslatorIdsJsonReading.{ variableIdReads, groupIdReads, modelIdReads }
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

/**
 * broadtranslator
 * Created by oliverr on 4/4/2017.
 */
object EvaluateRequestJsonReading {

  implicit val variableValueReads: Reads[VariableValue] = new Reads[VariableValue] {
    override def reads(json: JsValue): JsResult[VariableValue] = json match {
      case JsBoolean(boolean) => JsSuccess(VariableValue.BooleanValue(boolean))
      case JsNumber(number)   => JsSuccess(VariableValue.NumberValue(number.toDouble))
      case JsString(string)   => JsSuccess(VariableValue.StringValue(string))
      case _                  => JsError(s"Expected Boolean, Number or String, but got $json.")
    }
  }

  implicit val valueProbabilityReads: Reads[ValueProbability] =
    ((JsPath \ "variableValue").read[VariableValue] and
      (JsPath \ "priorProbability").read[Double])(ValueProbability)
      
  implicit val scalarValueReads: Reads[ProbabilityDistribution.Scalar[_]] = 
    (JsPath \ "variableValue").read[VariableValue].map(ProbabilityDistribution.Scalar(_))

  implicit val discreteDistributionReads: Reads[ProbabilityDistribution.Discrete[_]] =
    JsPath.read[Seq[ValueProbability]].map(ProbabilityDistribution.Discrete(_))

  implicit val gaussianDistributionReads: Reads[ProbabilityDistribution.Gaussian] =
    ((JsPath \ "distributionMean").read[Double] and
      (JsPath \ "distributionStDev").read[Double])(ProbabilityDistribution.Gaussian)

  implicit val poissonDistributionReads: Reads[ProbabilityDistribution.Poisson] =
    (JsPath \ "lambdaParameter").read[Double].map(ProbabilityDistribution.Poisson)

  implicit val empiricalDistributionReads: Reads[ProbabilityDistribution.Empirical] =
    ((JsPath \ "distributionMean").read[Double] and
      (JsPath \ "distributionStDev").read[Double] and
      (JsPath \ "distributionPercentile").read[Seq[Double]])(ProbabilityDistribution.Empirical)

  implicit val probabilityDistributionReads: Reads[ProbabilityDistribution] =
    ((JsPath \ "scalarValue").readNullable[ProbabilityDistribution.Scalar[_]] and
      (JsPath \ "discreteDistribution").readNullable[ProbabilityDistribution.Discrete[_]] and
      (JsPath \ "GaussianDistribution").readNullable[ProbabilityDistribution.Gaussian] and
      (JsPath \ "PoissonDistribution").readNullable[ProbabilityDistribution.Poisson] and
      (JsPath \ "empiricalDistribution").readNullable[ProbabilityDistribution.Empirical])(ProbabilityDistribution(_,_, _, _, _))

  implicit val modelVariableReads: Reads[ModelVariable] =
    ((JsPath \ "variableID").read[VariableId] and
      (JsPath \ "priorDistribution").read[ProbabilityDistribution])(ModelVariable(_, _))

  implicit val variableGroupReads: Reads[VariableGroup] =
    ((JsPath \ "variableGroupID").read[VariableGroupId] and
      (JsPath \ "modelVariable").read[Seq[ModelVariable]])(VariableGroup)

  implicit val outputGroupReads: Reads[OutputGroup] =
    ((JsPath \ "variableGroupID").read[VariableGroupId] and
      (JsPath \ "variableID").read[Seq[VariableId]] and
      (JsPath \ "rawOutput").read[Boolean])(OutputGroup)

  implicit val evaluateRequestReads: Reads[EvaluateModelRequest] =
    ((JsPath \ "modelID").read[ModelId] and
      (JsPath \ "modelInput").read[Seq[VariableGroup]] and
      (JsPath \ "modelOutput").read[Seq[OutputGroup]])(EvaluateModelRequest)

}
