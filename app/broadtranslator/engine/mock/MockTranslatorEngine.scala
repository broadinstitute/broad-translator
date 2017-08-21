package broadtranslator.engine.mock

import java.net.URI

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api._
import broadtranslator.engine.api.id._
import broadtranslator.engine.api.signature._
import broadtranslator.engine.api.evaluate._
import broadtranslator.engine.api.smart.SmartSpecs

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class MockTranslatorEngine extends TranslatorEngine {
  val applesNames = Seq("Gala", "Granny Smith", "Fuji", "Pink Lady")
  val applesList = ValueList.StringList(applesNames)
  val applesGroup = VariableGroupId("apples")
  val appleOneVar = VariableId("apple one")
  val appleTwoVar = VariableId("apple two")

  val orangesNames = Seq("Navel", "Clementine")
  val orangesList = ValueList.StringList(orangesNames)
  val orangesGroup = VariableGroupId("oranges")
  val bigOrangeVar = VariableId("big orange")
  val smallOrangeVar = VariableId("small orange")

  override def getAvailableModelIds: ModelListResult =
    ModelListResult(Seq("ModelOne", "ModelTwo", "ModelRed", "ModelBlue").map(ModelId))

  override def getSmartSpecs(modelId: ModelId): SmartSpecs =
    SmartSpecs(modelId.string, new URI("http://www.broadinstitute.org/translator"))

  override def getModelSignature(modelId: ModelId): ModelSignatureResult =
    ModelSignatureResult(modelId, Map(
      applesGroup -> GroupSignature(applesGroup, None, asInput = true, asOutput = false, Some(ProbabilityDistributionName.discrete), Some(StringType), Some(applesList), Seq(new ModelVariableSignature(appleOneVar), new ModelVariableSignature(appleTwoVar))),
      orangesGroup -> GroupSignature(orangesGroup, None, asInput = false, asOutput = true, Some(ProbabilityDistributionName.discrete), Some(StringType), Some(orangesList), Seq(new ModelVariableSignature(appleOneVar), new ModelVariableSignature(appleTwoVar)))
    ))

  override def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): GroupSignatureResult =
    GroupSignatureResult(modelId, groupId, Seq(new ModelVariableSignature(appleOneVar), new ModelVariableSignature(appleTwoVar)))

  override def evaluate(request: EvaluateModelRequest): EvaluateModelResult =
    EvaluateModelResult(Seq(
      VariableGroup(orangesGroup, Seq(
        ModelVariable(bigOrangeVar, ProbabilityDistribution.Discrete(Map(
          "Navel" -> 0.85, "Clementine" -> 0.15
        ))),
        ModelVariable(smallOrangeVar, ProbabilityDistribution.Discrete(Map(
          "Navel" -> 0.07, "Clementine" -> 0.93
        )))
      ))
    ))

}
