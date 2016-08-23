lazy val constructrZookeeperRoot = project
  .copy(id = "constructr-zookeeper-root")
  .in(file("."))
  .enablePlugins(GitVersioning)
  .aggregate(constructrCoordinationZookeeper, constructrAkkaTesting)

lazy val constructrCoordinationZookeeper = project
  .copy(id = "constructr-coordination-zookeeper")
  .in(file("constructr-coordination-zookeeper"))
  .enablePlugins(AutomateHeaderPlugin)

lazy val constructrAkkaTesting = project
  .copy(id = "constructr-akka-testing")
  .in(file("constructr-akka-testing"))
  .enablePlugins(AutomateHeaderPlugin)
  .configs(MultiJvm)
  .dependsOn(constructrCoordinationZookeeper % "test->compile")

name := "constructr-zookeeper-root"

unmanagedSourceDirectories.in(Compile) := Vector.empty
unmanagedSourceDirectories.in(Test)    := Vector.empty

publishArtifact := false
