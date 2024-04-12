package docker

import upickle.default._
import client.{HttpSocket, Header, Request, Path}
import scala.util.{Failure, Success, Try}
import java.net.HttpCookie

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

type Filters = Map[String, String | Int | Boolean]


private object Serializer {
  def deserialize[T: Reader](jsonString: String): Try[T] = Try(upickle.default.read[T](jsonString))
  def serialize[T: Writer](caseClass: T): Try[String] = Try(upickle.default.write[T](caseClass))
}

class HttpDockerClient(val path: Option[String], val host: Option[String]) {
  // parsing
  // interacting with HttpSocket class
  val socket = path match {
    case Some(path) => new HttpSocket(Path(path))
    case _ => throw new RuntimeException
  }

  val _host = host match {
    case Some(host) => host
    case _ => throw new RuntimeException
  }

  def send[T: Reader](requestBody: Request, request: (Request => (Option[Header], Option[String]))): Option[T] = {
    request(requestBody) match {
      case (Some(header), Some(body)) => Serializer.deserialize[T](body) match {
        case Success(bodyContent) => Option(bodyContent)
        case Failure(e) => println(s"[Docker.send]: failed to deserialize \n $e"); None
      }
        case (Some(header), None) => println("got only header"); None
        case _ => println("something went wrong"); None
    }
  }

  def close(): Unit = socket.close()
}


class Docker(implicit http: HttpDockerClient){
  private final val _containersEndpoint = "/v1.43/containers/json"
  private final val _createContainersEndpoint = "/v1.43/containers/create"
  private final val _imagesEndpoint = "/v1.43/images/json"
  private final val _networksEndpoint = "/v1.43/networks"
  private final val _volumesEndpoint = "/v1.43/volumes"



  // -- for now this will return the responseString if it failes to parse

// /v1.43/info
  /*--work on params and filters--*/
  def version(): Option[String] = {
    val req = Request("/v1.43/info", http._host)
    val (header, body) = http.socket.get(req)
    body match {
    case Some(body) => Some(body)
    case None => None
    }
  }
  
  /* -------------- Images ------------*/

  // /v1.43/images/json
  /*--done working with this endpoint--*/
  def listImages(listAll: Boolean = false, sharedSize: Boolean = false, digets: Boolean = false): Option[List[Image]] =
                   http.send[List[Image]](Request(_imagesEndpoint, http._host, Map("all" -> listAll,
                                                                         "shared-size" -> false,
                                                                         "digest" -> false)), http.socket.get)
  /*--done working with this endpoint--*/

  /* -------------- Networks ------------*/
  // /v1.43/networks
  /*--work on params and filters (filters are not working)--*/
  def listNetworks(filters: Filters = Map()): Option[List[Network]] =
    http.send[List[Network]](Request(_networksEndpoint, http._host, filters = filters), http.socket.get)
  /*--work on params and filters (filters are not working)--*/

  /* -------------- Volumes ------------*/
  // /v1.43/volumes
  /*--work on params and filters (filters are not working)--*/
  def listVolumes(filters: Filters = Map()): Unit =
    http.send[List[Volumes]](Request(_volumesEndpoint, http._host, filters = filters), http.socket.get)
  /*--work on params and filters (filters are not working)--*/


  /* -------------- Containers ------------*/
    // /v1.43/containers/create
    /*--work on params and filters--*/
    def createContainer(name: String, hostname: String = "", user: String = "", config: PostContainer = null): Option[String] = {
      Serializer.serialize[PostContainer](config) match {
        case Success(containerConfig) =>
          http.send[String](Request(_createContainersEndpoint, http._host, Map("name" -> name), body = containerConfig), http.socket.post)
        case Failure(e) => println(s"[Docker.Container.createContainer]: Failed to create a container with error:\n$e"); None
      }
    }

  // /v1.43/containers/json by default it will list all
  def listContainers(listAll: Boolean = true, getSize: Boolean = false, filters: Filters = Map()): Option[List[Container]] = {
    http.send[List[ContainerState]](Request(_containersEndpoint, http._host, Map("all" -> listAll, "size" -> getSize),
      filters), http.socket.get) match {
        case Some(containers) => {
          Option(containers.map(x => 
              new Container(http, x)
              ))
        }
        case _ => Option(List())
      }
  }

  // for now just return the jsonString response
  // inspecting a container
  // /containers/<id>/json
  /*--work on params and filters--*/
  def inspectContainer(id: String = "", size: Boolean = false): Option[String] = {
     if (id.isEmpty) {
       // check this later
       throw new RuntimeException("please porvide the a container id")
       None
     }
     val (header, body) = http.socket.get(Request("/v1.43/containers/"+id+"/json", http._host, Map("size" -> size)))
     body match {
       case Some(body) => Some(body)
       case None => None
     }
  }

  // get container by name or Id

  // listing processes running inside a container
  // /containers/<id>/top
  /*--work on params and filters--*/
  def top(id: String = "", psArgs: String = ""): Option[String] = {
     if (id.isEmpty) {
       // check this later
       throw new RuntimeException("please porvide a container id")
       None
     }
     val (header, body) = http.socket.get(Request("/v1.43/containers/"+id+"/top", http._host, Map("ps_args" -> psArgs)))
     body match {
       case Some(body) => Some(body)
       case None => None
     }
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
           tail: String = "all"): Option[String] = {
     if (id.isEmpty) {
       // check this later
       throw new RuntimeException("please porvide a container id")
       None
     }

     val req = Request(s"/v1.43/containers/$id/logs", http._host, Map("follow" -> follow,
       "stdout" -> stdout,
       "stderr" -> stderr,
       "since" -> since,
       "until" -> until,
       "timestamps" -> timestamps,
       "tail" -> tail))
     val (header, body) = http.socket.get(req)
     body match {
       case Some(body) => Some(body)
       case None => None
     }
  }

  // the output of this is to long and the format is not suitable
  def listFsChanges(id: String = ""): Option[String] = {
    if id.isEmpty then throw new RuntimeException("please provide a container id")
    else {
     val (header, body) = http.socket.get(Request(s"/v1.43/containers/$id/changes", http._host))
     body match {
       case Some(body) => Some(body)
       case None => None
     }
    }
  }


  def exportContainer(id: String = ""): Option[String] = {
    if id.isEmpty then throw new RuntimeException("please provide a container id")
    else {
     val (header, body) = http.socket.get(Request(s"/v1.43/containers/$id/export", http._host))
     body match {
       case Some(body) => Some(body)
       case None => None
     }
    }
  }

  def listProcesses(id: String = ""): Unit = { if (id.isEmpty) {
      throw new RuntimeException("please provide ")
    }
  }

  def containerStats(id: String = "", stream: Boolean = true, oneShot: Boolean = false): Option[Container] = {
    if (id.isEmpty) {
      throw new RuntimeException("please provide a container ID")
    } else {
      http.send[ContainerState](Request(
        s"/v1.44/containers/$id/stats", http._host, Map("stream" -> stream, "one-shot" -> oneShot)
      ), http.socket.get) match {
        case Some(res) =>
          Option(new Container(http, res))
        case _ => throw new RuntimeException
      }
    }
  }

  // Container class
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
                    //

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
                     Options: Map[String, String] = Map(),
                     Scope: String) derives ReadWriter

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
                           NetworkingConfig: Map[String, EndpointSettings] = Map()) derives ReadWriter
}
