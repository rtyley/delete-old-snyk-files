ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "delete-old-snyk-files"
  )

libraryDependencies += "com.madgag.play-git-hub" %% "core" % "12.0.0"