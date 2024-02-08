import docker.Docker

object Main {
  def api(docker: Docker): Unit = {
    // docker version
    println(docker.version())

    // list containers
    docker.listContainers().get match {
      case null => println("no containers")
      case containers => containers.foreach { x =>
        x.Names foreach println
      }
    }

    //    println("----------------------")

    // list images
    docker.listImages().get match {
      case null => println("no images")
      case images => images.foreach {img =>
        img.RepoTags foreach println
      }
    }

    // list networks
    docker.listNetworks().get match {
      case null => println("no networks")
      case networks => networks.foreach { net =>
        println(net.Driver)
      }
    }
    // println("----------------")

    // list volumes
    docker.listVolumes().get match {
      case null => println("no volumes")
      case volumes => volumes.foreach { volume =>
        println(volume.Name)
      }
    }

    // stop all containers
    docker.listContainers().get foreach { container =>
      container.kill()
    }

    // start all containers
    docker.listContainers().get foreach { container =>
      container.start()
    }

    // get container by name
        val modestGagarin = docker.getContainer("modest_gagarin")
        modestGagarin match {
          case Some(container) => println(container)
          case _ => println("name or id is not valid")
        }

    // search by id
        val kafka = docker.getContainer(id = "4e747fd8616a")
    //    println("----------------------")
    //    val kafka2 = docker.getContainer(id = "4e747fd8616a")
    //    println("----------------------")
    //    val kafka3 = docker.getContainer(id = "4e747fd8616a")
    //    println("----------------------")
    //    val kafka4 = docker.getContainer(id = "4e747fd8616a")
    // if (kafka.isInstanceOf[docker.Container]) println(kafka.Names.head)

    //    println("----------------------")
    // list volumes
    //    docker.listVolumes().get match {
    //      case items => items.Volumes foreach println
    //    }
  }

  def main(args: Array[String]): Unit = {
    // connecting to docker
    val docker: Docker = new Docker("/var/run/docker.sock", "localhost")

    // trying methods in the api
    api(docker)

    // release resources
    docker.close()
  }
}
