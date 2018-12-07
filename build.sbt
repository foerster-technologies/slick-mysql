lazy val commonSettings = Seq(
  organizationName := "foerster technologies",
  organization := "com.foerster-technologies",
  name := "slick-mysql",
  version := "0.0.1-SNAPSHOT",

  scalaVersion := "2.12.7",
  crossScalaVersions := Seq("2.12.7", "2.11.12"),
  scalacOptions ++= Seq("-deprecation",
    "-feature",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials"),

  resolvers += Resolver.mavenLocal,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  resolvers += "spray" at "http://repo.spray.io/",
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  makePomConfiguration ~= { _.copy(configurations = Some(Seq(Compile, Runtime, Optional))) },
  pomExtra :=
    <url>https://github.com/foerster-technologies/slick-mysql</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://www.opensource.org/licenses/bsd-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:foerster-technologies/slick-mysql.git</url>
        <connection>scm:git:git@github.com:foerster-technologies/slick-mysql.git</connection>
      </scm>
      <developers>
        <developer>
          <id>TimFoerster</id>
          <name>Tim Förster</name>
          <timezone>+1</timezone>
        </developer>
      </developers>
)

def mainDependencies(scalaVersion: String) = {
  val extractedLibs = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0" % "provided")
    case _ =>
      Seq()
  }
  Seq (
    "org.scala-lang" % "scala-reflect" % scalaVersion,
    "com.typesafe.slick" %% "slick" % "3.2.3",
    "org.slf4j" % "slf4j-simple" % "1.7.24" % "provided",
    "org.scalatest" %% "scalatest" % "3.0.4" % "test"
  ) ++ extractedLibs
}

lazy val slickMySQLCore = Project(id = "slick-mysql_core", base = file("./core"),
  settings = Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
    name := "slick-mysql_core",
    description := "Slick extensions for MySQL - Core",
    libraryDependencies := mainDependencies(scalaVersion.value)
  )
)

lazy val slickMySQLProject = Project(id = "slick-mysql", base = file("."),
  settings = Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
    name := "slick-mysql",
    description := "Slick extensions for MySQL",
    libraryDependencies := mainDependencies(scalaVersion.value)
  )
).dependsOn (slickMySQLCore)
  .aggregate (slickMySQLCore, slickMySQLJts)

lazy val slickMySQLJts = Project(id = "slick-mysql_jts", base = file("./addons/jts"),
  settings = Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
    name := "slick-mysql_jts",
    description := "Slick extensions for MySQL - jts module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "com.vividsolutions" % "jts-core" % "1.14.0"
    )
  )
).dependsOn (slickMySQLCore)
