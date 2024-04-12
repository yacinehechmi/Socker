import docker._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Main {
  def api(docker: Docker): Unit = {
    docker.listContainers().get foreach { container =>
      container.stop()
    }
    // docker version
    // println(docker.version())

    //docker.listImages(true, true, true).get.foreach(println)

    // VVVV this works VVVV
    //val filters = Map("name" -> "airflow-airflow-webserver-1", "ancestor" -> "apache/airflow:2.7.3")
    // list containers
    //docker.listContainers().get match {
    //case null => println("no containers")
    //case containers => containers.foreach { x =>
    //println(x.Names)
    //}
    //}

    //val filters = Map("name" -> "airflow_default", "driver" -> "bridge")
    //docker.listNetworks().get.foreach(x => println(x.Name))

    // -- filters are not working
    //val filters = Map("driver" -> "local")
    //docker.listVolumes(filters)
    // -- filters are not working

    //// start all containers
    //docker.listContainers().get foreach { container =>
    //container.stop()
    //}

    // restart all containers
    //docker.listContainers().get foreach { container =>
    //container.restart()
    //}

    // kill a container
    //// get container by name
    //val modestGagarin = docker.getContainer("modest_gagarin")
    //modestGagarin match {
    //case Some(container) => println(container)
    //case _ => println("name or id is not valid")
    //}

    //// search by id
    //val kafka = docker.getContainer(id = "4e747fd8616a")
    ////    println("----------------------")
    ////    val kafka2 = docker.getContainer(id = "4e747fd8616a")
    ////    println("----------------------")
    ////    val kafka3 = docker.getContainer(id = "4e747fd8616a")
    ////    println("----------------------")
    ////    val kafka4 = docker.getContainer(id = "4e747fd8616a")
    //// if (kafka.isInstanceOf[docker.Container]) println(kafka.Names.head)

    ////    println("----------------------")
    //// list volumes
    ////    docker.listVolumes().get match {
    ////      case items => items.Volumes foreach println
    ////    }

    // trying create Container
    //docker.createContainer(
    //name = "tito",
    //config = docker.PostContainer(
    //Hostname = "zmegri",
    //Cmd = List("ping", "google.com"),
    //Image = "hello-world"
    //)
    //)

    // inspecting a container
    //    docker.inspectContainer(id = "134689394a6c", size = true) match {
    //      case Some(inspection) =>
    //        println("found a inspections")
    //        println(inspection)
    //      case None =>
    //        println("no container found")
    //    }

    // stop containers
    //docker.listContainers().get match {
    //case null => println("no containers")
    //case containers => containers.foreach { x =>
    //val f = Future(x.start())
    //Thread.sleep(500)
    //f.foreach(println)
    //}
    //}

    // start containers
    docker.listContainers().get match {
      case null => println("no containers")
      case containers => containers.foreach { x =>
        val f = Future(x.getContainer.Names)
        f.foreach(println)
      }
    }

    // listing processes running inside a container
  // /containers/<id>/top
   //docker.top(id = "fcfda7fcbb31", psArgs = "a") match {
     //case Some(processes) => 
       //println(s"found a processes")
       //println(processes)
    //case None => println("no container found")
  //}
//}
 
  // getting container logs by ID
  // /containers/<id>/logs
  //docker.logs(id = "fcfda7fcbb31", stdout = true) match {
    //case Some(processes) => 
      //println(s"found a processes")
      //println(processes)
   //case None => println("no container found")
  //}

  // get container filesystem changes
  //docker.listfschanges(id = "fcfda7fcbb31") match {
    //case Some(value) => 
      //println(s"found something")
      //println(value)
    //case none => println("nothing")
  //}
  
  // export container
  //docker.exportContainer(id = "fcfda7fcbb31") match {
    //case Some(value) => 
      //println(s"found something")
      //println(value)
    //case none => println("nothing")
  //}

  // get container Stats
  //docker.containerStats(id = "a99770fe0f6a", stream = true) match {
    //case Some(value) => 
      //println(s"found something")
      //println(value)
    //case none => println("nothing")
  //}

  // Todays work
  /**/
}

  def main(args: Array[String]): Unit = {
    // connecting to docker
    implicit val http: HttpDockerClient = new HttpDockerClient(Some("/var/run/docker.sock"), Some("localhost"))
    val docker: Docker = new Docker

    // trying methods in the api
    api(docker)

    // release resources
  }
}

