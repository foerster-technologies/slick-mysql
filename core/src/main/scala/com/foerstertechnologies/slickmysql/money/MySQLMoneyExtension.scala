package com.foerstertechnologies.slickmysql.money

import slick.jdbc.{JdbcType, JdbcTypesComponent, MySQLProfile}
import slick.ast.{Library, LiteralNode, TypedType}
import slick.ast.Library.{SqlFunction, SqlOperator}
import slick.lifted.ExtensionMethods

trait MySQLMoneyExtension extends JdbcTypesComponent {
  self: MySQLProfile =>

  import self.api._

  object MoneyLibrary {
    val + = new SqlOperator("+")
    val - = new SqlOperator("-")
    val * = new SqlOperator("*")
    val / = new SqlOperator("/")
  }

  class MoneyColumnExtensionMethods[MoneyType, P1](val c: Rep[P1])(implicit tm: JdbcType[MoneyType]) extends ExtensionMethods[MoneyType, P1] {

    protected implicit def b1Type: TypedType[MoneyType] = implicitly[TypedType[MoneyType]]
  }
}
