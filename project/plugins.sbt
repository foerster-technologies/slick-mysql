// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Add sbt PGP Plugin
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")
// addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
// addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")
