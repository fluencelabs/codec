# Codec

`Codec` is an opinionated library for `cats`-friendly pure (reversible) conversions between types, with possible errors represented with `Either`.

It's cross-built for Scala and Scala.js, and can also be used for other free-of-effect conversions to reach functional composability.

## Motivation

### Use partial functions, track the errors

Often it's useful to do some validation alongside conversion, like in `String => Int`. 

However, this function throws when malformed input is given. Hence `String => Either[Throwable, Int]`, being a total function, should fit better.

In this case, error is reflected in type system. It keeps things pure. We go further, forcing the error to be of type `CodecError`, so that later it's easy to track where it comes from, especially in asynchronous environment.

This uni-direction type is called `fluence.codec.PureCodec.Func` for a fixed `CodecError` error type. Any other error type could be used by extending `fluence.codec.MonadicalEitherArrow[Error]`.

Bidirection type `A <=> B` is composed from two `Func`'s and is called `Bijection`.

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

Should pure functional types conversion be lazy or eager? Should it be performed in current thread or another?

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

## Roadmap

- `connect[A, B, C]` to compose several Funcs or Bijections
- `sbt-tut` for docs
- Implement more codecs
- Enhance `Func` api with shortcuts to `EitherT` methods

## License

Fluence is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3 (AGPLv3) as published by the Free Software Foundation.

Fluence includes some [external modules](https://github.com/fluencelabs/codec/blob/master/build.sbt) that carry their own licensing.