organization in ThisBuild := "org.jz"
version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.13.1"

val macwire   = "com.softwaremill.macwire" %% "macros"    % "2.3.3" % "provided"
val scalaTest = "org.scalatest"            %% "scalatest" % "3.1.1" % Test

lazy val `ticket-service` = (project in file("."))
  .aggregate(`event-service-api`, `event-service-impl`, `user-service-api`, `user-service-impl`)

lazy val `user-service-api` = (project in file("user-service-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `user-service-impl` = (project in file("user-service-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`user-service-api`)

lazy val `event-service-api` = (project in file("event-service-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )
  .dependsOn(`user-service-api`)

lazy val `event-service-impl` = (project in file("event-service-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`event-service-api`)
