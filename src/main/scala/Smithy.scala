package example

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import java.nio.file.Files
import java.nio.file.Paths
import scala.io.Source

object Smithy extends App {
  println("Smithy is running!")

  // Load all AWS service models from src/main/resources/models/
  val mapper = new ObjectMapper()
  val modelsPath = Paths.get("src/main/resources/models")

  if (Files.exists(modelsPath)) {
    val serviceModels = loadServiceModels(modelsPath)
    println(s"Loaded ${serviceModels.size} service models")
    serviceModels.foreach { case (serviceName, model) =>
      println(s"  - $serviceName")
    }
  } else {
    println(s"Models directory not found at $modelsPath")
  }

  /** Load all JSON models from the models directory. Each service folder
    * contains a JSON file (e.g., s3/s3.json).
    */
  def loadServiceModels(
      modelsPath: java.nio.file.Path
  ): Map[String, JsonNode] = {
    val serviceDir = modelsPath.toFile

    if (!serviceDir.isDirectory) {
      return Map()
    }

    serviceDir
      .listFiles()
      .filter(_.isDirectory)
      .flatMap { dir =>
        val serviceName = dir.getName
        val jsonFile = new java.io.File(dir, s"$serviceName.json")

        if (jsonFile.exists()) {
          try {
            val content = Source.fromFile(jsonFile).mkString
            val model = mapper.readTree(content)
            Some((serviceName, model))
          } catch {
            case e: Exception =>
              println(
                s"Error loading model for $serviceName: ${e.getMessage}"
              )
              None
          }
        } else {
          None
        }
      }
      .toMap
  }
}
