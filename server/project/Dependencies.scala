import sbt.librarymanagement.{DependencyBuilders, InclExclRule}
import sbt._

object Dependencies {
  lazy val basicDeps: Seq[ModuleID] = Seq(
    // logging
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    "org.slf4j" % "jcl-over-slf4j" % "2.0.1",
    "org.slf4j" % "slf4j-api" % "2.0.1",
    "ch.qos.logback" % "logback-classic" % "1.4.1", // this provides SLF4J backend
    "commons-logging" % "commons-logging" % "1.2",
    // config
    "com.typesafe" % "config" % "1.4.2",
    "com.google.guava" % "guava" % "31.1-jre",
    //    "org.apache.commons" % "commons-lang3" % "3.12.0",
    "com.google.code.gson" % "gson" % "2.9.1",
    //    "org.yaml"                     % "snakeyaml" % "1.30",
  )

  lazy val twitterSdkDeps: Seq[ModuleID] = Seq(
    /** twitterd
      *
      * @see
      * {https://github.com/redouane59/twittered} newer (API v1+v2) but incomplete (API v2 itself
      * is incomplete too)
      */
    //    "io.github.redouane59.twitter" % "twittered" % "2.17" exclude ("org.yaml", "snakeyaml"),

    /** @see {https://twitter4j.org/en/index.html} a fine but API v1-only client */
    "org.twitter4j" % "twitter4j-core" % "4.1.0",

    /** @see
      * {https://github.com/twitterdev/twitter-api-java-sdk} twitter's API v2-only client. enables
      * app-only auth
      */
    //    "com.twitter" % "twitter-api-java-sdk" % "2.0.3",
  )

  lazy val http4sDeps: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-ember-server",
    "org.http4s" %% "http4s-ember-client",
    //    "org.http4s" %% "http4s-circe",
    //    "org.http4s" %% "http4s-dsl",
  ).map(_ % Versions.http4s)

  lazy val catsDeps: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % "2.8.0",
    "org.typelevel" %% "cats-effect" % "3.3.14",
  )

  lazy val circeDeps: Seq[ModuleID] = Seq(
    // circe
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
  ).map(_ % Versions.circe)

  lazy val authDeps: Seq[ModuleID] = Seq(
    "org.springframework.security" % "spring-security-crypto" % "5.7.3",
    "com.github.scribejava" % "scribejava-apis" % "8.3.1",
    "com.github.jwt-scala" %% "jwt-circe" % "9.1.1",
  )

  lazy val redisDeps: Seq[ModuleID] = Seq(
    "redis.clients" % "jedis" % "4.2.3",
  )

  lazy val oauthDeps: Seq[ModuleID] = Seq(
  )

  lazy val tapirDeps: Seq[ModuleID] = Seq(
    // tapir as server
    "com.softwaremill.sttp.tapir" %% "tapir-core",
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs",
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle",
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server",
  ).map(_ % Versions.tapir)

  lazy val quillDeps: Seq[ModuleID] = Seq(
    "com.zaxxer" % "HikariCP" % "5.0.1",
    "org.postgresql" % "postgresql" % Versions.postgresql,
    "io.getquill" %% "quill-jdbc" % Versions.quill,
    //    "io.getquill"   %% "quill-async-postgres" % Versions.quill,
  )

  lazy val quill4Deps: Seq[ModuleID] = Seq(
    "org.postgresql" % "postgresql" % Versions.postgresql,
    "io.getquill" %% "quill-jdbc" % "4.6.0",
  )

  lazy val quillCodegenDeps: Seq[ModuleID] = Seq(
    "org.postgresql" % "postgresql" % Versions.postgresql,
    //    "io.getquill"   %% "quill-jdbc"           % Versions.quill,
    ("io.getquill" %% "quill-codegen-jdbc" % "4.6.0"),
  )

  lazy val testDeps: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.github.javafaker" % "javafaker" % "1.0.2",
  ).map(_ % Test)

  lazy val incompatibleDependencies: Seq[DependencyBuilders.OrganizationArtifactName] = Seq(
    //    "com.typesafe.scala-logging" % "scala-logging_2.13",
    //    "org.yaml"                     % "snakeyaml",
  )
}

private object Versions {
  val circe = "0.14.2"
  val circeYaml = "0.13.1"

  val tapir = "1.0.4" // FIXME: use released version

  val akkaHttp = "10.2.9"
  val akka = "2.6.19"
  val swaggerUi = "3.35.2"
  val upickle = "1.2.2"
  val http4s = "0.23.14"

  // rdbms
  val postgresql = "42.5.0"
  val quill = "4.6.0"
  val scalikeJDBC = "3.5.0"
  val flyway = "8.0.2"

  // testing
  val scalaTest = "3.2.13"
  val scalaTestPlusScalaCheck = "3.2.2.0"
}
