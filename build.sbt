name := """jacs"""

version := "1.10.2"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

resolvers += "Jaspersoft third party artifacts" at "http://jaspersoft.artifactoryonline.com/jaspersoft/third-party-ce-artifacts/"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.webjars" %% "webjars-play" % "2.4.0-M2",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "jquery-ui" % "1.11.2",
  "org.webjars" % "jquery-ui-themes" % "1.11.2",
  "org.webjars" % "font-awesome" % "4.3.0-1",
  "org.webjars" % "jquery-cookie" % "1.4.1-1",
  "org.webjars" % "jQuery-contextMenu" % "1.6.5",
  "org.webjars" % "select2" % "3.5.2",
  "org.webjars" % "flot" % "0.8.3",
  "org.webjars" % "x-editable-bootstrap3" % "1.5.1",
  "commons-io" % "commons-io" % "2.4",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "be.objectify" %% "deadbolt-java" % "2.3.2",
  "net.sf.jasperreports" % "jasperreports"  % "6.0.3" exclude("commons-io", "commons-io") ,
  "org.apache.poi" % "poi" % "3.11"
)

dependencyOverrides += "commons-io" % "commons-io" % "2.4"