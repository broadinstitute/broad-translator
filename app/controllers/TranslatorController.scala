package controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

/**
  * broadtranslator
  * Created by oliverr on 3/30/2017.
  */
@Singleton
class TranslatorController @Inject() extends Controller {

  /**
    * Get the list of available models in JSON
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def getModelList = Action { implicit request =>
    val json = Json.arr("variantsToPhenotypes", "geneExpressions", "coolModel")
    Ok(json)
  }


}
