package util.rdf

import java.io.ByteArrayOutputStream

import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.rio.{RDFFormat, Rio}
import org.eclipse.rdf4j.sail.memory.MemoryStore

/**
  * broadtranslator
  * Created by oliverr on 4/14/2017.
  */
object Rdf4jUtils {

  val valueFactory: ValueFactory = SimpleValueFactory.getInstance()

  def getNewMemoryRepository: Repository = {
    val repository = new SailRepository(new MemoryStore())
    repository.initialize()
    repository
  }

  def getContentAsString(repository: Repository, format: RDFFormat = RDFFormat.JSONLD) : String = {
    val out = new ByteArrayOutputStream
    val writer = Rio.createWriter(format, out)
    repository.getConnection.export(writer)
    out.toString("UTF-8")
  }

}
