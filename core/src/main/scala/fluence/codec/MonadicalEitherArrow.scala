/*
 * Copyright (C) 2017  Fluence Labs Limited
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fluence.codec

import cats.{Monad, MonadError, Traverse}
import cats.arrow.{ArrowChoice, Category}
import cats.syntax.arrow._
import cats.data.{EitherT, Kleisli}
import cats.syntax.flatMap._
import cats.syntax.compose._

import scala.language.{existentials, higherKinds}
import scala.util.Try

/**
 * MonadicalEitherArrow wraps Func and Bijection with a fixed E error type.
 *
 * @tparam E Error type
 */
abstract class MonadicalEitherArrow[E <: Throwable] {
  mea ⇒

  /**
   * Alias for error type
   */
  final type Error = E

  /**
   * Func is a special kind of arrow: A => EitherT[F, E, B] for any monad F[_].
   * This signature makes the type both flexible and pure. It could be eager or lazy, synchronous or not depending on the
   * call site context.
   *
   * @tparam A Input type
   * @tparam B Successful result type
   */
  abstract class Func[A, B] {
    f ⇒

    /**
     * Run the func on input, using the given monad.
     *
     * @param input Input
     * @tparam F All internal maps and composes are to be executed with this Monad
     */
    def runEither[F[_]: Monad](input: A): F[Either[E, B]] = apply[F](input).value

    /**
     * Run the func on input, lifting the error into MonadError effect.
     *
     * @param input Input
     * @param F All internal maps and composes, as well as errors, are to be executed with this MonadError.
     *          Error type should be a supertype for this arrow's error E.
     */
    def runF[F[_]](input: A)(implicit F: MonadError[F, EE] forSome { type EE >: E }): F[B] =
      runEither(input).flatMap(F.fromEither)

    /**
     * Run the func on input, producing EitherT-wrapped result.
     *
     * @param input Input
     * @tparam F All internal maps and composes are to be executed with this Monad
     */
    def apply[F[_]: Monad](input: A): EitherT[F, E, B]

    /**
     * Shortcut for function composition
     *
     * @param other Other function to run after
     * @tparam C Resulting input type
     * @return Composed function
     */
    def on[C](other: Func[C, A]): Func[C, B] =
      catsMonadicalEitherArrowChoice.compose(this, other)

    /**
     * Convert this Func into another one, lifting the error
     *
     * @param m Another instance of MonadicalEitherArrow
     * @param convertE Convert error
     * @tparam EE Error type
     * @return Converted function
     */
    def to[EE <: Throwable](m: MonadicalEitherArrow[EE])(implicit convertE: E ⇒ EE): m.Func[A, B] =
      new m.Func[A, B] {
        override def apply[F[_]: Monad](input: A): EitherT[F, EE, B] =
          f[F](input).leftMap(convertE)
      }

    /**
     * Converts this Func to Kleisli, using MonadError to execute upon and to lift errors into.
     *
     * @param F All internal maps and composes, as well as errors, are to be executed with this MonadError.
     *           Error type should be a supertype for this arrow's error E.
     */
    def toKleisli[F[_]](implicit F: MonadError[F, EE] forSome { type EE >: E }): Kleisli[F, A, B] =
      Kleisli(input ⇒ runF[F](input))

    /**
     * Run the function, throw the error if it happens. Intended to be used only in tests.
     *
     * @param input Input
     * @return Value, or throw exception of type E
     */
    def unsafe(input: A): B = {
      import cats.instances.try_._
      runF[Try](input).get
    }

    /**
     * Picks a point from the arrow, using the initial element (Unit) on the left.
     *
     * @param input Point to pick
     * @return Picked point
     */
    def pointAt(input: A): Point[B] =
      catsMonadicalEitherArrowChoice.lmap(this)(_ ⇒ input)
  }

  /**
   * Bijection is a transformation A => B with inverse B => A, both being partial functions which may fail with E.
   *
   * @param direct Direct transformation
   * @param inverse Inverse transformation
   * @tparam A Source type
   * @tparam B Target type
   */
  case class Bijection[A, B](direct: Func[A, B], inverse: Func[B, A]) {

    /**
     * Bijection with source and target types swapped
     */
    lazy val swap: Bijection[B, A] = Bijection(inverse, direct)

    /**
     * Splits the input and puts it to either bijection, then merges output.
     * It could have been achieved with `Strong` typeclass in case it doesn't extend `Profunctor`; but it does.
     */
    def split[A1, B1](bj: Bijection[A1, B1]): Bijection[(A, A1), (B, B1)] =
      mea.split(this, bj)
  }

