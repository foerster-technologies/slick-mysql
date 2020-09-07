package com.foerstertechnologies.slickmysql

import java.nio.ByteBuffer

import org.locationtech.jts.geom.{Geometry, GeometryFactory, Point, PrecisionModel}
import org.locationtech.jts.io.{ByteOrderValues, WKBReader, WKBWriter, WKTReader, WKTWriter}


object MySQLSpatialSupportUtils {
  private val wktWriterHolder = new ThreadLocal[WKTWriter]
  private val wktReaderHolder = new ThreadLocal[WKTReader]
  private val wkbWriterHolder = new ThreadLocal[WKBWriter]
  private val wkb3DWriterHolder = new ThreadLocal[WKBWriter]
  private val wkbReaderHolder = new ThreadLocal[WKBReader]

  def toLiteral(geom: Geometry): String = {
    if (wktWriterHolder.get == null) wktWriterHolder.set(new WKTWriter())
    val lit = wktWriterHolder.get.write(geom)
    lit
  }

  def fromLiteral[T](value: String): T = {

    splitRSIDAndWKT(value) match {
      case (srid, wkt) =>
        val geom =
          if (wkt.startsWith("00") || wkt.startsWith("01")) {
            if (wkbReaderHolder.get == null) wkbReaderHolder.set(new WKBReader())
            fromBytes(WKBReader.hexToBytes(wkt))
          } else {
            if (wktReaderHolder.get == null) wktReaderHolder.set(new WKTReader())

            wktReaderHolder.get.read(wkt)
          }

        if (srid != -1) geom.setSRID(srid)
        geom.asInstanceOf[T]

    }
  }

  def geometryFromWkb[T](wkbReader: ThreadLocal[WKBReader], bytes: Array[Byte]): T = {
    wkbReader.get.read(bytes).asInstanceOf[T]
  }

  def fromBytes[T](bytes: Array[Byte]): T = {
    // MySQL stores geometry values using 4 bytes to indicate the SRID followed by the WKB representation of the value.
    // https://dev.mysql.com/doc/refman/8.0/en/storage-requirements.html
    if (wkbReaderHolder.get == null) wkbReaderHolder.set(new WKBReader(new GeometryFactory(new PrecisionModel, ByteBuffer.wrap(bytes.take(4)).getInt)))
    geometryFromWkb(wkbReaderHolder, bytes.drop(4))
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

  def toBytes[T <: Geometry](geom: T): Array[Byte] = {

    var writer: WKBWriter = null

    if (geom != null && geom.getCoordinate != null && !java.lang.Double.isNaN(geom.getCoordinate.getZ)) {
      if (wkb3DWriterHolder.get == null) wkb3DWriterHolder.set(new WKBWriter(3, ByteOrderValues.LITTLE_ENDIAN, false))
      writer = wkbWriterHolder.get
    } else {
      if (wkbWriterHolder.get == null) wkbWriterHolder.set(new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN, false))
      writer = wkbWriterHolder.get
    }

    val wkbValue = writer.write(geom)

    val srid = geom.getSRID
    val sridByte = Array(srid.toByte, (srid >>> 8).toByte, (srid >>> 16).toByte, (srid >>> 24).toByte)

    // MySQL stores the srid with the wkb value in little endian.
    // Enabling the WKBWriter with `inclineSrid` resolves in incorrect data.
    sridByte ++ wkbValue
  }

  def toWkt(geom: Geometry): String = {
    new WKTWriter().write(geom)
  }

  /**
    * Helper method for debugging porpuse
    * @param bytes array that will be printed
    * @return bytes as hex representation
    */
  def convertBytesToHex(bytes: Seq[Byte]): String = {
    val sb = new StringBuilder
    for (b <- bytes) {
      sb.append(String.format("%02x", Byte.box(b)))
    }
    sb.toString
  }
}
