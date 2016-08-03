name := "pogo"

version := "1.0"

scalaVersion := "2.11.8"

import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

PB.protobufSettings

PB.runProtoc in PB.protobufConfig := (args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray))

scalaSource in PB.protobufConfig := sourceManaged.value

libraryDependencies ++= {
  val akkaV       = "2.4.8"
  val scalaTestV  = "2.2.6"

  Seq(
    "org.isuper"              %  "s2-geometry-library-java"             % "0.0.1",
    "com.typesafe.akka"       %% "akka-actor"                           % akkaV,
    "com.typesafe.akka"       %% "akka-stream"                          % akkaV,
    "com.typesafe.akka"       %% "akka-http-core"                       % akkaV,
    "com.typesafe.akka"       %% "akka-http-experimental"               % akkaV,
    "com.typesafe.akka"       %% "akka-http-spray-json-experimental"    % akkaV,
    "com.typesafe.akka"       %% "akka-http-testkit"                    % akkaV,
    "com.typesafe"            % "config"                                % "1.3.0",
    "org.scalatest"           %% "scalatest"                            % scalaTestV % "test"
  )
}
