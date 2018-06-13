package fluence.codec.examples

import cats.Id
import cats.data.EitherT
import cats.implicits._
import fluence.codec.{CodecError, PureCodec}
import fluence.codec.PureCodec.Bijection
import fluence.codec.PureCodec.Point

import scala.util.Try

object PureCodecExample {
  def main(args: Array[String]): Unit = {
    // Here we are defining a simple codec transforming a string to integer and back.
    //
    // It's not really a bijection: even not taking into account unparseable strings like "test", there are
    // different string values (e.g., "+20" and "20") producing the same integer value. It's good enough for
    // demonstration purposes though, so we keep using it.
    val str2intCodec: Bijection[String, Int] = PureCodec.build[String, Int](
      (x: String) => x.toInt,
      (x: Int) => x.toString
    )


    // Using an identity monad, we can parse a valid string into integer (which produces EitherT) and then map
    // the result. Now, we can use EitherT[F, E, B] or convert it into F[Either[E, B]] representation.
    {
      val res: EitherT[Id, CodecError, Int] = str2intCodec.direct[Id]("31330").map(_ + 7)
      val resMonad: Id[Either[CodecError, Int]] = res.value
      assert(res.toString == "EitherT(Right(31337))")
      assert(resMonad.toString == "Right(31337)")
    }


    // We can also supply a different type class (Monad[F[_]]) – in this case the result will be wrapped into
    // the corresponding type F[_] using the `F.pure(_)` method.
    {
      val res = str2intCodec.direct[Option]("42")
      val resMonad = res.value
      assert(res.toString == "EitherT(Some(Right(42)))")
      assert(resMonad.toString == "Some(Right(42))")
    }


    // Here we attempt to pass an unparseable string value. Note that PureCodec won't catch a thrown exception
    // automatically despite that return type is EitherT (this might be a bit confusing). Instead, the exception
    // will come all the way up to the caller, which will have to handle it manually.
    {
      val resWrapped = Try {
        val res: EitherT[Id, CodecError, Int] = str2intCodec.direct[Id]("foo")
        res
      }
      assert(resWrapped.toString == "Failure(java.lang.NumberFormatException: For input string: \"foo\")")
    }


    // To handle exceptions automatically, we can use Try monad. Note that we get `EitherT(Failure(...))`, not
    // `EitherT(Failure(Right(...)))` as one might expect by analogy with previous examples. It's not
    // `EitherT(Left(...))` too which could have been more convenient potentially.
    {
      val res = str2intCodec.direct[Try]("foo")
      val resMonad: Try[Either[CodecError, Int]] = res.value
      assert(res.toString == "EitherT(Failure(java.lang.NumberFormatException: For input string: \"foo\"))")
      assert(resMonad.toString == "Failure(java.lang.NumberFormatException: For input string: \"foo\")")
    }


    // If we really want to receive Left with the exception info when the string argument can't be parsed, a little
    // more effort is needed. The problem we had before was that the supplied function `(x: String) => x.toInt`
    // could throw parse exceptions and therefore was not really pure.
    //
    // However, we can catch exceptions in this function and return an Either, which will make it pure. Now, all we
    // need to do is to lift this function into the Func context.
    val str2intEitherCodec: Bijection[String, Int] = PureCodec.build(
      PureCodec.liftFuncEither((x: String) => Either.catchNonFatal(x.toInt).left.map(e => CodecError(e.getMessage))),
      PureCodec.liftFuncEither((x: Int) => Either.catchNonFatal(x.toString).left.map(e => CodecError(e.getMessage)))
    )


    // For lawful strings – those which can be parsed into an integer the behavior hasn't really changed.
    // Note that we receive Right(...) wrapped in the supplied monadic type.
    {
      val res: EitherT[Option, CodecError, Int] = str2intEitherCodec.direct[Option]("1024")
      val resMonad = res.value
      assert(res.toString == "EitherT(Some(Right(1024)))")
      assert(resMonad.toString == "Some(Right(1024))")
    }


    // However, for strings that can't be parsed, we will receive Left(...) – which is a desired behavior!
    {
      val res: EitherT[Option, CodecError, Int] = str2intEitherCodec.direct[Option]("bar")
      val resMonad = res.value
      assert(res.toString == "EitherT(Some(Left(fluence.codec.CodecError: For input string: \"bar\")))")
      assert(resMonad.toString == "Some(Left(fluence.codec.CodecError: For input string: \"bar\"))")
    }


    // It's also totally possible to perform an inverse transformation: after all, a codec is a bijection.
    {
      val res: EitherT[Id, CodecError, String] = str2intCodec.inverse[Id](720)
      val resMonad: Id[Either[CodecError, String]] = res.value
      assert(res.toString == "EitherT(Right(720))")
      assert(resMonad.toString == "Right(720)")
    }


    // It's also possible to pass the to-be-converted value first, but perform the actual transformation only
    // later on (using different enclosing monads if desired). To achieve this, `pointAt` method which returns a
    // lazily evaluated function can be used.
    {
      val point: Point[Int] = str2intCodec.direct.pointAt("333")
      val resId: EitherT[Id, CodecError, Int] = point[Id]()
      val resOption: EitherT[Option, CodecError, Int] = point[Option]()
      assert(resId.toString == "EitherT(Right(333))")
      assert(resOption.toString == "EitherT(Some(Right(333)))")
    }


    // TODO: describe `runF` and `toKleisli`
  }
}
