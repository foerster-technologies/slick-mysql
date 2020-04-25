  package com.foerstertechnologies.slickmysql

import org.scalatest.FunSuite
import slick.jdbc.{GetResult, MySQLProfile, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext}
import java.util.concurrent.Executors

class MySQLJodaMoneySuite extends FunSuite {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  trait MyMySQLProfile extends MySQLProfile with MySQLJodaMoneySupport {

    override val api: API = new API {}

    trait API extends super.API with JodaMoneyImplicits
  }

  object MyMySQLProfile extends MyMySQLProfile

  import MyMySQLProfile.api._

  test("Profile builds") {
    val db = Database.forURL("localhost", driver = "com.mysql.cj.jdbc.Driver")
  }
}
