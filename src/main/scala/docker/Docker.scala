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

// /v1.43/info
  /*--work on params and filters--*/
  def version(): Option[String] = {
    val req = Request("/v1.43/info", _host)
    val (header, body) = _http.get(req)
    body match {
    case Some(body) => Some(body)
    case None => None
    }
  }
  
  /* -------------- Images ------------*/

  // /v1.43/images/json
  /*--done working with this endpoint--*/
  def listImages(listAll: Boolean = false, sharedSize: Boolean = false, digets: Boolean = false): Option[List[Image]] =
                   send[List[Image]](Request(_imagesEndpoint, _host, Map("all" -> listAll,
                                                                         "shared-size" -> false,
                                                                         "digest" -> false)), _http.get)
  /*--done working with this endpoint--*/

  /* -------------- Networks ------------*/
  // /v1.43/networks
  /*--work on params and filters (filters are not working)--*/
  def listNetworks(filters: Map[String, String | Int | Boolean] = null) =
    send[List[Network]](Request(_networksEndpoint, _host, filters = filters), _http.get)
  /*--work on params and filters (filters are not working)--*/

  /* -------------- Volumes ------------*/
  // /v1.43/volumes
  /*--work on params and filters (filters are not working)--*/
  def listVolumes(filters: Map[String, String | Int | Boolean] = null) =
    send[List[Volumes]](Request(_volumesEndpoint, _host, filters = filters), _http.get)
  /*--work on params and filters (filters are not working)--*/


  /* -------------- Containers ------------*/
    // /v1.43/containers/create
    /*--work on params and filters--*/
    def createContainer(name: String,
                        hostname: String = null,
                        user: String = null,
                        config: PostContainer = null): Option[String] = {
      serialize[PostContainer](config) match {
        case Success(containerConfig) =>
          send[String](Request(_createContainersEndpoint, _host, Map("name" -> name), body = containerConfig), _http.post)
        case Failure(e) => println(s"[Docker.Container.createContainer]: Failed to create a container with error:\n$e"); None
      }
    }

  // /v1.43/containers/json by default it will list all
  def listContainers(listAll: Boolean = true,
                     limit: Int = 0,
                     getSize: Boolean = false,
                     filters: Map[String, String | Int | Boolean] = null): Option[List[Container]] = 
    send[List[Container]](Request(_containersEndpoint, _host, Map("all" -> listAll, "limit" -> limit, "size" -> getSize), filters), _http.get) 

  // for now just return the jsonString response
  // inspecting a container
  // /containers/<id>/json
  /*--work on params and filters--*/
  def inspectContainer(id: String = "", size: Boolean = false) = {
     if (id.isBlank) {
       // check this later
       throw new RuntimeException("please porvide the a container id")
       None
     }
     else send[String](Request("/v1.43/containers/"+id+"/json", _host, Map("size" -> size)), _http.get)
  }

  // listing processes running inside a container
  // /containers/<id>/top
  /*--work on params and filters--*/
  def top(id: String = "", psArgs: String = "") = {
     if (id.isBlank) {
       // check this later
       throw new RuntimeException("please porvide a container id")
       None
     }
     else send[Container](Request("/v1.43/containers/"+id+"/top", _host, Map("ps_args" -> psArgs)), _http.get)
  }

  // getting container logs by ID
  // /containers/<id>/logs
  // this needs to be a stream
  /*--work on params and filters--*/
  def logs(id: String = "",
           follow: Boolean = false,
           stdout: Boolean = false,
           stderr: Boolean = false,
           since: Int = 0,
           until: Int = 0,
           timestamps: Boolean = false,
           tail: String = "all") = {
     if (id.isBlank) {
       // check this later
       throw new RuntimeException("please porvide a container id")
       None
     }
     else send[Container](Request(s"/v1.43/containers/$id/logs", _host, Map("follow" -> follow,
                                                                              "stdout" -> stdout,
                                                                              "stderr" -> stderr,
                                                                              "since" -> since,
                                                                              "until" -> until,
                                                                              "timestamps" -> timestamps,
                                                                              "tail" -> tail)), _http.get)
  }

  // the output of this is to long and the format is not suitable
  def listFsChanges(id: String = "") = {
    if (id.isBlank()) {
      throw new RuntimeException("please provide a container id")
      None
    } else send[Container](Request(s"/v1.43/containers/$id/changes", _host), _http.get)
  }


  def exportContainer(id: String = "") = {
    if (id.isBlank()) {
      throw new RuntimeException("please provide a container id")
      None
    } else send[Container](Request(s"/v1.43/containers/$id/export", _host), _http.get)
  }

  def listProcesses(id: String = "") = { if (id.isBlank) {
      throw new RuntimeException("please provide ")
    }
  }

  def containerStats(id: String = "", stream: Boolean = true, oneShot: Boolean = false) = {
    if (id.isBlank) {
      throw new RuntimeException("please provide a container ID")
    } else send[Container](Request(s"/v1.44/containers/$id/stats", _host, Map("stream" -> stream, "one-shot" -> oneShot)), _http.get)
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
