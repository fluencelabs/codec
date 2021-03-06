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

import cats.laws.discipline.CategoryTests
import cats.tests.CatsSuite
import cats.Eq
import org.scalacheck.ScalacheckShapeless._

class PureCodecBijectionLawsSpec extends CatsSuite {
  import PureCodecFuncTestInstances._

  implicit def eqBifuncE[A, B](
    implicit directEq: Eq[PureCodec.Func[A, B]],
    inverseEq: Eq[PureCodec.Func[B, A]]
  ): Eq[PureCodec.Bijection[A, B]] =
    Eq.instance((x, y) ⇒ directEq.eqv(x.direct, y.direct) && inverseEq.eqv(x.inverse, y.inverse))

  checkAll(
    "PureCodec.Bijection.CategoryLaws",
    CategoryTests[PureCodec].category[Int, String, Double, BigDecimal]
  )
}
