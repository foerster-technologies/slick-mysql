package com.foerstertechnologies.slickmysql

import java.sql.{PreparedStatement, ResultSet}

import com.foerstertechnologies.slickmysql.spatial._
import org.locationtech.jts.geom._
import slick.ast.FieldSymbol
import slick.jdbc._

import scala.reflect.{ClassTag, classTag}

trait MySQLSpatialSupport extends MySQLSpatialExtension {
  driver: MySQLProfile =>

  import driver.api._

  trait MySQLSpatialCodeGenSupport {
    // register types to let `ExMMySQLBuilder` find them
    if (driver.isInstanceOf[ExMySQLProfile]) {
      driver.asInstanceOf[ExMySQLProfile].bindMySQLTypeToScala("geometry", classTag[Geometry])
    }
  }

  ///
  trait MySQLSpatialAssistants extends BaseMySQLSpatialAssistence[Geometry, Point, LineString, Polygon, GeometryCollection]

  trait MySQLSpatialImplicits extends MySQLSpatialCodeGenSupport {
    implicit val geometryTypeMapper: JdbcType[Geometry] = new GeometryJdbcType[Geometry]
    implicit val pointTypeMapper: JdbcType[Point] = new GeometryJdbcType[Point]
    implicit val polygonTypeMapper: JdbcType[Polygon] = new GeometryJdbcType[Polygon]
    implicit val lineStringTypeMapper: JdbcType[LineString] = new GeometryJdbcType[LineString]
    implicit val linearRingTypeMapper: JdbcType[LinearRing] = new GeometryJdbcType[LinearRing]
    implicit val geometryCollectionTypeMapper: JdbcType[GeometryCollection] = new GeometryJdbcType[GeometryCollection]
    implicit val multiPointTypeMapper: JdbcType[MultiPoint] = new GeometryJdbcType[MultiPoint]
    implicit val multiPolygonTypeMapper: JdbcType[MultiPolygon] = new GeometryJdbcType[MultiPolygon]
    implicit val multiLineStringTypeMapper: JdbcType[MultiLineString] = new GeometryJdbcType[MultiLineString]

    ///
    implicit def geometryColumnExtensionMethods[G1 <: Geometry](c: Rep[G1]) =
      new GeometryColumnExtensionMethods[Geometry, Point, LineString, Polygon, GeometryCollection, G1, G1](c)

    implicit def geometryOptionColumnExtensionMethods[G1 <: Geometry](c: Rep[Option[G1]]) =
      new GeometryColumnExtensionMethods[Geometry, Point, LineString, Polygon, GeometryCollection, G1, Option[G1]](c)
  }

  trait MySQLSpatialPlainImplicits extends MySQLSpatialCodeGenSupport {

    import MySQLSpatialSupportUtils._
    import utils.PlainSQLUtils._

    implicit class SpatialPositionedResult(r: PositionedResult) {
      def nextGeometry[T <: Geometry](): T = nextGeometryOption().getOrElse(null.asInstanceOf[T])

      def nextGeometryOption[T <: Geometry](): Option[T] = r.nextStringOption().map(fromLiteral[T])
    }

    ////////////////////////////////////////////////////////////////////////////////
    implicit val getGeometry = mkGetResult(_.nextGeometry[Geometry]())
    implicit val getGeometryOption = mkGetResult(_.nextGeometryOption[Geometry]())

    implicit object SetGeometry extends SetParameter[Geometry] {
      def apply(v: Geometry, pp: PositionedParameters) = setGeometry(Option(v), pp)
    }

    implicit object SetGeometryOption extends SetParameter[Option[Geometry]] {
      def apply(v: Option[Geometry], pp: PositionedParameters) = setGeometry(v, pp)
    }

    ///
    private def setGeometry[T <: Geometry](maybeGeo: Option[T], p: PositionedParameters) = {
      maybeGeo match {
        case Some(v) => p.setBytes(toBytes(v))
        case None => p.setNull(java.sql.Types.OTHER)
      }
    }
  }

  //////////////////////// geometry jdbc type ///////////
  class GeometryJdbcType[T <: Geometry](implicit override val classTag: ClassTag[T]) extends DriverJdbcType[T] {

    import MySQLSpatialSupportUtils._

    override def sqlType: Int = java.sql.Types.OTHER

    override def sqlTypeName(sym: Option[FieldSymbol]): String = "geometry"

    override def getValue(r: ResultSet, idx: Int): T = {

      val geoInWkb = r.getBytes(idx)
      if (r.wasNull()) null.asInstanceOf[T]
      else fromBytes(geoInWkb)
    }

    override def setValue(v: T, p: PreparedStatement, idx: Int): Unit = {
      p.setBytes(idx,toBytes(v))
    }

    override def updateValue(v: T, r: ResultSet, idx: Int): Unit = {
      r.updateBytes(idx, toBytes(v))
    }

    override def hasLiteralForm: Boolean = true

    override def valueToSQLLiteral(v: T) = {
      if (v eq null) "NULL" else s"'${toLiteral(v)}'"

    }
  }

}
