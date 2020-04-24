package com.foerstertechnologies.slickmysql

import slick.jdbc.{JdbcProfile, JdbcType, MySQLProfile, PositionedResult}

import scala.reflect.classTag
import java.sql.{PreparedStatement, ResultSet, SQLData, SQLInput, SQLOutput}

import io.circe.Json
import io.circe.parser._
import slick.jdbc.{GetResult, JdbcType, MySQLProfile, PositionedResult, SetParameter}

import scala.reflect.classTag

trait MySQLCirceJsonSupport extends json.MySQLJsonExtension with utils.MySQLCommonJdbcTypes {
  self: MySQLProfile =>

  import self.api._
  ///---
  val json: String = "json"
  ///---

  trait CirceJsonCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    self match {
      case profile1: ExMySQLProfile =>
        profile1.bindMySQLTypeToScala("json", classTag[Json])
        profile1.bindMySQLTypeToScala("jsonb", classTag[Json])
      case _ =>
    }
  }

  trait CirceJsonImplicits extends CirceJsonCodeGenSupport {
    implicit val circeJsonTypeMapper: JdbcType[Json] =
      new GenericJdbcType[Json](
        json,
        v => parse(v).toOption.getOrElse(Json.Null),
        v => v.noSpaces.replace("\\u0000", ""),
        java.sql.Types.LONGVARCHAR,
        hasLiteralForm = false
      )

    implicit def circeJsonColumnExtensionMethods(
                                                  c: Rep[Json]
                                                ): JsonColumnExtensionMethods[Json, Json] = {
      new JsonColumnExtensionMethods[Json, Json](c)
    }
    implicit def circeJsonOptionColumnExtensionMethods(
                                                        c: Rep[Option[Json]]
                                                      ): JsonColumnExtensionMethods[Json, Option[Json]] = {
      new JsonColumnExtensionMethods[Json, Option[Json]](c)
    }
  }

  trait CirceJsonPlainImplicits extends CirceJsonCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class MySQLJsonPositionedResult(r: PositionedResult) {
      def nextJson():       Json = nextJsonOption().getOrElse(Json.Null)
      def nextJsonOption(): Option[Json] = r.nextStringOption().flatMap(parse(_).toOption)
    }

    ////////////////////////////////////////////////////////////
    implicit val getJson: AnyRef with GetResult[Json] = mkGetResult(_.nextJson())
    implicit val getJsonOption: AnyRef with GetResult[Option[Json]] = mkGetResult(
      _.nextJsonOption()
    )
    implicit val setJson: SetParameter[Json] = mkSetParameter[Json](json, _.noSpaces)
    implicit val setJsonOption: SetParameter[Option[Json]] =
      mkOptionSetParameter[Json](json, _.noSpaces)
  }
}
