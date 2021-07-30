lazy val akkaHttpVersion = "10.2.5"
lazy val akkaVersion    = "2.6.15"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "clever.cloud.com",
      scalaVersion    := "2.13.4"
    )),
    name := "prometheus-metrics",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",
      "fr.davit"          %% "akka-http-metrics-prometheus" % "1.6.0",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.1.4"         % Test,
    )
  )
