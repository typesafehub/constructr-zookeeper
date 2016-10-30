/*
 * Copyright 2016 Lightbend Inc. <http://www.lightbend.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.constructr.coordination.zookeeper

import akka.actor.ActorDSL.{Act, actor}
import akka.cluster.{Cluster, ClusterEvent}
import akka.pattern.ask
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import akka.testkit.TestDuration
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.constructr.ConstructrExtension
import de.heikoseeberger.constructr.coordination.Coordination
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

object ConstructrMultiNodeConfig {
  val coordinationHost = {
    val dockerHostPattern = """tcp://(\S+):\d{1,5}""".r
    sys.env.get("DOCKER_HOST")
      .collect { case dockerHostPattern(address) => address }
      .getOrElse("127.0.0.1")
  }
}

class ConstructrMultiNodeConfig(coordinationPort: Int) extends MultiNodeConfig {

  import ConstructrMultiNodeConfig._

  commonConfig(ConfigFactory.load())
  for (n <- 1.to(5)) {
    val port = 2550 + n
    nodeConfig(role(port.toString))(ConfigFactory.parseString(
      s"""|akka.actor.provider            = akka.cluster.ClusterActorRefProvider
          |akka.remote.netty.tcp.hostname = "127.0.0.1"
          |akka.remote.netty.tcp.port     = $port
          |constructr.coordination.host   = $coordinationHost
          |constructr.coordination.port   = $coordinationPort
          |""".stripMargin
    ))
  }
}

abstract class MultiNodeZookeeperConstructrBaseSpec(coordinationPort: Int, clusterName: String)
  extends MultiNodeSpec(new ConstructrMultiNodeConfig(coordinationPort))
    with FreeSpecLike with Matchers with BeforeAndAfterAll {

  implicit val mat = ActorMaterializer()
  private val zookeeperClient = {
    val host = system.settings.config.getString("constructr.coordination.host")
    val port = system.settings.config.getInt("constructr.coordination.port")
    val delay = system.settings.config.getDuration("constructr.coordination.connection-delay", MILLISECONDS)
    val retry = system.settings.config.getInt("constructr.coordination.connection-retry")
    CuratorFrameworkFactory.builder()
      .connectString(s"$host:$port")
      .retryPolicy(new ExponentialBackoffRetry(delay.toInt, retry))
      .build()
  }
  private val zookeeperCoordination = Coordination(clusterName, system)

  "Constructr should manage an Akka cluster" in {
    runOn(roles.head) {
      within(20.seconds.dilated) {
        zookeeperClient.delete().deletingChildrenIfNeeded().forPath("/constructr")
      }
    }

    enterBarrier("coordination-started")

    ConstructrExtension(system)
    val listener = actor(new Act {

      import ClusterEvent._

      var isMember = false
      Cluster(context.system).subscribe(self, InitialStateAsEvents, classOf[MemberJoined], classOf[MemberUp])
      become {
        case "isMember" => sender() ! isMember
        case MemberJoined(member) if member.address == Cluster(context.system).selfAddress => isMember = true
        case MemberUp(member) if member.address == Cluster(context.system).selfAddress => isMember = true
      }
    })
    within(20.seconds.dilated) {
      awaitAssert {
        implicit val timeout = Timeout(1.second.dilated)
        val isMember = Await.result((listener ? "isMember").mapTo[Boolean], 1.second.dilated)
        isMember shouldBe true
      }
    }

    enterBarrier("cluster-formed")

    within(5.seconds.dilated) {
      awaitAssert {
        val constructrNodes = Await.result(
          zookeeperCoordination.getNodes(),
          1.second.dilated
        )
        roles.to[Set].map(_.name.toInt) shouldEqual constructrNodes.flatMap(_.port)
      }
    }

    enterBarrier("done")
  }

  override def initialParticipants = roles.size

  override protected def beforeAll() = {
    super.beforeAll()
    multiNodeSpecBeforeAll()
  }


  override protected def atStartup(): Unit = {
    super.atStartup()
    zookeeperClient.start()
  }

  override protected def afterAll() = {
    multiNodeSpecAfterAll()
    super.afterAll()
  }

  override protected def afterTermination(): Unit = {
    super.afterTermination()
    zookeeperClient.close()
  }
}
