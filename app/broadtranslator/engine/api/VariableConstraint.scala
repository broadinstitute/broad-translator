package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
trait VariableConstraint {

}

object VariableConstraint {

  trait Typed[T] extends VariableConstraint

  case class Equals[T](value: T) extends Typed[T]

}