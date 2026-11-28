package smithy.scala.codegen

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import scala.jdk.CollectionConverters.*

import software.amazon.smithy.build.FileManifest
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildException
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.model.shapes.ShapeId

/**
  * Command line: `Main [--derivation=hearth|mirror] <model file or directory> <service shape id> <scala package>
  * <output dir>`. The derivation defaults to `hearth`.
  *
  * The package directory under the output directory is replaced, so files for shapes that left
  * the model don't linger.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val (options, positional) = args.partition(_.startsWith("--"))
    val derivation = options.collectFirst { case o if o.startsWith("--derivation=") => o.stripPrefix("--derivation=") }
      .fold(Derivation.Hearth)(Derivation.parse)
    run(positional, derivation)
  }

  private def run(args: Array[String], derivation: Derivation): Unit = args match {
    case Array(modelPath, service, pkg, outDir) =>
      // AWS models apply traits the generator has no definitions for, so unknown traits are allowed.
      val model = ModelLoader.load(Paths.get(modelPath), allowUnknownTraits = true)
      val files = ScalaClientGenerator.generate(model, ShapeId.from(service), pkg, derivation)
      val root  = Paths.get(outDir)
      deleteRecursively(root.resolve(pkg.replace('.', '/')))
      files.foreach { (rel, src) =>
        val path = root.resolve(rel)
        Files.createDirectories(path.getParent)
        Files.writeString(path, src): Unit
      }
      println(s"Generated ${files.size} files for $service into ${root.resolve(pkg.replace('.', '/'))}")
    case _ =>
      System.err.println("usage: Main [--derivation=hearth|mirror] <model> <service shape id> <scala package> <output dir>")
      sys.exit(2)
  }

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
  *   "scala-client-codegen": { "service": "com.amazonaws.sqs#AmazonSQS", "package": "example.sqs", "derivation": "mirror" }
  * }
  * }}}
  */
final class ScalaClientCodegenPlugin extends SmithyBuildPlugin {
  override def getName: String = "scala-client-codegen"

  override def execute(context: PluginContext): Unit = {
    val settings = context.getSettings
    val service  = ShapeId.from(settings.expectStringMember("service").getValue)
    val pkg      = settings.expectStringMember("package").getValue
    val derivation = settings.getStringMember("derivation").map(n => Derivation.parse(n.getValue)).orElse(Derivation.Hearth)
    val manifest: FileManifest = context.getFileManifest
    ScalaClientGenerator.generate(context.getModel, service, pkg, derivation).foreach { (rel, src) =>
      manifest.writeFile(rel, src): Unit
    }
  }
}

/**
  * Placeholder for a Scala server generator, registered as `scala-server-codegen` so smithy-build finds
  * it next to the client plugin. There is no server generator yet: running it fails with a clear error
  * instead of smithy-build reporting an unknown plugin.
  */
final class ScalaServerCodegenPlugin extends SmithyBuildPlugin {
  override def getName: String = "scala-server-codegen"

  override def execute(context: PluginContext): Unit =
    throw new SmithyBuildException(
      "scala-server-codegen: server generation is not implemented yet; use scala-client-codegen for clients"
    )
}
