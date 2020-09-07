lazy val scala212 = "2.12.8"
lazy val scala213 = "2.13.3"

lazy val commonSettings = Seq(
  organizationName := "foerster technologies",
  organization := "com.foerster-technologies",
  name := "slick-mysql",
  version := "1.1.0",

  scalaVersion := scala213,
  crossScalaVersions := List(scala212, scala213),
  scalacOptions ++= Seq("-deprecation",
    "-feature",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials"),

  resolvers += Resolver.mavenLocal,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  resolvers += "spray" at "https://repo.spray.io/",
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  // makePomConfiguration := makePomConfiguration.value. // ~= { _.(configurations = Some(Seq(Compile, Runtime, Optional))) },
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
        <email>tim@foerster-technologies.com</email>
        <organization>foerster technologies GmbH</organization>
        <organizationUrl>https://www.foerster-technologies.com</organizationUrl>
        <timezone>+1</timezone>
      </developer>
    </developers>
)

def mainDependencies(scalaVersion: String) = Seq (
  "org.scala-lang" % "scala-reflect" % scalaVersion,
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "org.slf4j" % "slf4j-simple" % "1.7.30" % "provided",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2" % "provided",
  "org.scalatest" %% "scalatest" % "3.1.1" % "test"
)

lazy val slickMySQLCore = (project in file("./core"))
  .settings(
    Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
      name := "slick-mysql_core",
      description := "Slick extensions for MySQL - Core",
      libraryDependencies := mainDependencies(scalaVersion.value)
    )
  )

lazy val slickMySQLProject = (project in file("."))
  .settings(
    Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
      name := "slick-mysql",
      description := "Slick extensions for MySQL",
      libraryDependencies := mainDependencies(scalaVersion.value)
    )
  ).dependsOn(slickMySQLCore)
  .aggregate(slickMySQLCore, slickMySQLJts, slickMySQLPlayJson, slickMySQLCirceJson, slickMySQLJodaMoney)

lazy val slickMySQLJts = (project in file("./addons/jts"))
  .settings(
    Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
      name := "slick-mysql_jts",
      description := "Slick extensions for MySQL - jts module",
      libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
        "org.locationtech.jts" % "jts-core" % "1.16.1"
      )
    )
  ).dependsOn(slickMySQLCore)

lazy val slickMySQLPlayJson = (project in file("./addons/play-json"))
  .settings(
      Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
        name := "slick-mysql_play-json",
        description := "Slick extensions for MySQL - play-json module",
        libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
          "com.typesafe.play" %% "play-json" % "2.8.1"
      )
    )
  ).dependsOn(slickMySQLCore)

lazy val circeVersion = "0.13.0"

lazy val slickMySQLCirceJson = (project in file("./addons/circe-json"))
  .settings(
    Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
      name := "slick-mysql_circe-json",
      description := "Slick extensions for MySQL - circe-json module",
      libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
        "io.circe" %% "circe-core",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-parser",
        "io.circe" %% "circe-literal"
      ).map(_ % circeVersion),
    )
  ).dependsOn(slickMySQLCore)

lazy val slickMySQLJodaMoney = (project in file("./addons/joda-money"))
  .settings(
    Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
      name := "slick-mysql_joda-money",
      description := "Slick extensions for MySQL - joda-money module",
      libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
        "org.joda" % "joda-money" % "1.0.1"
      )
    )
  ).dependsOn(slickMySQLCore)
