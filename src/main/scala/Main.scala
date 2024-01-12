import docker.Docker

object Main {
  def api(docker: Docker): Unit = {
//    val broker = docker.getContainer(name = "broker")
    val newContainer = docker.PostContainer(Image = "hello-world", User = "ls", Domainname = "yacinescala")
//    println(docker.createContainer(name = "vovov", config = newContainer))
//    if (broker.isInstanceOf[docker.Container]) {
//      broker.Names foreach println
//    }
    // list containers
//    docker.listContainers().get match {
//      case null => println("nothing")
//      case value => value.foreach { x =>
//        x.Names foreach println
//      }
//    }
//    println("----------------------")

    // list images
//    docker.listImages().get match {
//      case images => images.foreach { image =>
//        println(image.RepoTags)
//        println(image.Containers)
//      }
//    }
//    println("----------------------")

    // list networks
//    docker.listNetworks().get match {
//      case images => images.foreach { image =>
//        println(image)
//      }
//    }

    // search by name
//    val t = docker.getContainer("modest_gagarin")
//    if (t.isInstanceOf[docker.Container]) println(t.Names.head)
//    println("----------------------")

    // search by id
//    val kafka = docker.getContainer(id = "4e747fd8616a")
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
    val docker: Docker = new Docker("/var/run/docker.sock", "localhost")
    // create container
    //    val containerConfig = Map(
    //      "Domainname" -> "test",
    //      "Image" -> "hello-world",
    //      "Labels" -> Map(
    //        "totatota" -> "bobaboba"
    //      ),
    //      "ExposedPorts" -> Map(
    //        "8080" -> Map(),
    //        "8808" -> Map()
    //      )
    //    )
    //    docker.createContainer(name = "tesko", setting = containerConfig)
    api(docker)


    // closes sockerChannel, InputStream, OutputStream
    docker.close()
  }
}
