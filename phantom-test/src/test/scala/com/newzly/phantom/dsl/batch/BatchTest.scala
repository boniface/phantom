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
import org.joda.time.DateTime
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.SpanSugar._
import com.newzly.phantom.Implicits._
import com.newzly.phantom.tables.{ JodaRow, PrimitivesJoda, Recipe, Recipes }
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

  it should "serialize a multiple table batch query applied to multiple statements" in {

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

  it should "serialize a multiple table batch query chained from adding statements" in {

    val row = JodaRow.sample
    val row2 = JodaRow.sample.copy(pkey = row.pkey)
    val row3 = JodaRow.sample

    val statement3 = PrimitivesJoda.update
      .where(_.pkey eqs row2.pkey)
      .modify(_.intColumn setTo row2.int)
      .and(_.timestamp setTo row2.bi)

    val statement4 = PrimitivesJoda.delete
      .where(_.pkey eqs row3.pkey)

    val batch = BatchStatement().add(statement3).add(statement4)
    batch.queryString shouldEqual s"BEGIN BATCH UPDATE PrimitivesJoda SET intColumn=${row2.int},timestamp=${row2.bi.getMillis} WHERE pkey='${row2.pkey}';DELETE  FROM PrimitivesJoda WHERE pkey='${row3.pkey}';APPLY BATCH;"
  }

  it should "correctly execute a chain of INSERT queries" in {
    val row = JodaRow.sample
    val row2 = JodaRow.sample
    val row3 = JodaRow.sample

    val statement1 = PrimitivesJoda.insert
      .value(_.pkey, row.pkey)
      .value(_.intColumn, row.int)
      .value(_.timestamp, row.bi)

    val statement2 = PrimitivesJoda.insert
      .value(_.pkey, row2.pkey)
      .value(_.intColumn, row2.int)
      .value(_.timestamp, row2.bi)

    val statement3 = PrimitivesJoda.insert
      .value(_.pkey, row3.pkey)
      .value(_.intColumn, row3.int)
      .value(_.timestamp, row3.bi)

    val batch = BatchStatement().add(statement1).add(statement2).add(statement3)

    val chain = for {
      ex <- PrimitivesJoda.truncate.future()
      batchDone <- batch.future()
      count <- PrimitivesJoda.count.one()
    } yield count

    chain.successful {
      res => {
        res.isDefined shouldEqual true
        res.get shouldEqual 3
      }
    }
  }

  it should "correctly execute a chain of INSERT queries with Twitter Futures" in {
    val row = JodaRow.sample
    val row2 = JodaRow.sample
    val row3 = JodaRow.sample

    val statement1 = PrimitivesJoda.insert
      .value(_.pkey, row.pkey)
      .value(_.intColumn, row.int)
      .value(_.timestamp, row.bi)

    val statement2 = PrimitivesJoda.insert
      .value(_.pkey, row2.pkey)
      .value(_.intColumn, row2.int)
      .value(_.timestamp, row2.bi)

    val statement3 = PrimitivesJoda.insert
      .value(_.pkey, row3.pkey)
      .value(_.intColumn, row3.int)
      .value(_.timestamp, row3.bi)

    val batch = BatchStatement().add(statement1).add(statement2).add(statement3)

    val chain = for {
      ex <- PrimitivesJoda.truncate.execute()
      batchDone <- batch.execute()
      count <- PrimitivesJoda.count.get()
    } yield count

    chain.successful {
      res => {
        res.isDefined shouldEqual true
        res.get shouldEqual 3
      }
    }
  }

  it should "correctly execute a chain of INSERT queries and not perform multiple inserts" in {
    val row = JodaRow.sample

    val statement1 = PrimitivesJoda.insert
      .value(_.pkey, row.pkey)
      .value(_.intColumn, row.int)
      .value(_.timestamp, row.bi)

    val batch = BatchStatement().add(statement1).add(statement1.ifNotExists()).add(statement1.ifNotExists())

    val chain = for {
      ex <- PrimitivesJoda.truncate.future()
      batchDone <- batch.future()
      count <- PrimitivesJoda.count.one()
    } yield count

    chain.successful {
      res => {
        res.isDefined shouldEqual true
        res.get shouldEqual 1
      }
    }
  }

  it should "correctly execute a chain of INSERT queries and not perform multiple inserts with Twitter Futures" in {
    val row = JodaRow.sample

    val statement1 = PrimitivesJoda.insert
      .value(_.pkey, row.pkey)
      .value(_.intColumn, row.int)
      .value(_.timestamp, row.bi)

    val batch = BatchStatement().add(statement1).add(statement1.ifNotExists()).add(statement1.ifNotExists())

    val chain = for {
      ex <- PrimitivesJoda.truncate.future()
      batchDone <- batch.future()
      count <- PrimitivesJoda.count.one()
    } yield count

    chain.successful {
      res => {
        res.isDefined shouldEqual true
        res.get shouldEqual 1
      }
    }
  }

  it should "correctly execute an UPDATE/DELETE pair batch query" in {
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

  it should "prioritise batch updates in a last first order" in {
    val row = JodaRow.sample

    val statement1 = PrimitivesJoda.insert
      .value(_.pkey, row.pkey)
      .value(_.intColumn, row.int)
      .value(_.timestamp, row.bi)

    val batch = BatchStatement()
      .add(statement1)
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo row.int))
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 10)))
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 15)))
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 20)))

    val chain = for {
      done <- batch.execute()
      updated <- PrimitivesJoda.select.where(_.pkey eqs row.pkey).get()
    } yield updated

    chain.successful {
      res => {
        res.isDefined shouldEqual true
        res.get.int shouldEqual (row.int + 20)
      }
    }
  }

  it should "prioritise batch updates in a last first order with Twitter Futures" in {
    val row = JodaRow.sample

    val statement1 = PrimitivesJoda.insert
      .value(_.pkey, row.pkey)
      .value(_.intColumn, row.int)
      .value(_.timestamp, row.bi)

    val batch = BatchStatement()
      .add(statement1)
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo row.int))
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 10)))
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 15)))
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 20)))

    val chain = for {
      done <- batch.future()
      updated <- PrimitivesJoda.select.where(_.pkey eqs row.pkey).one()
    } yield updated

    chain.successful {
      res => {
        res.isDefined shouldEqual true
        res.get.int shouldEqual (row.int + 20)
      }
    }
  }

  it should "prioritise batch updates based on a timestamp" in {
    val row = JodaRow.sample

    val last = new DateTime()
    val last2 = last.withDurationAdded(20, 5)

    val statement1 = PrimitivesJoda.insert
      .value(_.pkey, row.pkey)
      .value(_.intColumn, row.int)
      .value(_.timestamp, row.bi)

    val batch = BatchStatement()
      .add(statement1)
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 10)).timestamp(last2.getMillis))
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 15)))

    val chain = for {
      done <- batch.future()
      updated <- PrimitivesJoda.select.where(_.pkey eqs row.pkey).one()
    } yield updated

    chain.successful {
      res => {
        res.isDefined shouldEqual true
        res.get.int shouldEqual (row.int + 15)
      }
    }
  }

  it should "prioritise batch updates based on a timestamp with Twitter futures" in {
    val row = JodaRow.sample

    val last = new DateTime()
    val last2 = last.withDurationAdded(20, 5)

    val statement1 = PrimitivesJoda.insert
      .value(_.pkey, row.pkey)
      .value(_.intColumn, row.int)
      .value(_.timestamp, row.bi)

    val batch = BatchStatement()
      .add(statement1)
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo row.int))
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 10)).timestamp(last2.getMillis))
      .add(PrimitivesJoda.update.where(_.pkey eqs row.pkey).modify(_.intColumn setTo (row.int + 15)))

    val chain = for {
      done <- batch.execute()
      updated <- PrimitivesJoda.select.where(_.pkey eqs row.pkey).get()
    } yield updated

    chain.successful {
      res => {
        res.isDefined shouldEqual true
        res.get.int shouldEqual (row.int + 15)
      }
    }
  }

}
