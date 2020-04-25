package com.foerstertechnologies.slickmysql

import org.scalatest.FunSuite
import slick.jdbc.{GetResult, MySQLProfile, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext}
import java.util.concurrent.Executors

class MySQLSpatialSupportSuite extends FunSuite {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  trait MyMySQLProfile extends MySQLProfile with MySQLSpatialSupport {

    override val api: API = new API {}
    val plainApi = new API with MySQLSpatialPlainImplicits

    trait API extends super.API with MySQLSpatialImplicits
  }

  object MyMySQLProfile extends MyMySQLProfile

  import MyMySQLProfile.api._

  test("Profile builds") {
    val db = Database.forURL("localhost", driver = "com.mysql.cj.jdbc.Driver")
  }
}
