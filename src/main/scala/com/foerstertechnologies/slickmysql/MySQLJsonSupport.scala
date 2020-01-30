package com.foerstertechnologies.slickmysql

import slick.jdbc.{JdbcProfile, JdbcType, MySQLProfile, PositionedResult}

import scala.reflect.classTag

/** simple json string wrapper */
case class JsonString(value: String)

/**
 * simple json support; if all you want is just getting from / saving to db, and using json operations/methods, it should be enough
 */
trait MySQLJsonSupport extends json.MySQLJsonExtension with utils.MySQLCommonJdbcTypes { self: MySQLProfile =>

  import self.api._

  ///---
  val json = "json"
  ///---

  trait SimpleJsonCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (self.isInstanceOf[ExMySQLProfile]) {
      self.asInstanceOf[ExMySQLProfile].bindMySQLTypeToScala("json", classTag[JsonString])
      self.asInstanceOf[ExMySQLProfile].bindMySQLTypeToScala("jsonb", classTag[JsonString])
    }
  }

  /// alias
  trait JsonImplicits extends SimpleJsonImplicits

  trait SimpleJsonImplicits extends SimpleJsonCodeGenSupport {
    implicit val simpleJsonTypeMapper: JdbcType[JsonString] =
      new GenericJdbcType[JsonString](
        json,
        (v) => JsonString(v),
        (v) => v.value,
        java.sql.Types.LONGVARCHAR,
        hasLiteralForm = false
      )

    implicit def simpleJsonColumnExtensionMethods(c: Rep[JsonString]) = {
        new JsonColumnExtensionMethods[JsonString, JsonString](c)
      }
    implicit def simpleJsonOptionColumnExtensionMethods(c: Rep[Option[JsonString]]) = {
        new JsonColumnExtensionMethods[JsonString, Option[JsonString]](c)
      }
  }

  trait SimpleJsonPlainImplicits extends SimpleJsonCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class MySQLJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().orNull
      def nextJsonOption() = r.nextStringOption().map(JsonString)
    }

    //////////////////////////////////////////////////////////////
    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[JsonString](json, _.value)
    implicit val setJsonOption = mkOptionSetParameter[JsonString](json, _.value)
  }
}
