organization := "co.fs2"
name         := "fs2-chat"

scalaVersion := "3.7.1"

scalacOptions += "-deprecation"

val kyoVersion = "1.0-RC1"

libraryDependencies ++= Seq(
  "co.fs2"       %% "fs2-io"             % "3.13.0-M2",
  "co.fs2"       %% "fs2-scodec"         % "3.13.0-M2",
  "org.slf4j"     % "slf4j-simple"       % "2.0.13",
  "org.jline"     % "jline"              % "3.26.1",
  "com.monovore" %% "decline"            % "2.4.1",
  //basic Kyo
  "io.getkyo"    %% "kyo-core"           % kyoVersion,
  "io.getkyo"    %% "kyo-direct"         % kyoVersion,
  "io.getkyo"    %% "kyo-combinators"    % kyoVersion,
  //integration with Cats
  "io.getkyo"    %% "kyo-cats"           % kyoVersion,
  "io.getkyo"    %% "kyo-scheduler-cats" % kyoVersion
)

run / fork         := true
outputStrategy     := Some(StdoutOutput)
run / connectInput := true

scalafmtOnCompile := true

enablePlugins(UniversalPlugin, JavaAppPackaging)
