lazy val constructrZookeeperRoot = project
  .copy(id = "constructr-zookeeper-root")
  .in(file("."))
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

// Executes the sbt sub projects sequentially and ensures that the shutdown of the first project has been occurred
// before starting with the subsequent project. This is necessary due to starting and stopping Docker images during tests.
parallelExecution in Global := false
