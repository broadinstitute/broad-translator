package controllers

import javax.inject.{Inject, Singleton}

import broadtranslator.AppWiring
import broadtranslator.json.TranslatorJsonApi
import broadtranslator.json.smart.TranslatorSmartApi
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, Controller}

/**
  * broadtranslator
  * Created by oliverr on 3/30/2017.
  */
@Singleton
class TranslatorController @Inject() extends Controller {

  val jsonApi: TranslatorJsonApi = AppWiring.jsonApi
  val smartApi : TranslatorSmartApi = AppWiring.smartApi

  /**
    * Get the list of available models in JSON
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def getModelList: Action[AnyContent] = Action { implicit request =>
    Ok(jsonApi.getModelList)
  }

  def getModelSignature: Action[JsValue] = Action(parse.json) { implicit request =>
    Ok(jsonApi.getModelSignature(request.body))
  }

  def getVariablesByGroup: Action[JsValue] = Action(parse.json) { implicit request =>
    Ok(jsonApi.getVariablesByGroup(request.body))
  }

  def evaluate: Action[JsValue] = Action(parse.json) { implicit request =>
    Ok(jsonApi.evaluate(request.body))
  }


}
