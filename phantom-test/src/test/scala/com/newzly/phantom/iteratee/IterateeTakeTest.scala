/*
 * Copyright 2013 newzly ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newzly.phantom.iteratee

import scala.concurrent.Future
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.SpanSugar._
import com.newzly.phantom.tables.{ Primitive, Primitives }
import com.newzly.util.testing.cassandra.BaseTest
import com.newzly.util.testing.AsyncAssertionsHelper._

class IterateeTakeTest extends BaseTest {
  val keySpace: String = "iteratee_take_tests"
  implicit val s: PatienceConfiguration.Timeout = timeout(2 minutes)

  ignore should "take records from the iterator" in {
    Primitives.insertSchema()
    val rows = for (i <- 1 to 100) yield  Primitive.sample
    var count = 0
    val batch = Iterator.fill(100) {
      val row = rows(count)
      val st = Primitives.insert
        .value(_.pkey, row.pkey)
        .value(_.long, row.long)
        .value(_.boolean, row.boolean)
        .value(_.bDecimal, row.bDecimal)
        .value(_.double, row.double)
        .value(_.float, row.float)
        .value(_.inet, row.inet)
        .value(_.int, row.int)
        .value(_.date, row.date)
        .value(_.uuid, row.uuid)
        .value(_.bi, row.bi)
        .future()
      count += 1
      st
    }

    val traverse = Future.sequence(batch)
    val w = traverse.map(_ => Primitives.select.fetchEnumerator)

    val m = w flatMap {
      en => en run Iteratee.take(10)
    }

    m successful {
      res =>
        res.toIndexedSeq shouldBe rows.take(10)
    }
  }

}
