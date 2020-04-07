import com.lightbend.lagom.core.LagomVersion

organization in ThisBuild := "org.jz"
version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.13.1"

val macwire                    = "com.softwaremill.macwire"     %% "macros"                                        % "2.3.3" % "provided"
val scalaTest                  = "org.scalatest"                %% "scalatest"                                     % "3.1.1" % Test
val postgresDriver             = "org.postgresql"               % "postgresql"                                     % "42.2.8"
val akkaDiscoveryKubernetesApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api"                 % "1.0.1"
val lagomScaladslAkkaDiscovery = "com.lightbend.lagom"          %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current

ThisBuild / scalacOptions ++= List("-encoding", "utf8", "-deprecation", "-feature", "-unchecked", "-Xfatal-warnings")

def dockerSettings = Seq(
  dockerUpdateLatest := true,
  dockerBaseImage := "adoptopenjdk/openjdk8",
  dockerUsername := sys.props.get("docker.username"),
  dockerRepository := sys.props.get("docker.registry")
)

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
      akkaDiscoveryKubernetesApi,
      lagomScaladslAkkaDiscovery,
      lagomScaladslKafkaBroker,
      lagomScaladslPersistenceJdbc,
      lagomScaladslTestKit,
      macwire,
      postgresDriver,
      scalaTest,
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
      akkaDiscoveryKubernetesApi,
      lagomScaladslAkkaDiscovery,
      lagomScaladslKafkaBroker,
      lagomScaladslPersistenceJdbc,
      lagomScaladslTestKit,
      macwire,
      postgresDriver,
      scalaTest,
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`event-service-api`, `user-service-api`)

lagomCassandraEnabled in ThisBuild := false
lagomKafkaEnabled in ThisBuild := false
lagomKafkaPort in ThisBuild := 9092
