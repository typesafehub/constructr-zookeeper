name := "constructr-coordination-zookeeper"

libraryDependencies ++= Vector(
  Library.akkaActor,
  Library.constructrCoordination,
  Library.curatorFramework,
  Library.curatorRecipes,
  Library.akkaTestkit % "test",
  Library.scalaTest % "test",
  Library.dockerTestKit % "test",
  Library.dockerTestKitImpl % "test"
)
