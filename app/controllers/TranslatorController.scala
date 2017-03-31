package controllers

import javax.inject.{Inject, Singleton}

import broadtranslator.AppWiring
import play.api.mvc.{Action, Controller}

/**
  * broadtranslator
  * Created by oliverr on 3/30/2017.
  */
@Singleton
class TranslatorController @Inject() extends Controller {

  val jsonApi = AppWiring.jsonApi

  /**
    * Get the list of available models in JSON
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def getModelList = Action { implicit request =>
    Ok(jsonApi.getModelList)
  }


}
