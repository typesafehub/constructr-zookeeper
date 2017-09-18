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

import akka.actor.ActorSystem
import com.lightbend.constructr.coordination.zookeeper.ZookeeperNodes.Nodes
import com.typesafe.config.ConfigFactory
import org.scalatest.{ Matchers, WordSpec }

class ZookeeperNodesSpec extends WordSpec with Matchers with ZookeeperNodes {

  "ZookeeperNodes" should {
    "should extract nodes from a list" in {
      val config = ConfigFactory.parseString(
        s"""
           |constructr.coordination.nodes = ["host1:2181", "host2:2181"]
         """.stripMargin
      )
      val actorSystem = ActorSystem("default", config)

      config.getStringList(Nodes).size() shouldBe 2
      config.getStringList(Nodes) should contain("host1:2181")
      config.getStringList(Nodes) should contain("host2:2181")

      nodesConnectionString(actorSystem) shouldBe "host1:2181,host2:2181"
    }

    "should extract ZK nodes from a string" in {
      val config = ConfigFactory.parseString(
        s"""
           |constructr.coordination.nodes = "host1:2181,host2:2181"
         """.stripMargin
      )
      val actorSystem = ActorSystem("default", config)

      config.getString(Nodes) shouldBe "host1:2181,host2:2181"
      nodesConnectionString(actorSystem) shouldBe "host1:2181,host2:2181"
    }
  }
}
