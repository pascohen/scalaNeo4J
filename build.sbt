seq(webSettings :_*)

name := "hierarchy"

version := "1.0"

libraryDependencies ++= Seq("org.mortbay.jetty" % "jetty" % "6.1.25" % "container",
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
   "org.neo4j" % "neo4j" % "1.6")
