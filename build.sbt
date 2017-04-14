name := """broadtranslator"""
organization := "org.broadinstitute"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.10"

libraryDependencies += filters
libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/org.eclipse.rdf4j/rdf4j-rio-jsonld
  "org.eclipse.rdf4j" % "rdf4j-repository-sail" % "2.2",
  "org.eclipse.rdf4j" % "rdf4j-sail-memory" % "2.2",
  "org.eclipse.rdf4j" % "rdf4j-rio-jsonld" % "2.2",
  "com.github.jsonld-java" % "jsonld-java" % "0.8.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "org.broadinstitute.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "org.broadinstitute.binders._"
