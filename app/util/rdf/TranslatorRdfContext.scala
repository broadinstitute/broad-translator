package util.rdf

import java.net.URLEncoder

import broadtranslator.engine.api.id.ModelId
import org.eclipse.rdf4j.model.IRI

/**
  * broadtranslator
  * Created by oliverr on 4/14/2017.
  */
object TranslatorRdfContext {

  val projectBaseIriString = "http://www.broadinstitute.org/translator/"

  def getModelIri(modelId: ModelId): IRI = {
    Rdf4jUtils.valueFactory.createIRI(projectBaseIriString, URLEncoder.encode(modelId.string, "UTF-8"))
  }

}