  /**
   * Build a Func from an instance of Func with another error type EE.
   *
   * @param other Other Func.
   * @param convertE A way to convert other error into E.
   * @tparam EE Other Func's error.
   * @tparam A Source type
   * @tparam B Target type
   * @return Func, produced from the other func by leftMapping its result
   */
  implicit def fromOtherFunc[EE <: Throwable, A, B](
    other: MonadicalEitherArrow[EE]#Func[A, B]
  )(implicit convertE: EE ⇒ E): Func[A, B] =
    new Func[A, B] {
      override def apply[F[_]: Monad](input: A): EitherT[F, E, B] =
        other(input).leftMap(convertE)
    }

  /**
   * Provides a Func for any traversable type, given the scalar Func.
   *
   * @param f Scalar func
   * @tparam G Traversable type
   * @tparam A Source type
   * @tparam B Target type
   */
  implicit def funcForTraverse[G[_]: Traverse, A, B](implicit f: Func[A, B]): Func[G[A], G[B]] =
    new Func[G[A], G[B]] {
      override def apply[F[_]: Monad](input: G[A]): EitherT[F, E, G[B]] =
        Traverse[G].traverse[EitherT[F, E, ?], A, B](input)(f.apply[F](_))
    }

  /**
   * Bijection Summoner -- useful for making a composition of bijections.
   */
  def apply[A, B](implicit bijection: Bijection[A, B]): Bijection[A, B] = bijection

  /**
   * Lifts a pure function into Func context.
   *
   * @param f Function to lift
   */
  def liftFunc[A, B](f: A ⇒ B): Func[A, B] = new Func[A, B] {
    override def apply[F[_]: Monad](input: A): EitherT[F, E, B] =
      EitherT.rightT[F, E](input).map(f)
  }

  /**
   * Lift a pure function, returning Either, into Func context.
   *
   * @param f Function to lift
   */
  def liftFuncEither[A, B](f: A ⇒ Either[E, B]): Func[A, B] = new Func[A, B] {
    override def apply[F[_]: Monad](input: A): EitherT[F, E, B] =
      EitherT.rightT[F, E](input).subflatMap(f)
  }

  /**
   * Check a condition, lifted with a Func.
   *
   * @param error Error to produce when condition is not met
   * @return Func that takes boolean, checks it, and returns Unit or fails with given error
   */
  def cond(error: ⇒ E): Func[Boolean, Unit] =
    liftFuncEither(Either.cond(_, (), error))

  /**
   * Lift a function which returns a Func arrow with Unit on the left side.
   *
   * @param f Function to lift
   */
  def liftFuncPoint[A, B](f: A ⇒ Point[B]): Func[A, B] =
    new Func[A, B] {
      override def apply[F[_]: Monad](input: A): EitherT[F, E, B] =
        f(input).apply[F](())
    }

  /**
   * Func that does nothing with input.
   */
  implicit def identityFunc[T]: Func[T, T] = liftFunc(identity)

  /**
   * Func should obey ArrowChoiceLaws
   */
  implicit object catsMonadicalEitherArrowChoice extends ArrowChoice[Func] {
    override def choose[A, B, C, D](f: Func[A, C])(g: Func[B, D]): Func[Either[A, B], Either[C, D]] =
      new Func[Either[A, B], Either[C, D]] {
        override def apply[F[_]: Monad](input: Either[A, B]): EitherT[F, E, Either[C, D]] =
          input.fold(
            f(_).map(Left(_)),
            g(_).map(Right(_))
          )
      }

    override def lift[A, B](f: A ⇒ B): Func[A, B] =
      liftFunc(f)

    override def first[A, B, C](fa: Func[A, B]): Func[(A, C), (B, C)] = new Func[(A, C), (B, C)] {
      override def apply[F[_]: Monad](input: (A, C)): EitherT[F, E, (B, C)] =
        fa(input._1).map(_ → input._2)
    }

    override def compose[A, B, C](f: Func[B, C], g: Func[A, B]): Func[A, C] =
      new Func[A, C] {
        override def apply[F[_]: Monad](input: A): EitherT[F, E, C] =
          g(input).flatMap(f(_))
      }
  }

