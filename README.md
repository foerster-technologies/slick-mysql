# slick-mysql
Slick extensions for MySQL8

This package is heavly based on 
[slick-pg](https://github.com/tminglei/slick-pg) which brings addional support for different data structures to slick.

Currently this package supports:

Json
 * [Circle Json](https://circe.github.io/circe/)
 * [Play Json](https://github.com/playframework/play-json)

Spatial
 * [Spatial Data Types](https://dev.mysql.com/doc/refman/8.0/en/spatial-types.html) features that came along with MySQL 8.
 * [JTS](https://github.com/locationtech/jts)

Money
* [Joda Money](https://www.joda.org/joda-money/)

# Spatial extension
This package is working with `org.locationtech.jts » jts-core` instead of `com.vividsolutions » jts-core` 

## Installation

Play 2.8
```
libraryDependencies += "com.foerster-technologies" %% "slick-mysql" % "1.0.0"
```

### Addons

> Circle Json (version 0.13.0)
```
libraryDependencies += "com.foerster-technologies" %% "slick-circle-json" % "1.0.0"
```

> JTS Support
```
libraryDependencies += "com.foerster-technologies" %% "slick-mysql_jts" % "1.0.0"
```

> Joda Money
```
libraryDependencies += "com.foerster-technologies" %% "slick-mysql_joda-money" % "1.0.0"
```
is still under development and very minimal.
* Has a default currency (EUR)
* No currency field supported, only amount

> JTS Support
```
libraryDependencies += "com.foerster-technologies" %% "slick-mysql_jts" % "1.0.0"
```

> Play Json
```
libraryDependencies += "com.foerster-technologies" %% "slick-mysql_play-json" % "1.0.0"
```

## Usage

### Create your own Slick-Profile
this example includes the slick mysql profile with the spatial extension
```
package your.package

import com.foerstertechnologies.slickmysql.{ExMySQLProfile, MySQLSpatialSupport}

trait MySQLSpatialProfile extends ExMySQLProfile
  with MySQLSpatialSupport {

  override val api = new MyAPI {}

  trait MyAPI extends API
    with MySQLSpatialImplicits with MySQLSpatialAssistants
}

object MySQLSpatialProfile extends MySQLSpatialProfile
```

You can also use `com.foerstertechnologies.slickmysql.MySQLSpatialProfile` which comes with the JTS package.
### Register your profile

For slick play: Set your own profile in your config
```
slick.dbs.default.profile = "your.package.MySQLSpatialProfile$"
```


### In your code

Gps example with JTS
```
import your.package.MySQLSpatialProfile
import play.api.db.slick.HasDatabaseConfigProvider
import org.locationtech.jts.geom.Point

case class Test(id : Long, gps : Point)

class TestTable(tag: Tag) extends Table[Test](tag, Some("xxx"), "Test") {
  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
  def gps = column[Point]("location")

  def * = (id, gps) <> (Test.tupled, Test.unapply)
}


trait MyTestClass extends HasDatabaseConfigProvider[MySQLSpatialProfile]
{
    import profile.api._
    
    def findAroundPoint(position: Point, limit : Int) : DBIO[Seq[Test] = {
        TestTable.sortBy(_.gps.distanceSphere(position)).take(limit).result
    }
}

```

## Documentation

### JTS
#### Stored SRID Values
The Geometry values are stored and loaded with the given SRID.   
Make sure to add SRID to your value when you save it.
Otherwise SRID 0 will be used as default value by mysql.
 For computations on multiple geometry values, all values must have the same SRID or an error occurs. 

## Credits
* [rleibman](https://github.com/rleibman) for adding circle-json support
