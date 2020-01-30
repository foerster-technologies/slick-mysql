package com.foerstertechnologies.slickmysql

import slick.jdbc.{JdbcProfile, JdbcType, MySQLProfile, PositionedResult}

import scala.reflect.classTag

trait MySQLPlayJsonSupport extends json.MySQLJsonExtension with utils.MySQLCommonJdbcTypes { self: MySQLProfile =>

  import self.api._
  import play.api.libs.json._

  ///---
  val json: String = "json"
  ///---

  trait PlayJsonCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (self.isInstanceOf[ExMySQLProfile]) {
      self.asInstanceOf[ExMySQLProfile].bindMySQLTypeToScala("json", classTag[JsValue])
      self.asInstanceOf[ExMySQLProfile].bindMySQLTypeToScala("jsonb", classTag[JsValue])
    }
  }

  trait PlayJsonImplicits extends PlayJsonCodeGenSupport {
    implicit val playJsonTypeMapper: JdbcType[JsValue] =
      new GenericJdbcType[JsValue](
        json,
        (v) => Json.parse(v),
        (v) => Json.stringify(v).replace("\\u0000", ""),
        java.sql.Types.LONGVARCHAR,
        hasLiteralForm = false
      )

    implicit def playJsonColumnExtensionMethods(c: Rep[JsValue]) = {
        new JsonColumnExtensionMethods[JsValue, JsValue](c)
      }
    implicit def playJsonOptionColumnExtensionMethods(c: Rep[Option[JsValue]]) = {
        new JsonColumnExtensionMethods[JsValue, Option[JsValue]](c)
      }
  }

  trait PlayJsonPlainImplicits extends PlayJsonCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class MySQLJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JsNull)
      def nextJsonOption() = r.nextStringOption().map(Json.parse)
    }

    ////////////////////////////////////////////////////////////
    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[JsValue](json, Json.stringify)
    implicit val setJsonOption = mkOptionSetParameter[JsValue](json, Json.stringify)
  }
}
