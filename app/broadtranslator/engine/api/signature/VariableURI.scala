package broadtranslator.engine.api.signature

object VariableURI {
  def apply(uri: Option[String]): Option[VariableURI] = uri.map(VariableURI(_))
}

case class VariableURI(uri: String) {
  
}