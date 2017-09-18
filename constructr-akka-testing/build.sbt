name := "constructr-akka-testing"

libraryDependencies ++= Vector(
  Library.constructr           % "test",
  Library.akkaCluster          % "test",
  Library.akkaMultiNodeTestkit % "test",
  Library.akkaTestkit          % "test",
  Library.scalaTest            % "test",
  Library.dockerTestKit        % "test",
  Library.dockerTestKitImpl    % "test"
)


unmanagedSourceDirectories.in(MultiJvm) := Vector(scalaSource.in(MultiJvm).value)

test.in(Test) := { test.in(MultiJvm).value; test.in(Test).value }

inConfig(MultiJvm)(SbtScalariform.configScalariformSettings)

AutomateHeaderPlugin.automateFor(MultiJvm)
HeaderPlugin.settingsFor(MultiJvm)
