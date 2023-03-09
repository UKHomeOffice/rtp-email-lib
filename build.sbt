val moduleName = "rtp-email-lib"

val root = Project(id = moduleName, base = file("."))
  .enablePlugins(GitVersioning)
  .settings(
    name := moduleName,
    organization := "uk.gov.homeoffice",
    scalaVersion := "2.12.16",
    crossScalaVersions := Seq("2.11.8", "2.12.16")
  )

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
resolvers ++= Seq(
  "Artifactory Snapshot Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-snapshot-local/",
  "Artifactory Release Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release-local/",
  "Artifactory External Release Local Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/ext-release-local/"
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.5",
  "joda-time" % "joda-time" % "2.12.2",
  "org.joda" % "joda-convert" % "2.2.3",
  "org.apache.commons" % "commons-email" % "1.5",
  "commons-io" % "commons-io" % "2.11.0",
  "org.apache.commons" % "commons-lang3" % "3.12.0",
  "com.icegreen" % "greenmail" % "2.0.0" % "test",
  "uk.gov.homeoffice" %% "rtp-io-lib" % "2.2.11-g740212b",
  //"uk.gov.homeoffice" %% "rtp-io-lib" % "2.2.13-g55a5f1d-DPSPS-1142-DependencyUpdates",
  "uk.gov.homeoffice" %% "rtp-test-lib" % "1.6.18-g13a878a",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "3.1.11-g386f964",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "3.1.11-g386f964" % Test classifier "tests"
)

publishTo := {
  val artifactory = sys.env.get("ARTIFACTORY_SERVER").getOrElse("https://artifactory.digital.homeoffice.gov.uk/")
  Some("release"  at artifactory + "artifactory/libs-release-local")
}

publishArtifact in (Test, packageBin) := true
publishArtifact in (Test, packageDoc) := true
publishArtifact in (Test, packageSrc) := true

git.useGitDescribe := true
git.gitDescribePatterns := Seq("v?.?")
git.gitTagToVersionNumber := { tag :String =>

val branchTag = if (git.gitCurrentBranch.value == "master") "" else "-" + git.gitCurrentBranch.value
val uncommit = if (git.gitUncommittedChanges.value) "-U" else ""

tag match {
  case v if v.matches("v\\d+.\\d+") => Some(s"$v.0${uncommit}".drop(1))
  case v if v.matches("v\\d+.\\d+-.*") => Some(s"${v.replaceFirst("-",".")}${branchTag}${uncommit}".drop(1))
  case _ => None
}}
