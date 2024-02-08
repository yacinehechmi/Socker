# Welcome to Socker

Socker is a [Docker](https://docs.docker.com/get-started/overview/) SDK implemented in [Scala](https://www.scala-lang.org/).
It provides a fast and secure connection through [Unix Domain Sockets](https://en.wikipedia.org/wiki/Unix_domain_socket) in future versions it will also support HTTP connection, inspired by [Docker SDK for Python](https://docker-py.readthedocs.io/en/stable/)

---

## Requirements
- [Scala version](https://www.scala-lang.org/download/3.3.0.html) `3.3.0`
- [Docker engine version](https://docs.docker.com/engine/api/v1.43/) `1.43`

---

## Installation
Still not available on the [Maven repository](https://mvnrepository.com/) 
1. Clone the repo: `git clone https://github.com/yacinehechmi/Socker.git`
2. Build and run via sbt: `sbt` then `sbt runMain Main`

---

## Usage
- Connecting to Docker through a Unix Domain Socket
`val docker: Docker = new Docker("/var/run/docker.sock", "localhost")`

to interact with the Docker remote API use a Docker class instance
- Using the Docker Class instance

1. Getting the docker version
`docker.version()`


2. Listing containers (by default it will list all containers including non running ones)
`docker.listContainers().get match {
      case null => println("no containers")
      case containers => containers.foreach { x =>
        x.Names foreach println
      }
}`


3. Listing images
`docker.listImages().get match {
      case null => println("no images")
      case images => images.foreach {img =>
        img.RepoTags foreach println
      }
}`


4. Listing networks
`docker.listNetworks().get match {
      case null => println("no networks")
      case networks => networks.foreach { net =>
        println(net.Driver)
      }
}`


5. Get container by name 
`val modestGagarin = docker.getContainer("modest_gagarin")`


6.  Get container by id
`val kafkaBroker = docker.getContainer(id = "4e239dke04a")`


7. Start a container
`kafkaBroker.start()`


8. Kill a container
`kafkaBroker.kill()`


10. Start all containers
`docker.listContainers().get foreach(_.start())`

## Todos
- [] Implement HTTP support
- [] Implement secure connection (HTTPS)
- [] Add more endpoints
- [] Work on filters and parameters of each endpoint
- [] Add X-Registry-Auth for authenticating to docker
