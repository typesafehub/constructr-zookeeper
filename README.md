# ConstructR-ZooKeeper #
[![Build Status](https://travis-ci.org/typesafehub/constructr-zookeeper.svg?branch=master)](https://travis-ci.org/typesafehub/constructr-zookeeper)

This library enables to use [ZooKeeper](https://zookeeper.apache.org/) as cluster coordinator in a [ConstructR](https://github.com/hseeberger/constructr) based node.

[ConstructR](https://github.com/hseeberger/constructr) aims at cluster bootstrapping (construction) by using a coordination service and provides etcd as the default one. By means of this library, you will be able to use [ZooKeeper](https://zookeeper.apache.org/) as coordination service instead.

You will need to add the following dependency in your `build.sbt` in addition to the core ConstructR ones:

```
libraryDependencies += "com.lightbend.constructr" %% "constructr-coordination-zookeeper" % "0.2.0"
```


## Configuration ##

Check [this section](https://github.com/hseeberger/constructr#coordination) in ConstructR for general information about configuration.

Check [reference.conf](constructr-coordination-zookeeper/src/main/resources/reference.conf) for ZooKeeper related configuration.

### Configuring ZK cluster nodes ###

The default configuration tries to establish a connection to ZooKeeper on `localhost:2181`.

Override the `constructr.coordination.nodes` configuration to specify another ZooKeeper node:

```
constructr.coordination.nodes = ["10.10.10.10:2181"]
```

The format per node `ip:port`.

You are also able to connect to a multi-node cluster by specifying multiple nodes, separated by a comma:

```
constructr.coordination.nodes = ["10.10.10.10:2181", "10.10.10.11:2181", "10.10.10.12:2181"]
```

## Testing

Requirements:
  - ZooKeeper needs to be running, e.g. via `docker run --name zookeeper -p 2181:2181 -d jplock/zookeeper`

Run tests:
  - `sbt test`
