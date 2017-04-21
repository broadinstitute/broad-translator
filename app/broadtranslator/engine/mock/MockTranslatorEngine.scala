package broadtranslator.engine.mock

import java.net.URI
import java.io.File
import java.io.PrintWriter
import java.io.FileWriter

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api._
import broadtranslator.engine.api.smart.SmartSpecs

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class MockTranslatorEngine extends TranslatorEngine {
  val applesNames = Seq("Gala", "Granny Smith", "Fuji", "Pink Lady")
  val applesList = VarValueSet.StringList(applesNames)
  val applesGroup = VariableGroupId("apples")
  val appleOneVar = VariableId("apple one")
  val appleTwoVar = VariableId("apple two")

  val orangesNames = Seq("Navel", "Clementine")
  val orangesList = VarValueSet.StringList(orangesNames)
  val orangesGroup = VariableGroupId("oranges")
  val bigOrangeVar = VariableId("big orange")
  val smallOrangeVar = VariableId("small orange")

  override def getAvailableModelIds: ModelListResult =
    ModelListResult(Seq("ModelOne", "ModelTwo", "ModelRed", "ModelBlue").map(ModelId))

  override def getSmartSpecs(modelId: ModelId): SmartSpecs =
    SmartSpecs(modelId.string, new URI("http://www.broadinstitute.org/translator"))

  override def getModelSignature(modelId: ModelId): ModelSignatureResult =
    ModelSignatureResult(modelId, Map(
      applesGroup -> VariableGroup(modelId, applesGroup, asConstraints = true, asOutputs = false, applesList),
      orangesGroup -> VariableGroup(modelId, orangesGroup, asConstraints = false, asOutputs = true, orangesList)
    ))

  override def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): VariablesByGroupResult =
    VariablesByGroupResult(VariableGroup(modelId, groupId, asConstraints = true, asOutputs = false, applesList),
      Seq(appleOneVar, appleTwoVar))

  override def evaluate(request: EvaluateRequest): EvaluateResult ={
    exportRequest(request, new File("/tmp/translatorRequest.txt"))
    EvaluateResult(Seq(
      GroupWithProbabilities(orangesGroup, Seq(
        VariableWithProbabilities(bigOrangeVar, ProbabilityDistribution.Discrete(Map(
          1 -> 0.85, 2 -> 0.15
        ))),
        VariableWithProbabilities(smallOrangeVar, ProbabilityDistribution.Discrete(Map(
          1 -> 0.07, 2 -> 0.93
        )))
      ))
    ))
  }
  
  private def exportRequest(request: EvaluateRequest, file: File): Unit = {
    val out = new PrintWriter(new FileWriter(file))
    out.println("io\tvariableGroup\tvariableName\tvariableValue\tprobability")
    for (
      group <- request.constraints;
      variableGroup = group.groupId.string;
      variable <- group.variablesAndConstraints;
      variableName = variable.variableId.string;
      value = variable.constraint match {
        case VariableConstraint.Equals(value) => value
      };
      probability = 0.18
    ) {
      out.println("input\t" + variableGroup + "\t" + variableName + "\t" + value + "\t" + probability)
    }
    for (
      group <- request.outputs;
      variableGroup = group.groupId.string;
      variable <- group.variableIds;
      variableName = variable.string
    ) {
      out.println("output\t" + variableGroup + "\t" + variableName + "\t\t")
    }
    out.close()
  }
}
