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

package com.lightbend.constructr.coordination
package zookeeper

import java.time.Instant

import akka.Done
import akka.actor.{ ActorSystem, Address, AddressFromURIString }
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import de.heikoseeberger.constructr.coordination.Coordination
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex
import org.apache.curator.utils.ZKPaths
import org.apache.zookeeper.KeeperException.NodeExistsException

private object ZookeeperCoordination {

  object Converters {
    implicit class InstantOps(instant: Instant) {
      def encode: Array[Byte] = {
        val bytes = java.nio.ByteBuffer.allocate(java.lang.Long.BYTES).putLong(instant.toEpochMilli).array()
        Base64.getEncoder.encode(bytes)
      }

      def hasTimeLeft(): Boolean =
        !isOverdue()

      def isOverdue(): Boolean =
        Instant.now.isAfter(instant)

      def +(duration: Duration): Instant =
        instant.plusMillis(duration.toMillis)
    }

    implicit class ByteArrayOps(bytes: Array[Byte]) {
      def decodeInstant: Instant = {
        val decodedBytes = Base64.getDecoder.decode(bytes)
        Instant.ofEpochMilli(java.nio.ByteBuffer.wrap(decodedBytes).getLong)
      }
    }

    implicit class AddressOps(address: Address) {
      def encode: String =
        Base64.getUrlEncoder.encodeToString(address.toString.getBytes(UTF_8))
    }

    implicit class StringOps(s: String) {
      def decodeNode: Address =
        AddressFromURIString(new String(Base64.getUrlDecoder.decode(s), UTF_8))
    }
  }
}

/**
 * A coordination service for ConstructR that uses Zookeeper as the distributed data store.
 *
 * The locking mechanism is using the [[InterProcessSemaphoreMutex]] lock from the Apache Curator library
 * in combination with an additional lock file to store the TTL.
 *
 * Zookeeper does not support the concept that keys can expiry based on a TTL.
 * Therefore, this implementation is using [[Instant]] to represent a TTL.
 * The instant value is stored inside the key as a data object.
 * It is stored in Zookeeper as milliseconds, converted to a byte array and then encoded as a Base64 string.
 * The TTL in milliseconds represents the time elapsed since 1970-01-01T00:00:00 UTC.
 * Because TTL value is always converted into the UTC time zone, it can be safely used across different time zones.
 */
final class ZookeeperCoordination(clusterName: String, system: ActorSystem) extends Coordination {
  import ZookeeperCoordination.Converters._

  private val BasePath = s"/constructr/$clusterName"
  private val NodesPath = s"$BasePath/nodes"
  private val BaseLockPath = s"$BasePath/locks"
  private val SharedLockPath = s"$BaseLockPath/shared"
  private val NodesLockKey = s"$BaseLockPath/nodes-lock"

  private val nodesConnectionString =
    system.settings.config.getStringList("constructr.coordination.nodes").asScala.mkString(",")

  private val client = {
    val delay = system.settings.config.getDuration("constructr.coordination.connection-delay", MILLISECONDS)
    val retry = system.settings.config.getInt("constructr.coordination.connection-retry")
    CuratorFrameworkFactory.builder()
      .connectString(nodesConnectionString)
      .retryPolicy(new ExponentialBackoffRetry(delay.toInt, retry))
      .build()
  }

  run()
  private val lock = init()

  private def run(): Unit = {
    def shutdown(): Unit = {
      system.log.info("Zookeeper client closes connection to nodes [{}]..", nodesConnectionString)
      client.close()
    }

    system.log.info("Zookeeper client tries to establish a connection to nodes [{}]..", nodesConnectionString)
    client.start()
    client.blockUntilConnected()
    sys.addShutdownHook(shutdown())
  }

  private def init(): InterProcessSemaphoreMutex = {
    ZKPaths.mkdirs(client.getZookeeperClient.getZooKeeper, NodesPath)
    ZKPaths.mkdirs(client.getZookeeperClient.getZooKeeper, BaseLockPath)
    new InterProcessSemaphoreMutex(client, SharedLockPath)
  }

  override def getNodes(): Future[Set[Address]] =
    Future.successful {
      val result = nodes
        .flatMap { node =>
          val nodePath = s"$NodesPath/$node"
          val deadline = client.getData.forPath(nodePath).decodeInstant
          if (deadline.hasTimeLeft()) {
            Some(node.decodeNode)
          } else {
            client.delete().forPath(nodePath)
            None
          }
        }
      result
    }

  override def lock(self: Address, ttl: FiniteDuration): Future[Boolean] = {
    def readLock(): Option[Instant] =
      Option(client.checkExists().forPath(NodesLockKey)).map { _ =>
        client.getData.forPath(NodesLockKey).decodeInstant
      }

    def writeLock(expiredLockExist: Boolean): Boolean = {
      try {
        lock.acquire()
        if (lock.isAcquiredInThisProcess) {
          if (expiredLockExist)
            client.delete().forPath(NodesLockKey)
          try {
            client.create().forPath(NodesLockKey, (Instant.now + ttl).encode)
            true
          } catch {
            case e: NodeExistsException =>
              // In the meantime another process has created the write lock.
              // We know that this write lock is active by another process
              // and therefore return false.
              false
          }
        } else {
          false
        }
      } finally {
        lock.release()
      }
    }

    Future.successful {
      readLock() match {
        case Some(deadline) if deadline.hasTimeLeft() => false
        case Some(deadline)                           => writeLock(expiredLockExist = true)
        case None                                     => writeLock(expiredLockExist = false)
      }
    }
  }

  override def addSelf(self: Address, ttl: FiniteDuration): Future[Done] = {
    Future.successful {
      val nodePath = s"$NodesPath/${self.encode}"
      Option(client.checkExists().forPath(nodePath))
        .foreach(_ => client.delete().forPath(nodePath))

      client.create().forPath(nodePath, (Instant.now + ttl).encode)
      Done
    }
  }

  override def refresh(self: Address, ttl: FiniteDuration): Future[Done] =
    Future.successful {
      nodes.foreach { node =>
        val nodePath = s"$NodesPath/$node"
        if (node.decodeNode == self)
          client.setData().forPath(nodePath, (Instant.now + ttl).encode)
        else if (client.getData.forPath(nodePath).decodeInstant.isOverdue())
          client.delete().forPath(nodePath)
      }
      Done
    }

  private def nodes: Set[String] =
    client
      .getChildren
      .forPath(NodesPath)
      .asScala
      .toSet
}
