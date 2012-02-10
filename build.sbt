seq(webSettings :_*)

name := "hierarchy"

version := "1.0"

libraryDependencies ++= {
  val liftVersion = "2.4" // Put the current/latest lift version here
  Seq(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-mapper" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-wizard" % liftVersion % "compile->default")
}

libraryDependencies ++= Seq("org.mortbay.jetty" % "jetty" % "6.1.25" % "container",
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
   "org.neo4j" % "neo4j" % "1.6")
