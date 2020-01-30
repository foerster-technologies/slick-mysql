package com.foerstertechnologies.slickmysql

import java.math.RoundingMode
import java.sql.{PreparedStatement, ResultSet}

import org.joda.money.{CurrencyUnit, Money}
import slick.ast.NumericTypedType
import slick.jdbc._

import scala.reflect.{ClassTag, classTag}

trait MySQLJodaMoneySupport extends money.MySQLMoneyExtension with utils.MySQLCommonJdbcTypes { self: MySQLProfile =>

  import self.api._

  val defaultCurrencyUnit: CurrencyUnit = CurrencyUnit.EUR

  trait JodaMoneyCodegenSupport {
    // register types to let `ExMMySQLBuilder` find them
    if (self.isInstanceOf[ExMySQLProfile]) {
      self.asInstanceOf[ExMySQLProfile].bindMySQLTypeToScala("money", classTag[Money])
    }
  }

  class JodaMoneyJdbcType extends DriverJdbcType[Money] with NumericTypedType {
    def sqlType = java.sql.Types.DECIMAL
    def setValue(v: Money, p: PreparedStatement, idx: Int) = p.setBigDecimal(idx, v.getAmount)
    def getValue(r: ResultSet, idx: Int) = {
      val v = r.getBigDecimal(idx)
      if(v eq null) null else Money.of(defaultCurrencyUnit, BigDecimal(v).bigDecimal, RoundingMode.UNNECESSARY)
    }
    def updateValue(v: Money, r: ResultSet, idx: Int) = r.updateBigDecimal(idx, v.getAmount)
  }

  trait JodaMoneyImplicits extends JodaMoneyCodegenSupport {

    implicit val jodaMoneyJdbType = new JodaMoneyJdbcType

    implicit def playJsonColumnExtensionMethods(c: Rep[Money]) = {
      new MoneyColumnExtensionMethods[Money, Money](c)
    }
    implicit def playJsonOptionColumnExtensionMethods(c: Rep[Option[Money]]) = {
      new MoneyColumnExtensionMethods[Money, Option[Money]](c)
    }
  }

  trait JodaMoneyPlainImplicits extends JodaMoneyCodegenSupport {

    private def stringToJodaMoney(v: String): Money =
      Money.of(defaultCurrencyUnit, BigDecimal(v).bigDecimal, RoundingMode.UNNECESSARY)

    private def jodaMoneyToString(money: Money): String = {
      println("writing", money.getAmount.toString)
      money.getAmount.toString
    }

    import utils.PlainSQLUtils._

    implicit class MySQLJodaMoneyPositionedResult(r: PositionedResult) {
      def nextMoney() = nextMoneyOption().orNull
      def nextMoneyOption() = r.nextStringOption().map(stringToJodaMoney)
    }

    // Accessors
    implicit val getMoney: GetResult[Money] = mkGetResult(_.nextMoney())
    implicit val getMoneyOption: GetResult[Option[Money]] = mkGetResult(_.nextMoneyOption())
    implicit val setMoney = mkSetParameter[Money]("money", jodaMoneyToString, java.sql.Types.DECIMAL)
    implicit val setMoneyOption = mkOptionSetParameter[Money]("money", jodaMoneyToString, java.sql.Types.DECIMAL)
  }


}
