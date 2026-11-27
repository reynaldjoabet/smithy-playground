package smithy.scala.bench

import java.time.Instant
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import smithy.scala.runtime.*
import smithy.scala.runtime.codecs.*

import zio.blocks.schema.DynamicValue

import example.dynamodb.*
import example.weather.*

/** The payloads: one small RPC response and one larger, nested DynamoDB item. */
object Payloads {
  val forecast: GetForecastOutput = GetForecastOutput(
    days = Some(List.tabulate(3)(i => Some(Day(date = Some(Instant.parse("2026-11-28T00:00:00Z").plusSeconds(86400L * i)), copy_ = Some(s"day $i"))))),
    next = Some("token-123"),
    `type` = Kind.Rain(3)
  )

  /** 50 attributes: strings, numbers, blobs, booleans, and nested lists and maps. */
  val put: Put = Put(
    item = (0 until 50).map { i =>
      s"attr$i" -> (i % 5 match {
        case 0 => AttributeValue.S(s"value number $i")
        case 1 => AttributeValue.N(i.toString)
        case 2 => AttributeValue.B(Blob.fromUtf8(s"bytes-$i" * 4))
        case 3 => AttributeValue.L(List(AttributeValue.BOOL(true), AttributeValue.N("1.5"), AttributeValue.S("x")))
        case _ => AttributeValue.M(Map("a" -> AttributeValue.S("b"), "c" -> AttributeValue.M(Map("d" -> AttributeValue.N("2")))))
      })
    }.toMap,
    tableName = "orders"
  )

  def codec(name: String): Codec = name match {
    case "jackson"  => new JsonCodec(JsonSettings(useJsonName = false, useTimestampFormat = false))
    case "zio-json" => new ZioBlocksJsonCodec
    case "msgpack"  => new MessagePackCodec
    case "avro"     => new AvroCodec
    case "avro-schema" => new AvroSchemaCodec
    case "yaml"     => new YamlCodec
    case "thrift"   => new ThriftCodec
    case other      => throw new IllegalArgumentException(other)
  }
}

abstract class PayloadBench[A](value: A)(using c: SchemaCodec[A]) {
  protected var codec: Codec        = scala.compiletime.uninitialized
  protected var bytes: Array[Byte]  = scala.compiletime.uninitialized

  protected def setUp(name: String): Unit = {
    codec = Payloads.codec(name)
    bytes = codec.serialize(w => c.write(w, c.schema, value))
  }

  protected def doWrite(): Array[Byte] = codec.serialize(w => c.write(w, c.schema, value))
  protected def doRead(): A            = codec.deserialize(bytes)(r => c.read(r, c.schema))
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class ForecastBench extends PayloadBench(Payloads.forecast) {
  @Param(Array("jackson", "zio-json", "msgpack", "avro", "avro-schema", "yaml"))
  var format: String = scala.compiletime.uninitialized

  @Setup def setup(): Unit = setUp(format)

  @Benchmark def write(): Array[Byte]     = doWrite()
  @Benchmark def read(): GetForecastOutput = doRead()
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class PutBench extends PayloadBench(Payloads.put) {
  @Param(Array("jackson", "zio-json", "msgpack", "avro", "avro-schema", "yaml"))
  var format: String = scala.compiletime.uninitialized

  @Setup def setup(): Unit = setUp(format)

  @Benchmark def write(): Array[Byte] = doWrite()
  @Benchmark def read(): Put          = doRead()
}

/** The bridge alone, without any format: what every ZIO Blocks codec pays on top of its own encoding. */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class BridgeBench {
  private val c      = summon[SchemaCodec[Put]]
  private val bridge = new ZioBlocksJsonCodec
  private val value: DynamicValue = bridge.toDynamicValue(w => c.write(w, c.schema, Payloads.put))

  @Benchmark def toDynamicValue(): DynamicValue = bridge.toDynamicValue(w => c.write(w, c.schema, Payloads.put))
  @Benchmark def fromDynamicValue(): Put        = bridge.fromDynamicValue(value)(r => c.read(r, c.schema))
}

/** Prints the encoded size of each payload per format: `clientBench/runMain smithy.scala.bench.Sizes`. */
object Sizes {
  def main(args: Array[String]): Unit = {
    val f = summon[SchemaCodec[GetForecastOutput]]
    val p = summon[SchemaCodec[Put]]
    println("format        forecast      put")
    List("jackson", "zio-json", "msgpack", "avro", "avro-schema", "yaml", "thrift").foreach { name =>
      val codec = Payloads.codec(name)
      val small = codec.serialize(w => f.write(w, f.schema, Payloads.forecast)).length
      val large = codec.serialize(w => p.write(w, p.schema, Payloads.put)).length
      println(f"$name%-12s $small%9d $large%8d")
    }
  }
}
