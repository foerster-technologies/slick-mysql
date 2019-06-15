package com.foerstertechnologies.slickmysql

trait MySQLSpatialProfile extends ExMySQLProfile
  with MySQLSpatialSupport {

  override val api = new MyAPI {}

  trait MyAPI extends API
    with MySQLSpatialImplicits with MySQLSpatialAssistants
}

object MySQLSpatialProfile extends MySQLSpatialProfile
