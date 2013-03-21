name := "context-pacifier"

organization := "com.jteigen"

autoScalaLibrary := false

crossPaths := false

libraryDependencies ++= Seq(
    "javax.servlet" % "servlet-api" % "2.5" % "provided",
    "nu.validator.htmlparser" % "htmlparser" % "1.4")

libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-webapp" % "8.1.10.v20130312" % "test")

