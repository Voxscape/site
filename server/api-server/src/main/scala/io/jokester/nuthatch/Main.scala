package io.jokester.nuthatch

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.{ipv4, port}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import io.jokester.api.OpenAPIBuilder
import io.jokester.cats_effect.TerminateCondition
import io.jokester.http4s.VerboseLogger
import io.jokester.nuthatch.base.ApiBinder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.{HttpApp, HttpRoutes}

import java.nio.file.{Files, Path}

object Main extends IOApp with LazyLogging {
  val config: Config = ConfigFactory.load()

  private def buildApp(): AppRoot = AppRoot.build(ConfigFactory.load())

  def runServer(appRoot: AppRoot): IO[ExitCode] = {
    val apiRoutes: HttpRoutes[IO] =
      ApiBinder.buildRoutes(appRoot).tapWith(VerboseLogger.logReqRes[IO])

    val httpApp: HttpApp[IO] =
      Router[IO]("/api/nuthatch_v1" -> apiRoutes, "/" -> VerboseLogger.notFound).orNotFound

    val apiServer: Resource[IO, Server] = EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build

    val r = appRoot.apiContext.redis.use(jedis => IO.blocking(jedis.info()));

    for (
      redisInfo1 <- IO.race(r, r);
      serverPair <- apiServer.allocated;
      _          <- IO.race(TerminateCondition.enterPressed, TerminateCondition.sigTerm);
      _          <- serverPair._2;
      _          <- IO { logger.info("{} stopped", serverPair._1) }
    ) yield ExitCode.Success
  }

  def shutdown(serviceBundle: AppRoot): IO[Unit] = {
    for (
      _ <- serviceBundle.apiContext.close();
      _ <- IO { logger.info("shutting down") }
    ) yield ()
  }

  def testDeps(serviceBundle: AppRoot): IO[ExitCode] = {
    for (
      redisInfo <- serviceBundle.apiContext.redis.use(jedis => IO.blocking(jedis.info()));
      _         <- serviceBundle.apiContext.quill.testConnection();
      _         <- IO.print(redisInfo.linesIterator.take(5).toSeq)
    ) yield ExitCode.Success
  }

  def exportApiSpec(destFilename: String): IO[ExitCode] = IO {
    logger.info("Exporting OpenAPI spec to {}", destFilename)
    Files.writeString(
      Path.of(destFilename),
      OpenAPIBuilder.buildOpenApiYaml(ApiBinder.apiList, "nuthatch", "0.1"),
    )
    ExitCode.Success
  }

  def run(args: List[String]): IO[ExitCode] = {
    args match {
      case List() =>
        val serviceBundle = buildApp()
        testDeps(serviceBundle) <* shutdown(serviceBundle)
      case List("writeOpenApiSpec", dest) =>
        exportApiSpec(dest)
      case List("runServer") =>
        val serviceBundle = buildApp()
        runServer(serviceBundle) <* shutdown(serviceBundle)
      case "runScript" :: rest =>
        val serviceBundle = buildApp()
        new Scripts(serviceBundle).runScript(rest) <* shutdown(serviceBundle)
      case _ =>
        IO.println(s"command not recognized: $args").map(_ => ExitCode.Error)
    }
  }
}
