name := "SWRS"

version := "0.1"

scalaVersion := "2.12.6"

updateOptions := updateOptions.value.withGigahorse(false)

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Jzy3d Releases"    at "https://maven.jzy3d.org/releases/"
)

libraryDependencies  ++= Seq(
  "org.scalanlp"                 %% "breeze"                  % "0.13.2",
  "org.scalanlp"                 %% "breeze-natives"          % "0.13.2",
  "org.scalanlp"                 %% "breeze-viz"              % "0.13.2",
  "org.jzy3d"                    %  "jzy3d-api"               % "1.0.0",
  "net.sf.jchart2d"              %  "jchart2d"                % "3.3.2",
  "com.typesafe.akka"            %% "akka-stream"             % "2.5.14",
  "org.graphstream"              %  "gs-core"                 % "1.3"
)
