name := """blank-slate-play-authentication"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  javaJpa,
  cache,
  javaWs
)

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "bootstrap" % "4.0.0-alpha.5",
  "org.webjars" % "jquery" % "3.1.1",
  "org.webjars" % "font-awesome" % "4.6.3",
  "com.adrianhurt" %% "play-bootstrap" % "1.1-P25-B4" exclude("org.webjars", "jquery"),
  "org.webjars.npm" % "tether" % "1.3.7" //tether is now required for bootstrap 4
)
lazy val akkaVersion = "2.4.11"
libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test

// https://mvnrepository.com/artifact/org.mockito/mockito-core
libraryDependencies += "org.mockito" % "mockito-core" % "2.2.17"


// https://mvnrepository.com/artifact/dom4j/dom4j
libraryDependencies += "dom4j" % "dom4j" % "1.6"
// https://mvnrepository.com/artifact/org.hibernate/hibernate-core
// must exclude dom4j in hibernate core because it causes staxeventreader exceptions
// http://stackoverflow.com/questions/36222306/caused-by-java-lang-classnotfoundexception-org-dom4j-io-staxeventreader
libraryDependencies += "org.hibernate" % "hibernate-core" % "5.2.3.Final" exclude("dom4j", "dom4j") exclude("javax.transaction", "jta") exclude("org.slf4j", "slf4j-api")

// https://mvnrepository.com/artifact/org.postgresql/postgresql
libraryDependencies += "org.postgresql" % "postgresql" % "9.4-1206-jdbc42"

LessKeys.compress := true
// This is a required fix for JPA/play bug in prod
// See https://github.com/playframework/playframework/issues/4590#issuecomment-108409625
PlayKeys.externalizeResources := false