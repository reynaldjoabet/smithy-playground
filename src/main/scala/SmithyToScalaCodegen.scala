import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.io.Source
import scala.util.Using

/** Generates Scala 3 case classes from Smithy model.json
  *
  * Usage: scala SmithyToScalaCodegen.scala <path-to-model.json> <output-dir>
  * scala SmithyToScalaCodegen.scala target/smithy/output/model.json
  * src/main/scala/generated
  */

case class Shape(
    id: String,
    `type`: String,
    members: Map[String, Member] = Map.empty,
    values: Map[String, EnumValue] = Map.empty,
    member: Option[String] = None
)

case class Member(
    target: String,
    required: Boolean = false
)

case class EnumValue(
    target: String
)

case class SmithyModel(shapes: Map[String, Shape] = Map.empty)

object SmithyToScalaCodegen {

  def readModelJson(path: String): SmithyModel = {
    Using(Source.fromFile(path)) { source =>
      val json = source.mkString
      parseJson(json)
    }.get
  }

  private def parseJson(json: String): SmithyModel = {
    try {
      import com.fasterxml.jackson.databind.{ObjectMapper, JsonNode}
      import scala.jdk.CollectionConverters.*

      val mapper = new ObjectMapper()
      val root: JsonNode = mapper.readTree(json)
      val shapesNode: JsonNode = root.get("shapes")

      val shapes: Map[String, Shape] =
        if (shapesNode != null && !shapesNode.isNull) {
          shapesNode
            .fields()
            .asScala
            .foldLeft(Map.empty[String, Shape]) { (acc, entry) =>
              val id = entry.getKey()
              val shapeNode = entry.getValue()
              val shapeType: String = shapeNode.get("type").asText()

              val members: Map[String, Member] = if (shapeNode.has("members")) {
                shapeNode
                  .get("members")
                  .fields()
                  .asScala
                  .foldLeft(Map.empty[String, Member]) {
                    (memberAcc, memberEntry) =>
                      val name = memberEntry.getKey()
                      val memberNode = memberEntry.getValue()
                      val target: String = memberNode.get("target").asText()
                      val required: Boolean = memberNode.has(
                        "required"
                      ) && memberNode.get("required").asBoolean()
                      memberAcc + (name -> Member(target, required))
                  }
              } else {
                Map.empty[String, Member]
              }

              val values: Map[String, EnumValue] =
                if (shapeNode.has("values")) {
                  shapeNode
                    .get("values")
                    .fields()
                    .asScala
                    .foldLeft(Map.empty[String, EnumValue]) {
                      (valueAcc, valueEntry) =>
                        val name = valueEntry.getKey()
                        val valueNode = valueEntry.getValue()
                        val target: String = valueNode.get("target").asText()
                        valueAcc + (name -> EnumValue(target))
                    }
                } else {
                  Map.empty[String, EnumValue]
                }

              val member: Option[String] = if (shapeNode.has("member")) {
                Option(shapeNode.get("member").get("target").asText())
              } else {
                None
              }

              acc + (id -> Shape(id, shapeType, members, values, member))
            }
        } else {
          Map.empty[String, Shape]
        }

      SmithyModel(shapes)
    } catch {
      case e: Exception =>
        println(s"Error parsing JSON: ${e.getMessage}")
        SmithyModel(Map.empty)
    }
  }

  def generateScalaClass(shape: Shape, smithyModel: SmithyModel): String = {
    shape.`type` match {
      case "structure" => generateStructure(shape, smithyModel)
      case "enum"      => generateEnum(shape)
      case "list"      => generateList(shape, smithyModel)
      case "map"       => generateMap(shape, smithyModel)
      case "string" | "integer" | "long" | "boolean" | "double" => ""
      case _ => s"// Unsupported shape type: ${shape.`type`}"
    }
  }

  private def generateStructure(shape: Shape, model: SmithyModel): String = {
    val className = extractClassName(shape.id)
    val fields = shape.members
      .map { case (name, member) =>
        val scalaType = resolveType(member.target, model)
        val optional = if (member.required) "" else "Option["
        val optionalClose = if (member.required) "" else "]"
        s"  $name: $optional$scalaType$optionalClose"
      }
      .mkString(",\n")

    val fieldsList = if (fields.isEmpty) "" else "\n" + fields + "\n"
    s"""case class $className($fieldsList) {
       |  override def toString: String = s"$className(...)"
       |}
       |""".stripMargin
  }

  private def generateEnum(shape: Shape): String = {
    val className = extractClassName(shape.id)
    val values = shape.values.keys
      .map { v =>
        s"  case ${v.toUpperCase}"
      }
      .mkString("\n")
    s"""enum $className {
       |$values
       |}
       |""".stripMargin
  }

  private def generateList(shape: Shape, model: SmithyModel): String = {
    shape.member match {
      case Some(target) =>
        val elementType = resolveType(target, model)
        s"type ${extractClassName(shape.id)} = Seq[$elementType]"
      case None => ""
    }
  }

  private def generateMap(shape: Shape, model: SmithyModel): String = {
    s"type ${extractClassName(shape.id)} = Map[String, Any]"
  }

  private def resolveType(target: String, model: SmithyModel): String = {
    target match {
      case "smithy.api#String"    => "String"
      case "smithy.api#Integer"   => "Int"
      case "smithy.api#Long"      => "Long"
      case "smithy.api#Boolean"   => "Boolean"
      case "smithy.api#Double"    => "Double"
      case "smithy.api#Timestamp" => "java.time.Instant"
      case s if s.contains("#")   => extractClassName(s)
      case _                      => "Any"
    }
  }

  private def extractClassName(shapeId: String): String = {
    shapeId.split("#").last.split("\\$").last
  }

  def writeFile(path: String, content: String): Unit = {
    Files.createDirectories(Paths.get(path).getParent)
    Files.write(Paths.get(path), content.getBytes)
  }

  @main def run(modelPath: String, outputDir: String): Unit = {
    println(s"Reading Smithy model from: $modelPath")
    val model = readModelJson(modelPath)

    println(s"Found ${model.shapes.size} shapes")

    val generated = model.shapes
      .filter(!_._1.startsWith("smithy.api#"))
      .map { case (id, shape) =>
        println(s"  Generating: ${extractClassName(id)}")
        generateScalaClass(shape, model)
      }
      .filter(_.nonEmpty)
      .mkString("\n\n")

    val outputFile = s"$outputDir/SmithyModels.scala"
    writeFile(
      outputFile,
      s"""package unison.generated

object SmithyModels {
  $generated
}
"""
    )

    println(s"✓ Generated ${model.shapes.size} classes")
    println(s"✓ Written to: $outputFile")
  }

}
