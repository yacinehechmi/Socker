import docker.Docker

object Main {
  def api(docker: Docker): Unit = {
    // docker version
    // println(docker.version())
    // list containers
    //docker.listImages(true, true, true).get.foreach(println)
    //val filters = Map("ancestor" -> "apache/airflow:2.8.1", "name" -> "airflow-airflow-scheduler-1")
    //docker.listContainers(filters=filters).get match {
      //case null => println("no containers")
      //case containers => containers.foreach { x =>
       //println(x.Names)
      //}
    //}
    
    //val filters = Map("dangling" -> true, "driver" -> "bridge", "type" -> "builtin")
    //docker.listNetworks(filters).get.foreach(println)
    //// stop all containers
    //docker.listContainers().get foreach { container =>
      //container.stop()
    //}

    // -- filters are not working
    //val filters = Map("driver" -> "local")
    //docker.listVolumes(filters)
    // -- filters are not working
    
    //// start all containers
    //docker.listContainers().get foreach { container =>
      //container.start()
    //}

    // restart all containers
    //docker.listContainers().get foreach { container =>
      //container.restart()
    //}
    
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
  //docker.inspectContainer(id = "e5a7fb66e17c", size = true) match {
    //case Some(inspection) => 
      //println("found a inspections")
      //println(inspection)
    //case None => println("no container found")
  //}

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
  docker.containerStats(id = "fcfda7fcbb31", stream = false) match {
    case Some(value) => 
      println(s"found something")
      println(value)
    case none => println("nothing")
  }

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
