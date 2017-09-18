import sbt._

object Version {
  val akka             = "2.5.1"
  val constructr       = "0.17.0"
  val curator          = "2.12.0"
  val scala211         = "2.11.11"
  val scala212         = "2.12.3"
  val scalaTest        = "3.0.1"
  val dockerItScala    = "0.9.5"
}

object Library {
  val akkaActor              = "com.typesafe.akka"        %% "akka-actor"                       % Version.akka
  val akkaCluster            = "com.typesafe.akka"        %% "akka-cluster"                     % Version.akka
  val akkaMultiNodeTestkit   = "com.typesafe.akka"        %% "akka-multi-node-testkit"          % Version.akka
  val akkaTestkit            = "com.typesafe.akka"        %% "akka-testkit"                     % Version.akka
  val constructr             = "de.heikoseeberger"        %% "constructr"                       % Version.constructr
  val constructrCoordination = "de.heikoseeberger"        %% "constructr-coordination"          % Version.constructr
  val curatorFramework       = "org.apache.curator"       %  "curator-framework"                % Version.curator
  val curatorRecipes         = "org.apache.curator"       %  "curator-recipes"                  % Version.curator
  val scalaTest              = "org.scalatest"            %% "scalatest"                        % Version.scalaTest
  val dockerTestKit          = "com.whisk"                %% "docker-testkit-scalatest"         % Version.dockerItScala
  val dockerTestKitImpl      = "com.whisk"                %% "docker-testkit-impl-docker-java"  % Version.dockerItScala
}
