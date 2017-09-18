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

import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.{DockerContainer, DockerReadyChecker}

trait DockerZookeeper extends DockerKitDockerJava {
  val DefaultZookeeperPort = 2181

  val zookeeperContainer: DockerContainer = DockerContainer("jplock/zookeeper:3.4.10")
    .withPorts(DefaultZookeeperPort -> Some(DefaultZookeeperPort))
    .withReadyChecker(DockerReadyChecker.LogLineContains("binding to port"))

  abstract override def dockerContainers: List[DockerContainer] = zookeeperContainer :: super.dockerContainers
}
