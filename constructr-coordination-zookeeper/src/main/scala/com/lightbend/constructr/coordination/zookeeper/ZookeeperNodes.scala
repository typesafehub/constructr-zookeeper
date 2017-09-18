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
import com.typesafe.config.ConfigException.WrongType

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.util.Try

object ZookeeperNodes {
  val Nodes: String = "constructr.coordination.nodes"
}

/**
 * Helper for extracting Zookeeper nodes configuration from {@link akka.actor.ActorSystem ActorSystem} settings.
 *
 * First, tries to get comma-saparated list of nodes from `String` settings,
 * if not found then falls back to parsing `List` of strings.
 */
trait ZookeeperNodes {
  def nodesConnectionString(system: ActorSystem): String = {
    Try(
      system.settings.config.getString(Nodes)
    ).recover {
        case ex: WrongType => system.settings.config.getStringList(Nodes).asScala.mkString(",")
      }.get
  }
}
