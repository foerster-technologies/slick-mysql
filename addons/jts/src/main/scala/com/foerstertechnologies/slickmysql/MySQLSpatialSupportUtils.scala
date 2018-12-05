package com.foerstertechnologies.slickmysql

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.{WKBReader, WKBWriter, WKTReader, WKTWriter}


object MySQLSpatialSupportUtils {
  private val wktWriterHolder = new ThreadLocal[WKTWriter]
  private val wktReaderHolder = new ThreadLocal[WKTReader]
  private val wkbWriterHolder = new ThreadLocal[WKBWriter]
  private val wkb3DWriterHolder = new ThreadLocal[WKBWriter]
  private val wkbReaderHolder = new ThreadLocal[WKBReader]

  def toLiteral(geom: Geometry): String = {
    if (wktWriterHolder.get == null) wktWriterHolder.set(new WKTWriter())
    wktWriterHolder.get.write(geom)
  }

  def fromLiteral[T](value: String): T = {
    if (wktReaderHolder.get == null) wktReaderHolder.set(new WKTReader())
    splitRSIDAndWKT(value) match {
      case (srid, wkt) => {
        val geom =
          if (wkt.startsWith("00") || wkt.startsWith("01"))
            fromBytes(WKBReader.hexToBytes(wkt))
          else wktReaderHolder.get.read(wkt)

        if (srid != -1) geom.setSRID(srid)
        geom.asInstanceOf[T]
      }
    }
  }

  def fromBytes[T](bytes: Array[Byte]): T = {
    if (wkbReaderHolder.get == null) wkbReaderHolder.set(new WKBReader())
    wkbReaderHolder.get.read(bytes).asInstanceOf[T]
  }

  private def splitRSIDAndWKT(value: String): (Int, String) = {
    if (value.startsWith("SRID=")) {
      val index = value.indexOf(';', 5) // srid prefix length is 5
      if (index == -1) {
        throw new java.sql.SQLException("Error parsing Geometry - SRID not delimited with ';' ")
      } else {
        val srid = Integer.parseInt(value.substring(0, index))
        val wkt = value.substring(index + 1)
        (srid, wkt)
      }
    } else (-1, value)
  }

  def toBytes(geom: Geometry): Array[Byte] = {
    if (geom != null && geom.getCoordinate != null && !(java.lang.Double.isNaN(geom.getCoordinate.z))) {
      if (wkb3DWriterHolder.get == null) wkb3DWriterHolder.set(new WKBWriter(3, true))
      wkb3DWriterHolder.get.write(geom)
    } else {
      if (wkbWriterHolder.get == null) wkbWriterHolder.set(new WKBWriter(2, true))
      wkbWriterHolder.get.write(geom)
    }
  }
}