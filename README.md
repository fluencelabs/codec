# Codec

`Codec` is an opinionated library for `cats`-friendly pure (reversible) conversions between types, with possible errors represented with `Either`.

It's cross-built for Scala and Scala.js, and can also be used for other free-of-effect conversions to reach functional composability.

## Motivation

### Use partial functions, track the errors

Often it's useful to do some validation alongside conversion, like in `String => Int`. 

However, this function throws when malformed input is given. Hence `String => Either[Throwable, Int]`, being a total function, should fit better.

In this case, error is reflected in type system. It keeps things pure. We go further, forcing the error to be of type `CodecError`, so that later it's easy to track where it comes from, especially in asynchronous environment.

This uni-direction type is called `fluence.codec.PureCodec.Func` for a fixed `CodecError` error type. Any other error type could be used by extending `fluence.codec.MonadicalEitherArrow[Error]`.

Bidirection type `A <=> B` is composed from two `Func`s and is called `Bijection`.

### Lawful composition

A type `Func[A, B]`, being something like `A => Either[E, B]`, is not very composable on it's own, so we implemented `cats.arrow.ArrowChoice[Func]` for it. You may use `cats.syntax.compose._` or anything like that to receive `andThen` and other lawful functions.

`Bijection[A, B]` is more complex type, so it has only `Compose[Bijection]` typeclass. Finally you can do something like this:

```scala
import cats.syntax.compose._
import fluence.codec.PureCodec

val intToBytes: PureCodec[Int, Array[Byte]] = PureCodec[Int, String] andThen PureCodec[String, Array[Byte]]
```

Errors are handled in monad-like "fail-fast" fashion.

### Benefit from different Monads

In general, functional types conversion could be lazy or eager, be performed in current thread or another. This choice should not affect the logic of conversion, as it's pure.

`PureCodec` may use any  monad to preform execution upon, retaining its nature. The most simple case is strict eager evaluation:

```scala
import cats.Id

val resEagerSync: Either[CodecError, Array[Byte]] = intToBytes.runF[Id](33)

```

You may use any monad, like `Task`, `Coeval`, `Eval`, `IO`...

### Minimal dependencies

`codec-core` depends only on `cats`. Each particular codec set is moved into separate module.

### Cross compile

In case of complex algorithms, it's worthy to share codebase between platforms. We cross-compile all the codecs possible both to Scala and Scala.js.

## Installation

```scala
// Bintray repo is used so far. Migration to Maven Central is planned
resolvers += Resolver.bintrayRepo("fluencelabs", "releases")

val codecV = "0.0.1"

libraryDependencies ++= Seq(
  "one.fluence" %%% "codec-core" % codecV, // basic types
  "one.fluence" %%% "codec-bits" % codecV, // scodec-bits conversions for ByteVector 
  "one.fluence" %%% "codec-circe" % codecV, // json handling with circe
  "one.fluence" %%% "codec-protobuf" % codecV, // ByteString conversions for both scala and scala.js
  "one.fluence" %% "codec-kryo" % codecV // typesafe kryo codecs, only for scala
)
```

## Example

```scala
  import cats.syntax.compose._
import fluence.codec.PureCodec
import fluence.codec.circe.CirceCodecs._
import io.circe.{Decoder, Encoder, Json}
import scodec.bits.ByteVector
import fluence.codec.bits.BitsCodecs._

  // Simple class
  case class User(id: Int, name: String)

  // Encode and decode with circe
  implicit val encoder: Encoder[User] =
    user ⇒ Json.obj("id" → Encoder.encodeInt(user.id), "name" → Encoder.encodeString(user.name))

  implicit val decoder: Decoder[User] = cursor ⇒
    for {
      id ← cursor.downField("id").as[Int]
      name ← cursor.downField("name").as[String]
    } yield User(id, name)

  // Get codec for encoder/decoder
  implicit val userJson: PureCodec[User, Json] = circeJsonCodec(encoder, decoder)

  // A trivial string to bytes codec; never use it in production!
  implicit val stringCodec: PureCodec[String, Array[Byte]] =
    PureCodec.liftB(_.getBytes, bs ⇒ new String(bs))

  // Convert user to byte vector and vice versa
  implicit val userJsonVec: PureCodec[User, ByteVector] =
    PureCodec[User, Json] andThen
      PureCodec[Json, String] andThen
      PureCodec[String, Array[Byte]] andThen
      PureCodec[Array[Byte], ByteVector]

  // Try it with an instance
  val user = User(234, "Hey Bob")

  // unsafe() is to be used in tests only; it throws!
  println(userJsonVec.direct.unsafe(user).toBase64)
  
  // eyJpZCI6MjM0LCJuYW1lIjoiSGV5IEJvYiJ9
```

For more real-world examples, see [Fluence](https://github.com/fluencelabs/fluence).

## Roadmap

- `connect[A, B, C]` to compose several Funcs or Bijections
- `sbt-tut` for docs
- Implement more codecs
- Enhance `Func` api with shortcuts to `EitherT` methods
- Consider improving performance: `EitherT` [is not so fast](https://twitter.com/alexelcu/status/988031831357485056) (at least yet)

## License

Fluence is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3 (AGPLv3) as published by the Free Software Foundation.

Fluence includes some [external modules](https://github.com/fluencelabs/codec/blob/master/build.sbt) that carry their own licensing.