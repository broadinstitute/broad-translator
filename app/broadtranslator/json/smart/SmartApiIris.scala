package broadtranslator.json.smart

import org.eclipse.rdf4j.model.IRI
import util.rdf.Rdf4jUtils

/**
  * broadtranslator
  * Created by oliverr on 4/14/2017.
  */
object SmartApiIris {

  val baseIriString = "http://www.broadinstitute.org/translator/"

  def newIRI(name: String): IRI = Rdf4jUtils.valueFactory.createIRI(baseIriString, name)

  val name: IRI = newIRI("name")
  val accessPoint: IRI = newIRI("accessPoint")

}
