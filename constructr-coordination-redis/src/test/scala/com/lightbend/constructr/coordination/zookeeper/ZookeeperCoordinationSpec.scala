package com.lightbend.constructr.coordination.zookeeper

import java.nio.charset.StandardCharsets._

import akka.Done
import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.constructr.coordination.Coordination
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.concurrent.{ Awaitable, Await }
import scala.concurrent.duration._
import scala.util.Random

object ZookeeperCoordinationSpec {
  import Coordination._

  private implicit val stringNodeSerialization = new NodeSerialization[String] {
    override def fromBytes(bytes: Array[Byte]) = new String(bytes, UTF_8)
    override def toBytes(s: String) = s.getBytes(UTF_8)
  }

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
      val coordination: Coordination = new ZookeeperCoordination(randomString(), randomString(), system)

      resultOf(coordination.getNodes[String]()) shouldBe 'empty

      resultOf(coordination.lock[String]("self", 10.seconds)) shouldBe true
      resultOf(coordination.lock[String]("self", 10.seconds)) shouldBe true
      resultOf(coordination.lock[String]("other", 10.seconds)) shouldBe false

      resultOf(coordination.addSelf[String]("self", 10.seconds)) shouldBe Done
      resultOf(coordination.getNodes[String]()) shouldBe Set("self")

      resultOf(coordination.refresh[String]("self", 1.second)) shouldBe Done
      resultOf(coordination.getNodes[String]()) shouldBe Set("self")

      val probe = TestProbe()
      import probe._
      within(5.seconds) { // 2 seconds should be enough, but who knows hows ...
        awaitAssert {
          resultOf(coordination.getNodes[String]()) shouldBe 'empty
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
