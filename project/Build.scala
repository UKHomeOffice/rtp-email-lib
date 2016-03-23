import sbt.Keys._
import sbt._

object Build extends Build {
  val moduleName = "rtp-email-lib"

  val root = Project(id = moduleName, base = file("."))
    .settings(
      name := moduleName,
      organization := "uk.gov.homeoffice",
      version := "1.0.2",
      scalaVersion := "2.11.8",
      scalacOptions ++= Seq(
        "-feature",
        "-language:implicitConversions",
        "-language:higherKinds",
        "-language:existentials",
        "-language:reflectiveCalls",
        "-language:postfixOps",
        "-Yrangepos",
        "-Yrepl-sync"
      ),
      ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
      resolvers ++= Seq(
        "Artifactory Snapshot Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-snapshot-local/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
        "Kamon Repository" at "http://repo.kamon.io"
      )
    )
    .settings(libraryDependencies ++= {
      val `rtp-io-lib-version` = "1.2.0-SNAPSHOT"
      val `rtp-test-lib-version` = "1.2.0-SNAPSHOT"
      val `rtp-mongo-lib-version` = "1.6.0-SNAPSHOT"

      Seq(
        "org.clapper" %% "grizzled-slf4j" % "1.0.2",
        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "joda-time" % "joda-time" % "2.5",
        "org.joda" % "joda-convert" % "1.7",
        "org.mongodb" %% "casbah-core" % "2.7.4",
        "org.apache.commons" % "commons-email" % "1.3.2",
        "commons-io" % "commons-io" % "2.4",
        "com.icegreen" % "greenmail" % "1.3.1b" % "test",
        "com.github.finagle" %% "finch-core" % "0.3.0",
        "com.github.finagle" %% "finch-json" % "0.3.0",
        "uk.gov.homeoffice" %% "rtp-io-lib" % `rtp-io-lib-version` withSources(),
        "uk.gov.homeoffice" %% "rtp-test-lib" % `rtp-test-lib-version` withSources(),
        "uk.gov.homeoffice" %% "rtp-mongo-lib" % `rtp-mongo-lib-version` withSources()
      ) ++ Seq(
        "uk.gov.homeoffice" %% "rtp-io-lib" % `rtp-io-lib-version` % Test classifier "tests" withSources(),
        "uk.gov.homeoffice" %% "rtp-test-lib" % `rtp-test-lib-version` % Test classifier "tests" withSources(),
        "uk.gov.homeoffice" %% "rtp-mongo-lib" % `rtp-mongo-lib-version` % Test classifier "tests" withSources()
      )
    })
}