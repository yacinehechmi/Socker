package docker

import upickle.default._
import client.{HttpSocket, Header, Request, Path}


case class ContainerState(Id: String, Names: List[String], Image: String, ImageID: String,
  Command: String, Created: String, State: String, Status: String,
  HostConfig: Map[String, String], NetworkSettings: Map[String, Map[String, Driver]] = Map(),
  Ports: List[Port], Labels: Map[String, String], Mounts: List[Mount] = List()) derives ReadWriter

case class Port(IP: String = "", PrivatePort: Int = 0,
  PublicPort: Int = 0, Type: String = "") derives ReadWriter

case class Driver(IPAMConfig: String, Links: String, Aliases: String, NetworkID: String,
  EndpointID: String, Gateway: String, IPAddress: String, IPPrefixLen: Int,
  IPv6Gateway: String, GlobalIPv6Address: String, GlobalIPv6PrefixLen: Int, MacAddress: String,
  DriverOpts: String) derives ReadWriter

case class Mount(Type: String = "", Name: String = "", Source: String = "", Destination: String = "",
  Driver: String = "", Mode: String = "", RW: Boolean = false, Propagation: String = "") derives ReadWriter

class Container(http: HttpDockerClient, container: ContainerState) {
  private final val _endpoint = "/v1.43/containers/"

   def stop(): Unit = {
     if (this.container.Status.startsWith("Up")) {
       val req = Request(_endpoint+this.container.Id.substring(0, 12)+"/stop", http._host)
       http.send[String](req, http.socket.post)
     }

   else return
   }

   /*--work on params and filters--*/
  // POST /v1.43/containers/{id}/start
  // still working on this, should not return Unit
  def start(): Unit = {
    if (this.container.Status.startsWith("Exited")) {
      val req = Request(_endpoint+this.container.Id+"/start", http._host)
      http.send[String](req, http.socket.post)
    }

  else return
  }

  // /v1.43/containers/{id}/json = docker inspect <container_id>
  // still working on this, should not return Unit

  /*--work on params and filters--*/
  // POST /v1.43/containers/{id}/restart
  def restart(): Unit = {
   val req = Request(_endpoint+this.container.Id+"/restart", http._host)
   http.send[String](req, http.socket.post)
  }


  def kill(): Unit = {
    val req = Request(_endpoint+this.container.Id+"/kill", http._host)
  }

   /*--work on params and filters--*/
  // DELETE /v1.43/containers/{id}/
  def remove(): Unit = {
    val req = Request(_endpoint+this.container.Id, http._host)
    http.send[String](req, http.socket.delete)
  }

  def getContainer: ContainerState = container
}

