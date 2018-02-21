name := "Simulation with reac streams"

version := "0.1"

scalaVersion := "2.12.4"

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Jzy3d Releases"    at "http://maven.jzy3d.org/releases"
)

libraryDependencies  ++= Seq(
  "org.scalanlp"                 %% "breeze"                  % "0.13.2",
  "org.scalanlp"                 %% "breeze-natives"          % "0.13.2",
  "org.scalanlp"                 %% "breeze-viz"              % "0.13.2",
  "org.jzy3d"                    %  "jzy3d-api"               % "1.0.0"
)

