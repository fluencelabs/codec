package fluence.codec.examples

import cats.Id
import fluence.codec.kryo.KryoCodecs
import fluence.codec.{CodecError, PureCodec}
import monix.eval.Task
import shapeless.{::, HNil}

object KryoCodecExample {
  case class Aircraft(manufacturer: String, model: String, tailNumber: String)
  case class Fuel(amount: Double) extends AnyVal

  case class UnknownClass(x: String)

  def main(args: Array[String]): Unit = {
    // This way we can define a typed collection of codecs using kryo for the underlying serialization.
    //
    // These codecs can be used only to transform the corresponding type: i.e. it won't be possible to
    // use an aircraft codec to serialize fuel (which is essentially a typed wrapper over double value).
    //
    // It won't be possible to obtain from this collection a codec for previously not registered class.
    // Type safety FTW!
    //
    // Note that different methods are used to register Aircraft and Fuel – that's because one is a reference,
    // and another is a value type.
    val codecs: KryoCodecs[Task, ::[Fuel, ::[Aircraft, ::[Array[Byte], ::[Long, ::[String, HNil]]]]]] = KryoCodecs()
      .addCase(classOf[Aircraft])
      .add[Fuel]
      .build[Task]()

    val skyhawk61942 = Aircraft("Cessna", "172S G1000", "N61942")
    val tabsFuel = Fuel(53)

    val aircraftCodec: PureCodec[Aircraft, Array[Byte]] = codecs.pureCodec[Aircraft]
    val fuelCodec: PureCodec[Fuel, Array[Byte]] = codecs.pureCodec[Fuel]

    // This will cause a compilation error, because the class was never registered with the codecs.
    // "You requested an element of type (...).UnknownClass, but there is none in the HList"
    //
    // val unknownCodec = codecs.pureCodec[UnknownClass]


    // Here all the standard machinery of codecs applies (for more examples, consider checking out PureCodecExample.
    // We can serialize and deserialize the object – and unsurprisingly the original and restored values match.
    //
    // Let's serialize an aircraft instance.
    {
      val ser: Id[Either[CodecError, Array[Byte]]] = aircraftCodec.direct[Id](skyhawk61942).value
      val deser: Id[Either[CodecError, Aircraft]] = aircraftCodec.inverse[Id](ser.right.get).value

      println(ser.right.map(x => s"$skyhawk61942 => serialized size: ${x.length}"))
      assert(deser == Right(skyhawk61942))
    }


    // Same thing for the fuel instance (which is AnyVal fwiw).
    {
      val ser: Id[Either[CodecError, Array[Byte]]] = fuelCodec.direct[Id](tabsFuel).value
      val deser: Id[Either[CodecError, Fuel]] = fuelCodec.inverse[Id](ser.right.get).value

      println(ser.right.map(x => s"$tabsFuel => serialized size: ${x.length}"))
      assert(deser == Right(tabsFuel))
    }
  }
}
