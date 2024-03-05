package docker

import upickle.default._
import client._

import scala.util.{Failure, Success, Try}
import java.nio.CharBuffer
import geny.Generator.End

/*
-------------- TODO LONGTERM ---------------------
1. finish working on an MVP (X)
2. finish all case classes for Images and networks 
3. build methods for each parent class to fulfill each endpoint (X)
4. work on filters and parameters of each endpoint (X)
*/


/*
-------------- TODO ----------------------
1. fix params parsing to verify if param is set to default (if so the param will be ignored) else parse param
2. same thing for filters
3. see how can you parse different responses with different types and formats
4. finish all container related endpoints
*/



class Docker(path: String, hostAddress: String) {
  private lazy val _containersEndpoint = "/v1.43/containers/json"
  private lazy val _createContainersEndpoint = "/v1.43/containers/create"
  private lazy val _imagesEndpoint = "/v1.43/images/json"
  private lazy val _networksEndpoint = "/v1.43/networks"
  private lazy val _volumesEndpoint = "/v1.43/volumes"
  private lazy val _host = hostAddress
  private given Path(path)
  private val _http: HttpSocket = new HttpSocket

  private def deserialize[T: Reader](jsonString: String): Try[T] = Try(upickle.default.read[T](jsonString))
  private def serialize[T: Writer](caseClass: T): Try[String] = Try(upickle.default.write[T](caseClass))

  def close(): Unit = _http.close()

  // -- for now this will return the responseString if it failes to parse
  def send[T: Reader](requestBody: Request, request: (Request => (Option[Header], Option[String]))): Option[T] = {
    request(requestBody) match {
      case (Some(header), Some(body)) => deserialize[T](body) match {
        case Success(bodyContent) => Option(bodyContent)
        case Failure(e) => println(s"[Docker.send]: failed to deserialize \n $e"); None
      }
        case (Some(header), None) => println("got only header"); None
        case _ => println("something went wrong"); None
    }
  }

  /* GET */
  // /v1.43/info
  def version(): Option[String] = {
    val req = Request("/v1.43/info", _host)
    val (header, body) = _http.get(req)
    body match {
    case Some(body) => Some(body)
    case None => None
    }
  }
  
  /* -------------- Images ------------*/

//   /v1.43/images/json
  def listImages(): Option[List[Image]] = {
    send[List[Image]](Request(_imagesEndpoint, _host), _http.get)
  }

// /v1.43/networks
  def listNetworks(): Option[List[Network]] = {
    send[List[Network]](Request(_networksEndpoint, _host), _http.get)
  }

// /v1.43/volumes
  def listVolumes(): Option[List[Volumes]] = {
    send[List[Volumes]](Request(_volumesEndpoint, _host), _http.get)
  }


// this should be replaced by /container/json?id=<id>
  def getContainer(name: String = "",
                   id: String = "",
                   listAll: Boolean = true): Option[List[Container]] = {
    (name.isBlank, id.isBlank) match {
      case (false, true) =>
        send[List[Container]](Request(_containersEndpoint, _host, Map("all" -> listAll, "filters" -> s"""{"name":["$name"]}""")), _http.get)
      case (true, false) =>
        send[List[Container]](Request(_containersEndpoint, _host, Map("all" -> listAll, "filters" -> s"""{"id": ["$id"]}""")), _http.get)
      case _ => println(s"[docker:getContainer] please provide a container id or name"); null
    }
  }


// Container class
  case class Port(IP: String = "", PrivatePort: Int = 0,
                  PublicPort: Int = 0, Type: String = "") derives ReadWriter

  case class Driver(IPAMConfig: String, Links: String, Aliases: String, NetworkID: String,
                    EndpointID: String, Gateway: String, IPAddress: String, IPPrefixLen: Int,
                    IPv6Gateway: String, GlobalIPv6Address: String, GlobalIPv6PrefixLen: Int, MacAddress: String,
                    DriverOpts: String) derives ReadWriter

  case class Mount(Type: String, Name: String, Source: String, Destination: String,
                   Driver: String, Mode: String, RW: Boolean, Propagation: String) derives ReadWriter

  case class Container(Id: String, Names: List[String], Image: String, ImageID: String,
    Command: String, Created: Long, State: String, Status: String,
    HostConfig: Map[String, String], NetworkSettings: Map[String, Map[String, Driver]],
    Ports: List[Port], Labels: Map[String, String], Mounts: List[Mount]) derives ReadWriter {
      private lazy val _endpoint = "/v1.43/containers/"

      // /v1.43/containers/{id}/kill
      def kill(): Unit = {
        if (this.Status.startsWith("Up")) {
          val req = Request(_endpoint+this.Id.substring(0, 12)+"/kill", _host)
          send[String](req, _http.post)
        }
      }
      
      // /v1.43/containers/{id}/start
      def start(): Unit = {
        if (this.Status.startsWith("Exited")) {
          val req = Request(_endpoint+this.Id.substring(0, 12)+"/start", _host)
          send[String](req, _http.post)
        }
      }

    // /v1.43/containers/{id}/restart
    //  def restart() = ???

    // /v1.43/containers/{id}
    //  def remove() = ???

    // /v1.43/containers/{id}/json
    //  def inspect() = ???

    // /v1.43/containers/{id}/logs
    //  def logs() = ???

      // /v1.43/containers/{id}/stats
      //  def stats() = ???

//    def stop(): Unit = sendRequest[String]("POST", s"${_endpoint}/${this.Id}/stop")

    // /v1.43/containers/{id}/start
//    def start(): Unit = sendRequest[String]("POST", s"${_endpoint}/${this.Id}/start")
  }
  // Container class

