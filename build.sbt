val moduleName = "rtp-email-lib"

val root = Project(id = moduleName, base = file("."))
  .enablePlugins(GitVersioning)
  .settings(
    name := moduleName,
    organization := "uk.gov.homeoffice",
    scalaVersion := "2.12.6",
    crossScalaVersions := Seq("2.11.8", "2.12.6")
  )

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
resolvers ++= Seq(
  "Artifactory Snapshot Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-snapshot-local/",
  "Artifactory Release Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release-local/",
  "Artifactory External Release Local Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/ext-release-local/"
)

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.11.1",
  "org.joda" % "joda-convert" % "2.2.2",
  "org.apache.commons" % "commons-email" % "1.5",
  "commons-io" % "commons-io" % "2.11.0",
  "com.icegreen" % "greenmail" % "1.6.10" % "test",
  "com.github.finagle" %% "finch-core" % "0.31.0",
  "com.github.finagle" %% "finch-circe" % "0.31.0",
  "io.circe" %% "circe-generic" % "0.11.2",
  "uk.gov.homeoffice" %% "rtp-io-lib" % "2.2.5-g588ed6c",
  "uk.gov.homeoffice" %% "rtp-test-lib" % "1.6.6-g6f56307",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "3.1.5-g4c83f76",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "3.1.5-g4c83f76" % Test classifier "tests"
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
