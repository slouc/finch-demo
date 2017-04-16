name := "FinchDemo"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-http" % "6.43.0",
  "com.twitter" %% "finagle-stats" % "6.43.0",
  "com.twitter" %% "twitter-server" % "1.28.0",
  "com.github.finagle" %% "finch-core" % "0.14.0",
  "com.github.finagle" %% "finch-circe" % "0.14.0",
  "io.circe" %% "circe-core" % "0.7.1",
  "io.circe" %% "circe-generic" % "0.7.1"
)