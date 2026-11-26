package smithyscala.codegen

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import scala.jdk.CollectionConverters.*

import software.amazon.smithy.build.FileManifest
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler
import software.amazon.smithy.model.shapes.ShapeId

/**
  * Command line: `Main <model file or directory> <service shape id> <scala package> <output dir>`.
  *
  * The package directory under the output directory is replaced, so files for shapes that left
  * the model don't linger.
  */
object Main {
  def main(args: Array[String]): Unit = args match {
    case Array(modelPath, service, pkg, outDir) =>
      val model = loadModel(Paths.get(modelPath))
      val files = ScalaClientGenerator.generate(model, ShapeId.from(service), pkg)
      val root  = Paths.get(outDir)
      deleteRecursively(root.resolve(pkg.replace('.', '/')))
      files.foreach { (rel, src) =>
        val path = root.resolve(rel)
        Files.createDirectories(path.getParent)
        Files.writeString(path, src): Unit
      }
      println(s"Generated ${files.size} files for $service into ${root.resolve(pkg.replace('.', '/'))}")
    case _ =>
      System.err.println("usage: Main <model> <service shape id> <scala package> <output dir>")
      sys.exit(2)
  }

  def loadModel(path: Path): Model =
    Model
      .assembler(getClass.getClassLoader)
      .discoverModels(getClass.getClassLoader)
      .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
      .addImport(path)
      .assemble()
      .unwrap()

  private def deleteRecursively(dir: Path): Unit =
    if (Files.exists(dir)) {
      val paths = Files.walk(dir)
      try paths.iterator().asScala.toList.reverse.foreach(Files.delete)
      finally paths.close()
    }
}

/**
  * The same generator as a smithy-build plugin, found through `ServiceLoader`:
  *
  * {{{
  * "plugins": {
  *   "scala-client-codegen": { "service": "com.amazonaws.sqs#AmazonSQS", "package": "example.sqs" }
  * }
  * }}}
  */
final class ScalaClientCodegenPlugin extends SmithyBuildPlugin {
  override def getName: String = "scala-client-codegen"

  override def execute(context: PluginContext): Unit = {
    val settings = context.getSettings
    val service  = ShapeId.from(settings.expectStringMember("service").getValue)
    val pkg      = settings.expectStringMember("package").getValue
    val manifest: FileManifest = context.getFileManifest
    ScalaClientGenerator.generate(context.getModel, service, pkg).foreach { (rel, src) =>
      manifest.writeFile(rel, src): Unit
    }
  }
}
