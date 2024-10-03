val moduleName = "rtp-email-lib"

val root = Project(id = moduleName, base = file("."))
  .enablePlugins(GitVersioning)
  .settings(
    name := moduleName,
    organization := "uk.gov.homeoffice",
    scalaVersion := "2.13.14",
    crossScalaVersions := Seq("2.12.16", "2.13.14")
  )

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
resolvers ++= Seq(
  "Artifactory Snapshot Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-snapshot-local/",
  "Artifactory Release Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release-local/",
  "Artifactory External Release Local Realm" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/ext-release-local/"
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "joda-time" % "joda-time" % "2.12.5",
  "org.joda" % "joda-convert" % "2.2.3",
  "org.apache.commons" % "commons-lang3" % "3.14.0",
  "uk.gov.homeoffice" %% "rtp-io-lib" % "2.2.24-gd67586d" excludeAll ExclusionRule(organization = "org.json4s"),
  "uk.gov.homeoffice" %% "rtp-test-lib" % "1.6.22-gacd233d",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "5.0.4-g9085b13",
  "uk.gov.homeoffice" %% "rtp-mongo-lib" % "5.0.4-g9085b13" % Test classifier "tests",
  "org.typelevel" %% "cats-effect" % "3.5.2"
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
