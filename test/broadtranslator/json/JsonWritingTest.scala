package broadtranslator.json

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import play.api.libs.json._
import broadtranslator.engine.api.evaluate.EvaluateModelRequest
import broadtranslator.json.EvaluateRequestJsonReading.evaluateRequestReads
import broadtranslator.json.EvaluateRequestJsonWriting.evaluateRequestWrites

class JsonWritingTest extends FlatSpec with Matchers {

  "Evaluate request JSON writer" should
    "write mock request" in {

      val jsonStr = """
      {
  "modelID": "ModelThree",
  "modelOutput": [ 
    { "variableGroupID": "oranges",
     "variableID": ["Navel","Clementine", "Tangerine"]
    }
  ],
  "modelInput": [
    {"variableGroupID": "apples",
     "modelVariable": [
       { "variableID": "Gala",
         "priorDistribution": [
           { "variableValue": "red","priorProbability": 0.15},
           { "variableValue": "green","priorProbability": 0.45},
           { "variableValue": "yellow","priorProbability": 0.40}
         ]
       },
       { "variableID": "Granny Smith",
         "priorDistribution": [
           { "variableValue": "red","priorProbability": 0.15},
           { "variableValue": "green","priorProbability": 0.45},
           { "variableValue": "yellow","priorProbability": 0.40}
         ]
       },
       { "variableID": "Fuji",
         "priorDistribution": [
           { "variableValue": "red","priorProbability": 0.15},
           { "variableValue": "green","priorProbability": 0.45},
           { "variableValue": "yellow","priorProbability": 0.40}
         ]
       }
     ]
    }
  ]
}
      """
      val json = Json.parse(jsonStr)
      val obj = json.validate[EvaluateModelRequest]
      obj shouldBe a[JsSuccess[_]]
      info("OK")
      val request = obj.asInstanceOf[JsSuccess[EvaluateModelRequest]].value
      println(Json.prettyPrint(Json.toJson(request)))
      info("OK")
    }
}