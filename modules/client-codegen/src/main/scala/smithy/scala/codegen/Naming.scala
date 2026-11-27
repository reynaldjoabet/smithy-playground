package smithy.scala.codegen

/** Turning Smithy names into safe Scala identifiers and literals. */
object Naming {

  private val keywords = Set(
    "abstract", "case", "catch", "class", "def", "do", "else", "enum", "export", "extends", "false", "final",
    "finally", "for", "forSome", "given", "if", "implicit", "import", "lazy", "match", "new", "null", "object",
    "override", "package", "private", "protected", "return", "sealed", "super", "then", "this", "throw", "trait",
    "true", "try", "type", "val", "var", "while", "with", "yield", "as", "derives", "end", "extension", "infix",
    "inline", "opaque", "open", "transparent", "using", "_"
  )

  /** Members a case class (or an exception) already has, or its companion defines, so a field can't use the name. */
  private val caseClassMembers = Set(
    "copy", "equals", "hashCode", "toString", "productArity", "productElement", "productPrefix",
    "productIterator", "productElementName", "productElementNames", "canEqual", "getClass", "wait", "notify",
    "notifyAll", "clone", "finalize", "eq", "ne", "synchronized", "asInstanceOf", "isInstanceOf",
    "getMessage", "getCause", "getStackTrace", "fillInStackTrace", "initCause", "printStackTrace",
    "getLocalizedMessage", "addSuppressed", "getSuppressed", "setStackTrace", "smithyShapeId", "smithyFault",
    "smithyMetadata", "smithyRetryable", "smithyThrottling", "attach",
    // Members of every generated companion. A field may legally share a companion member's name, but Hearth's
    // case-class parsing then resolves the field to the companion's member (Cognito's CreateUserPoolRequest has a
    // `Schema` member), so these get the `_` suffix too.
    "schema", "codec"
  )

  def escape(name: String): String = if (keywords.contains(name)) s"`$name`" else name

  /** `QueueUrl` -> `queueUrl`, `MD5OfMessageBody` -> `md5OfMessageBody`, `ARN` -> `arn`. */
  def lowerCamel(name: String): String = {
    val run = name.takeWhile(c => c.isUpper || c.isDigit).length
    if (run == 0) name
    else if (run == name.length) name.toLowerCase
    else if (run == 1) name.substring(0, 1).toLowerCase + name.substring(1)
    else name.substring(0, run - 1).toLowerCase + name.substring(run - 1)
  }

  def upperCamel(name: String): String =
    if (name.isEmpty) name else name.substring(0, 1).toUpperCase + name.substring(1)

  /** A field name for a structure member. */
  def field(memberName: String): String = {
    val n = lowerCamel(memberName)
    escape(if (caseClassMembers.contains(n)) n + "_" else n)
  }

  /** A Scala string literal. */
  def lit(s: String): String = {
    val b = new StringBuilder("\"")
    s.foreach {
      case '"'                => b.append("\\\"")
      case '\\'               => b.append("\\\\")
      case '\n'               => b.append("\\n")
      case '\r'               => b.append("\\r")
      case '\t'               => b.append("\\t")
      case c if c < ' '       => b.append(f"\\u${c.toInt}%04x")
      case c                  => b.append(c)
    }
    b.append('"').toString
  }

  /** One line of documentation from a Smithy `@documentation` value (which is HTML). */
  def scaladoc(html: String, indent: String): String = {
    val text = html
      .replaceAll("<[^>]+>", " ")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&amp;", "&")
      .replaceAll("\\s+", " ")
      .replaceAll(" ([.,;:)])", "$1")
      .trim
    val sentence = {
      val end = text.indexOf(". ")
      if (end > 0) text.substring(0, end + 1) else text
    }
    val short = if (sentence.length > 300) sentence.take(297) + "..." else sentence
    if (short.isEmpty) "" else s"$indent/** ${short.replace("*/", "* /")} */\n"
  }
}
