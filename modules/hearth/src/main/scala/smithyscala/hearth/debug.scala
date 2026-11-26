package smithyscala.hearth

/** `import smithyscala.hearth.debug.logDerivation` makes `HearthCodec.derived` print how it derived each codec. */
object debug {
  given logDerivation: HearthCodec.LogDerivation = HearthCodec.LogDerivation
}
