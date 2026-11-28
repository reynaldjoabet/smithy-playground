package smithy.scala.codegen

import java.nio.file.Path

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler

/**
  * Loads a Smithy model from a file or a directory, for the command-line tools (`Main`, `EventSchemas`).
  *
  * Trait definitions (AWS traits, waiters, protocols) and validators (such as the JSON-RPC ones in
  * [[jsonrpc]]) are discovered on the classpath, so models can use them without listing them. Loading
  * fails on any `ERROR` or `DANGER` event, which includes a model's own `metadata validators`.
  */
object ModelLoader {

  /**
    * @param allowUnknownTraits
    *   `true` loads a model that applies traits nobody defined, ignoring them. The client generator
    *   needs that for AWS models, which use traits it has no definitions for. Leave it `false` for your
    *   own models: then a misspelled trait (`@sensitve`) fails to load instead of being ignored.
    */
  def load(path: Path, allowUnknownTraits: Boolean = false): Model =
    Model
      .assembler(getClass.getClassLoader)
      .discoverModels(getClass.getClassLoader)
      .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, allowUnknownTraits)
      .addImport(path)
      .assemble()
      .unwrap()
}
