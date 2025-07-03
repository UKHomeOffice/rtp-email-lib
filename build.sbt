val moduleName = "rtp-email-lib"

val root = Project(id = moduleName, base = file("."))
  .enablePlugins(GitVersioning)
  .settings(
    name := moduleName,
    organization := "uk.gov.homeoffice",
    scalaVersion := "3.3.5",
    crossScalaVersions := Seq("3.3.5", "2.13.16")
  )

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
resolvers ++= Seq(
  "Artifactory Snapshot Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-snapshot-local/",
  "Artifactory Release Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release-local/",
  "Artifactory External Release Local Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/ext-release-local/"
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.5.18",
  "joda-time" % "joda-time" % "2.14.0",
  "org.joda" % "joda-convert" % "3.0.1",
  "org.apache.commons" % "commons-lang3" % "3.17.0",
  "uk.gov.homeoffice" %% "rtp-io-lib" % "2.2.28-ge2e7d1e",
  "uk.gov.homeoffice" %% "rtp-test-lib" % "1.6.37-g813af7a",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "5.0.15-g96ac4e2",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "5.0.15-g96ac4e2" % Test classifier "tests",
  "org.typelevel" %% "cats-effect" % "3.6.1"
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
