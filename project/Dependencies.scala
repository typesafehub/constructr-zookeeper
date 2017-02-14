import sbt._

object Version {
  val akka             = "2.4.17"
  val constructr       = "0.16.1"
  val curator          = "2.11.0"
  val scala211         = "2.11.8"
  val scala212         = "2.12.1"
  val scalaTest        = "3.0.1"
}

object Library {
  val akkaActor              = "com.typesafe.akka"        %% "akka-actor"              % Version.akka
  val akkaCluster            = "com.typesafe.akka"        %% "akka-cluster"            % Version.akka
  val akkaMultiNodeTestkit   = "com.typesafe.akka"        %% "akka-multi-node-testkit" % Version.akka
  val akkaTestkit            = "com.typesafe.akka"        %% "akka-testkit"            % Version.akka
  val constructr             = "de.heikoseeberger"        %% "constructr"              % Version.constructr
  val constructrCoordination = "de.heikoseeberger"        %% "constructr-coordination" % Version.constructr
  val curatorFramework       = "org.apache.curator"       %  "curator-framework"       % Version.curator
  val curatorRecipes         = "org.apache.curator"       %  "curator-recipes"         % Version.curator
  val scalaTest              = "org.scalatest"            %% "scalatest"               % Version.scalaTest
}
