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
package com.newzly.phantom.dsl.batch

import scala.concurrent.blocking
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.SpanSugar._
import com.newzly.phantom.Implicits._
import com.newzly.phantom.tables.{ JodaRow, PrimitivesJoda }
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.newzly.util.testing.cassandra.BaseTest


class BatchTest extends BaseTest {
  val keySpace: String = "batch_tests"
  implicit val s: PatienceConfiguration.Timeout = timeout(10 seconds)

  override def beforeAll(): Unit = {
    blocking {
      super.beforeAll()
      PrimitivesJoda.insertSchema()
    }
  }

  it should "get the correct count for batch queries" in {
    val row = JodaRow.sample
    val statement3 = PrimitivesJoda.update
      .where(_.pkey eqs row.pkey)
      .modify(_.intColumn setTo row.int)
      .and(_.timestamp setTo row.bi)

    val statement4 = PrimitivesJoda.delete
      .where(_.pkey eqs row.pkey)

    val batch = BatchStatement().add(statement3, statement4)

  }

  it should "serialize a multiple table batch query" in {

    val row = JodaRow.sample
    val row2 = JodaRow.sample.copy(pkey = row.pkey)
    val row3 = JodaRow.sample

    val statement3 = PrimitivesJoda.update
      .where(_.pkey eqs row2.pkey)
      .modify(_.intColumn setTo row2.int)
      .and(_.timestamp setTo row2.bi)

    val statement4 = PrimitivesJoda.delete
      .where(_.pkey eqs row3.pkey)

    val batch = BatchStatement().add(statement3, statement4)
    batch.queryString shouldEqual s"BEGIN BATCH UPDATE PrimitivesJoda SET intColumn=${row2.int},timestamp=${row2.bi.getMillis} WHERE pkey='${row2.pkey}';DELETE  FROM PrimitivesJoda WHERE pkey='${row3.pkey}';APPLY BATCH;"
  }

  it should "correctly execute a batch query" in {
    val row = JodaRow.sample
    val row2 = JodaRow.sample.copy(pkey = row.pkey)
    val row3 = JodaRow.sample

    val statement1 = PrimitivesJoda.insert
        .value(_.pkey, row.pkey)
        .value(_.intColumn, row.int)
        .value(_.timestamp, row.bi)

    val statement2 = PrimitivesJoda.insert
      .value(_.pkey, row3.pkey)
      .value(_.intColumn, row3.int)
      .value(_.timestamp, row3.bi)

    val statement3 = PrimitivesJoda.update
      .where(_.pkey eqs row2.pkey)
      .modify(_.intColumn setTo row2.int)
      .and(_.timestamp setTo  row2.bi)

    val statement4 = PrimitivesJoda.delete
      .where(_.pkey eqs row3.pkey)

    val batch = BatchStatement().add(statement3).add(statement4)

    val w = for {
      s1 <- statement1.future()
      s3 <- statement2.future()
      b <- batch.future()
      updated <- PrimitivesJoda.select.where(_.pkey eqs row.pkey).one()
      deleted <- PrimitivesJoda.select.where(_.pkey eqs row3.pkey).one()
    } yield (updated, deleted)

    w successful {
      res => {
        res._1.isDefined shouldEqual true
        res._1.get shouldEqual row2

        res._2.isEmpty shouldEqual true
      }
    }
  }

  it should "correctly execute a batch query with Twitter Futures" in {
    val row = JodaRow.sample
    val row2 = JodaRow.sample.copy(pkey = row.pkey)
    val row3 = JodaRow.sample

    val statement1 = PrimitivesJoda.insert
      .value(_.pkey, row.pkey)
      .value(_.intColumn, row.int)
      .value(_.timestamp, row.bi)

    val statement2 = PrimitivesJoda.insert
      .value(_.pkey, row3.pkey)
      .value(_.intColumn, row3.int)
      .value(_.timestamp, row3.bi)

    val statement3 = PrimitivesJoda.update
      .where(_.pkey eqs row2.pkey)
      .modify(_.intColumn setTo row2.int)
      .and(_.timestamp setTo row2.bi)

    val statement4 = PrimitivesJoda.delete
      .where(_.pkey eqs row3.pkey)

    val batch = BatchStatement().add(statement3).add(statement4)

    val w = for {
      s1 <- statement1.execute()
      s3 <- statement2.execute()
      b <- batch.execute()
      updated <- PrimitivesJoda.select.where(_.pkey eqs row.pkey).get()
      deleted <- PrimitivesJoda.select.where(_.pkey eqs row3.pkey).get()
    } yield (updated, deleted)

    w successful {
      res => {
        res._1.isDefined shouldEqual true
        res._1.get shouldEqual row2

        res._2.isEmpty shouldEqual true
      }
    }
  }
}
