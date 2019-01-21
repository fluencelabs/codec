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

import cats.{Eq, Invariant}
import cats.data.EitherT
import cats.syntax.functor._
import cats.laws.discipline.{MonadErrorTests, SemigroupalTests}
import cats.tests.CatsSuite
import fluence.codec
import org.scalacheck.ScalacheckShapeless._

class PureCodecPointLawsSpec extends CatsSuite {

  import PureCodecFuncTestInstances._

  implicit def eqEitherTFEA: Eq[EitherT[PureCodec.Point, CodecError, Int]] =
    Eq.instance{
      case (aa,bb) ⇒
        aa.value.unsafe(()) == bb.value.unsafe(())
    }

  implicit val iso = SemigroupalTests.Isomorphisms.invariant[PureCodec.Point](
    new Invariant[PureCodec.Point]{
      override def imap[A, B](fa: codec.PureCodec.Point[A])(f: A ⇒ B)(g: B ⇒ A): codec.PureCodec.Point[B] =
        fa.map(f)
    }
  )

  checkAll(
    "PureCodec.Point.MonadErrorLaws",
    MonadErrorTests[PureCodec.Point, CodecError].monadError[Int, String, Double]
  )
}
