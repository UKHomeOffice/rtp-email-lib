val moduleName = "rtp-email-lib"

val root = Project(id = moduleName, base = file(".")).enablePlugins(ReleasePlugin)
  .settings(
    name := moduleName,
    organization := "uk.gov.homeoffice",
    scalaVersion := "2.12.6",
    crossScalaVersions := Seq("2.11.8", "2.12.6")
  )

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  "Artifactory Snapshot Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-snapshot-local/",
  "Artifactory Release Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-release-local/",
  "Artifactory External Release Local Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/ext-release-local/"
)

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.5",
  "org.joda" % "joda-convert" % "1.7",
  "org.apache.commons" % "commons-email" % "1.3.2",
  "commons-io" % "commons-io" % "2.4",
  "com.icegreen" % "greenmail" % "1.3.1b" % "test",
  "com.github.finagle" %% "finch-core" % "0.21.0",
  "com.github.finagle" %% "finch-circe" % "0.21.0",
  "io.circe" %% "circe-generic" % "0.9.0",
  "uk.gov.homeoffice" %% "rtp-io-lib" % "1.9.11-SNAPSHOT",
  "uk.gov.homeoffice" %% "rtp-test-lib" % "1.4.4-SNAPSHOT",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "3.0.16-SNAPSHOT",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "3.0.16-SNAPSHOT" % Test classifier "tests"
)

publishTo := {
  val artifactory = "http://artifactory.registered-traveller.homeoffice.gov.uk/"

  if (isSnapshot.value)
    Some("snapshot" at artifactory + "artifactory/libs-snapshot-local")
  else
    Some("release"  at artifactory + "artifactory/libs-release-local")
}

// Enable publishing the jar produced by `test:package`
publishArtifact in (Test, packageBin) := true

// Enable publishing the test API jar
publishArtifact in (Test, packageDoc) := true

// Enable publishing the test sources jar
publishArtifact in (Test, packageSrc) := true

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".java" => MergeStrategy.discard
  case _ => MergeStrategy.first
}