  // Image class
  case class Image(Id: String,
                   ParentId: String,
                   RepoTags: List[String], RepoDigests: List[String],
                   Created: String,
                   Size: Long,
                   SharedSize: Long,
                   VirtualSize: Long,
                   Labels: Map[String, String],
                   Containers: Int) derives ReadWriter
  // Image class

  // Network class
  case class Ipam(Driver: String = "",
                  Option: String = "",
                  Config: List[Map[String, String]] = List(Map())) derives ReadWriter

  case class Network(Name: String,
                     Id: String = "",
                     Created: String = "",
                     Scope: String = "",
                     Driver: String = "",
                     EnableIPv6: Boolean,
                     IPAM: Ipam,
                     Internal: Boolean,
                     Ingress: Boolean,
                     ConfigFrom: Map[String, String] = Map(),
                     ConfigOnly: Boolean,
                     Containers: Map[String, String] = Map(),
                     Options: Map[String, String] = Map(),
                     Labels: Map[String, String] = Map()) derives ReadWriter
  // Network class

  // Volumes class
  case class AccessibilityRequirements(Requisite: List[Map[String, String]],
                                       Preferred: List[Map[String, String]]) derives ReadWriter

  case class AccessMode(Scope: String,
                        Sharing: String,
                        MountVolume: Map[String, String],
                        Secrets: List[Map[String, String]],
                        AccessibilityRequirements: AccessibilityRequirements,
                        CapacityRange: Map[String, Int],
                        Availibility: String) derives ReadWriter

  case class Spec(Group: String, AccessMode: AccessMode) derives ReadWriter

  case class Info(CapacityBytes: Int,
                  VolumeContext: Map[String, String],
                  VolumeID: String,
                  AccessibleTopology: List[Map[String, String]]) derives ReadWriter

  case class PublishStatus(NodeID: String, State: String, PublishContext: Map[String, String]) derives ReadWriter

  case class ClusterVolume(ID: String,
                           Version: Map[String, Long],
                           CreatedAt: String,
                           UpdatedAt: String,
                           Spec: Spec,
                           Info: Info,
                           publishStatus: List[PublishStatus],
                           Options: Map[String, String],
                           UsageData: Map[String, Int]) derives ReadWriter

  case class Volumes(CreatedAt: String,
                     Driver: String,
                     Labels: Map[String, String],
                     Mountpoint: String,
                     Name: String,
                     Options: Map[String, String] = null,
                     Scope: String) derives ReadWriter


  case class VolumesWrapper(Volumes: List[Volumes], Warnings: List[String] = null) derives ReadWriter

  // Volumes class
  
  case class HealthConfig(Test: List[String],
                          Interval: Int,
                          Timeout: Int,
                          Retries: Int,
                          StartPeriod: Int) derives ReadWriter

  case class BlkioWeightDevice(Path: String, Weight: Int) derives ReadWriter

  case class HostConfig(CpuShares: Int,
                        Memory: Int,
                        CgroupParent: String,
                        BlkioWeight: Int,
                        BlkioWeightDevice: BlkioWeightDevice) derives ReadWriter

  case class IPAMConfig(IPv4Address: String, IPv6Address: String, LinkLocalIPs: List[String]) derives ReadWriter

  case class EndpointSettings(IPAMConfig: IPAMConfig,
                              Links: List[String],
                              Aliases: List[String],
                              NetworkID: String,
                              EndpointID: String,
                              Gateway: String,
                              IPAddress: String,
                              IPPrefixLen: Int,
                              IPv6Gateway: String,
                              GlobalIPv6PrefixLen: Int,
                              MacAddress: String,
                              DriverOpts: Map[String, String]) derives ReadWriter

  case class PostContainer(Hostname: String = "",
                           Domainname: String = "",
                           User: String = "",
                           AttachStdin: Boolean = false,
                           AttachStdout: Boolean = true,
                           AttachStderr: Boolean = true,
                           Tty: Boolean = false,
                           OpenStdin: Boolean = false,
                           Env: List[String] = List(),
                           Cmd: List[String] = List(),
                           Entrypoint: String = "",
                           Image: String = "",
                           Labels: Map[String, String] = Map(),
                           Volumes: Map[String, Map[String, String]] = Map(),
                           WorkingDir: String = "",
                           NetworkDisabled: Boolean = false,
                           MacAddress: String = "",
                           ExposedPorts: Map[String, Map[String, String]] = Map(),
                           StopSignal: String = "",
                           StopTimeout: Int = 10,
                           HostConfig: HostConfig = null,
//                           HealthConfig: HealthConfig = null,
//                           ArgsEscaped: Boolean,
//                           OnBuild: List[String] = null,
//                           Shell: List[String] = null,
                           NetworkingConfig: Map[String, EndpointSettings] = Map()) derives ReadWriter
}
