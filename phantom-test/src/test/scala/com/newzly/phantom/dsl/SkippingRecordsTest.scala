
package com.newzly.phantom.dsl

import scala.concurrent.blocking
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.SpanSugar._
import com.newzly.phantom.Implicits._
import com.newzly.phantom.tables.{ Article, Articles }
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.newzly.util.testing.cassandra.BaseTest

class SkippingRecordsTest extends BaseTest {
  val keySpace: String = "SkippingRecordsTest"
  implicit val s: PatienceConfiguration.Timeout = timeout(20 seconds)

  override def beforeAll(): Unit = {
    blocking {
      super.beforeAll()
      Articles.insertSchema()
    }
  }

  it should "allow skipping records " in {
    val article1 = Article.sample
    val article2 = article1.copy(order_id = article1.order_id + 1)
    val article3 = article1.copy(order_id = article1.order_id + 2)

    val result = for {
      i1 <- Articles.insert
        .value(_.name, article1.name).value(_.id, article1.id)
        .value(_.order_id, article1.order_id)
        .future()
      i2 <- Articles.insert
        .value(_.name, article2.name)
        .value(_.id, article2.id)
        .value(_.order_id, article2.order_id)
        .future()
      i3 <- Articles.insert
        .value(_.name, article3.name)
        .value(_.id, article3.id)
        .value(_.order_id, article3.order_id)
        .future()
      all <- Articles.select.fetch
      res <- Articles.select.where(_.id eqs  article1.id ).skip(article1.order_id).one
    } yield (all.size, res)

    result successful {
      r => {
        val allSize = r._1
        val row = r._2

        allSize shouldEqual 3
        row.isDefined shouldEqual true
        row.get shouldEqual article2
      }
    }
  }

}

