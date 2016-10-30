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

import akka.Done
import akka.actor.{ ActorSystem, AddressFromURIString }
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.constructr.coordination.Coordination
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.concurrent.{ Await, Awaitable }
import scala.concurrent.duration._
import scala.util.Random

object ZookeeperCoordinationSpec {
  private val address1 = AddressFromURIString("akka.tcp://default@a:2552")
  private val address2 = AddressFromURIString("akka.tcp://default@b:2552")

  // this test assumes zookeeper server is up on DOCKER_HOST or localhost(127.0.0.1)
  // below command would be help:
  //   $ docker run --name zookeeper -p 2181:2181 -d jplock/zookeeper
  private val coordinationHost = {
    val dockerHostPattern = """tcp://(\S+):\d{1,5}""".r
    sys.env.get("DOCKER_HOST")
      .collect { case dockerHostPattern(address) => address }
      .getOrElse("127.0.0.1")
  }
}

class ZookeeperCoordinationSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  import ZookeeperCoordinationSpec._

  private implicit val system = {
    val config = ConfigFactory.parseString(s"constructr.coordination.host = $coordinationHost").withFallback(ConfigFactory.load())
    ActorSystem("default", config)
  }

  "ZookeeperCoordination" should {
    "correctly interact with zookeeper" in {
      val coordination: Coordination = new ZookeeperCoordination(randomString(), system)

      resultOf(coordination.getNodes()) shouldBe 'empty

      resultOf(coordination.lock(address1, 10.seconds)) shouldBe true
      resultOf(coordination.lock(address2, 10.seconds)) shouldBe false

      resultOf(coordination.addSelf(address1, 10.seconds)) shouldBe Done
      resultOf(coordination.getNodes()) shouldBe Set(address1)

      resultOf(coordination.refresh(address1, 1.second)) shouldBe Done
      resultOf(coordination.getNodes()) shouldBe Set(address1)

      val probe = TestProbe()
      import probe._
      within(5.seconds) { // 2 seconds should be enough, but who knows hows ...
        awaitAssert {
          resultOf(coordination.getNodes()) shouldBe 'empty
        }
      }
    }
  }

  override protected def afterAll() = {
    Await.ready(system.terminate(), Duration.Inf)
    super.afterAll()
  }

  private def resultOf[A](awaitable: Awaitable[A], max: FiniteDuration = 3.seconds): A = Await.result(awaitable, max)

  private def randomString() = math.abs(Random.nextInt).toString
}
