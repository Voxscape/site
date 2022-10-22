addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")
// addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.2")

// packaging
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.11")

libraryDependencies += "org.postgresql" % "postgresql" % "42.3.6"

addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "3.5.0")

//addSbtPlugin("io.github.davidmweber" % "flyway-sbt" % "7.4.0")

addDependencyTreePlugin

dependencyOverrides ++= Seq(
  "org.scala-lang.modules" %% "scala-xml_2.12" % "2.1.0",
)
