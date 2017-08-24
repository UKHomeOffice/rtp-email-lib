import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin

object Build extends Build {
  val moduleName = "rtp-email-lib"

  val root = Project(id = moduleName, base = file(".")).enablePlugins(ReleasePlugin)
    .settings(
      name := moduleName,
      organization := "uk.gov.homeoffice",
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
        "Artifactory Release Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-release-local/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
        "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
        "Kamon Repository" at "http://repo.kamon.io"
      )
    )
    .settings(libraryDependencies ++= {
      val `rtp-io-lib-version` = "1.9.10"
      val `rtp-test-lib-version` = "1.4.3"
      val `rtp-mongo-lib-version` = "3.0.13"

      Seq(
        "joda-time" % "joda-time" % "2.5",
        "org.joda" % "joda-convert" % "1.7",
        "org.apache.commons" % "commons-email" % "1.3.2",
        "commons-io" % "commons-io" % "2.4",
        "com.icegreen" % "greenmail" % "1.3.1b" % "test",
        "com.github.finagle" %% "finch-core" % "0.3.0",
        "com.github.finagle" %% "finch-json" % "0.3.0",
        "uk.gov.homeoffice" %% "rtp-io-lib" % `rtp-io-lib-version` withSources() exclude("javax.mail", "mailapi"),
        "uk.gov.homeoffice" %% "rtp-test-lib" % `rtp-test-lib-version` withSources(),
        "uk.gov.homeoffice" %% "rtp-mongo-lib" % `rtp-mongo-lib-version` withSources() exclude("javax.mail", "mailapi")
      ) ++ Seq(
        "uk.gov.homeoffice" %% "rtp-io-lib" % `rtp-io-lib-version` % Test classifier "tests" withSources() exclude("javax.mail", "mailapi"),
        "uk.gov.homeoffice" %% "rtp-test-lib" % `rtp-test-lib-version` % Test classifier "tests" withSources(),
        "uk.gov.homeoffice" %% "rtp-mongo-lib" % `rtp-mongo-lib-version` % Test classifier "tests" withSources() exclude("javax.mail", "mailapi")
      )
    })
}