  /**
   * Point type maps from Unit to a particular value of A, so it's just a lazy Func.
   *
   * @tparam A Output value type
   */
  type Point[A] = Func[Unit, A]

  /**
   * Point must obey MonadErrorLaws
   */
  implicit object catsMonadicalEitherPointMonad extends MonadError[Point, E] {
    override def flatMap[A, B](fa: Point[A])(f: A ⇒ Point[B]): Point[B] =
      new Func[Unit, B] {
        override def apply[F[_]: Monad](input: Unit): EitherT[F, E, B] =
          fa[F](()).flatMap(f(_).apply[F](()))
      }

    override def tailRecM[A, B](a: A)(f: A ⇒ Point[Either[A, B]]): Point[B] =
      new Func[Unit, B] {
        override def apply[F[_]: Monad](input: Unit): EitherT[F, E, B] =
          Monad[EitherT[F, E, ?]].tailRecM(a)(f(_).apply[F](()))
      }

    override def raiseError[A](e: E): Point[A] =
      liftFuncEither(_ ⇒ Left(e))

    override def handleErrorWith[A](fa: Point[A])(f: E ⇒ Point[A]): Point[A] =
      new Func[Unit, A] {
        override def apply[F[_]: Monad](input: Unit): EitherT[F, E, A] =
          fa[F](()).leftFlatMap(e ⇒ f(e).apply[F](()))
      }

    override def pure[A](x: A): Point[A] =
      liftFunc(_ ⇒ x)
  }

  /**
   * Lifts pure direct and inverse functions into Bijection.
   *
   * @param direct Pure direct transformation.
   * @param inverse Pure inverse transformation.
   * @tparam A Source type.
   * @tparam B Target type.
   */
  def liftB[A, B](direct: A ⇒ B, inverse: B ⇒ A): Bijection[A, B] =
    Bijection(liftFunc(direct), liftFunc(inverse))

  /**
   * Lifts partial direct and inverse functions (returning errors with Either) into Bijection.
   *
   * @param direct Partial direct transformation.
   * @param inverse Partial inverse transformation.
   * @tparam A Source type.
   * @tparam B Target type.
   */
  def liftEitherB[A, B](direct: A ⇒ Either[E, B], inverse: B ⇒ Either[E, A]): Bijection[A, B] =
    Bijection(liftFuncEither(direct), liftFuncEither(inverse))

  /**
   * Lifts point functions into Bijection.
   */
  def liftPointB[A, B](direct: A ⇒ Point[B], inverse: B ⇒ Point[A]): Bijection[A, B] =
    Bijection(liftFuncPoint(direct), liftFuncPoint(inverse))

  /**
   * Bijection that does no transformation.
   */
  implicit def identityBijection[T]: Bijection[T, T] = Bijection(identityFunc, identityFunc)

  /**
   * Bijection for any traversable type.
   *
   * @param bijection Scalar bijection
   * @tparam G Traversable type
   * @tparam A Source type
   * @tparam B Target type
   */
  implicit def traversableBijection[G[_]: Traverse, A, B](implicit bijection: Bijection[A, B]): Bijection[G[A], G[B]] =
    Bijection(funcForTraverse(Traverse[G], bijection.direct), funcForTraverse(Traverse[G], bijection.inverse))

  /**
   * Gives a bijection with source and target types swapped.
   */
  implicit def swap[A, B](implicit bijection: Bijection[A, B]): Bijection[B, A] = bijection.swap

  /**
   * Bijection should obey CategoryLaws
   */
  implicit object catsMonadicalBijectionCategory extends Category[Bijection] {
    override def compose[A, B, C](f: Bijection[B, C], g: Bijection[A, B]): Bijection[A, C] =
      Bijection(f.direct compose g.direct, g.inverse compose f.inverse)

    override def id[A]: Bijection[A, A] = identityBijection
  }

  /**
   * Splits the input and puts it to either bijection, then merges output.
   * It could be achieved with `Strong` typeclass in case it doesn't extend `Profunctor`; but it does.
   */
  def split[A1, B1, A2, B2](f1: Bijection[A1, B1], f2: Bijection[A2, B2]): Bijection[(A1, A2), (B1, B2)] =
    Bijection(f1.direct *** f2.direct, f1.inverse *** f2.inverse)

}
