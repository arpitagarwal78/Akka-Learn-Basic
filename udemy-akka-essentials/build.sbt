name := "udemy-akka-essentials"

version := "0.1"

scalaVersion := "2.12.7"

// come here after creating project in intellij -> new project -> scala -> sbt -> name
// then put akka version u want to use

// define a akkaVersion variable
val akkaVersion = "2.5.13"

// library dependency is predefined build.sbt variable to tell what dependency to include
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion, // this is for akka actor
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion, // this is for akka testing
  "org.scalatest" %% "scalatest" % "3.0.5" // this is normal scala test lib
)

// click import changes down in intellij once they are included you can use the akka libraries :)
// goto src -> scala package and create a new package playground and we will create first application
// create a playground object that will help us to actually run application