package client

import java.io.IOException
import java.io.{InputStreamReader, PrintWriter}
import java.nio.CharBuffer
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.ReadOnlyBufferException
import java.util.Base64
import jnr.unixsocket.{UnixSocketAddress, UnixSocketChannel}

import scala.util.control.NoStackTrace
import scala.util.{Failure, Success, Try}
import scala.annotation.tailrec
import upickle.default._


/*
-------------- TODO ---------------------
1.finish working on an MVP 
2.create an HttpWrapper to Socket (X)
3.Test X-Registry-Auth
4.Fix request formatting (X)
5.Add Ssl/Tls Checking and connection
6.complete Delete, Head and Put Methods
7.tls connection logic
*/

case class Path(path: String)

type A = Map[String, String | Int | Boolean]

enum Method:
  case GET, POST, DELETE, HEAD, PUT

final case class Header(response: String = null,
  apiVersion: String = null,
  contentType: String = null,
  dockerExperimental: String = null,
  oStype: String = null,
  server: String = null,
  date: String = null,
  transferEncoding: String = null)

final case class Request(uri: String,
  host: String = null,
  params: A = null,
  filters: A = null,
  body: String = null,
  auth: Map[String, String] = null)

class HttpSocket(path: Path) extends Socket(path: Path) {
  def post(request: Request): (Option[Header], Option[String]) = sendAndReceive(request, Method.POST)
  def get(request: Request): (Option[Header], Option[String]) = sendAndReceive(request, Method.GET)
  def put(request: Request): (Option[Header], Option[String]) = sendAndReceive(request, Method.PUT)
  def delete(request: Request): (Option[Header], Option[String]) = sendAndReceive(request, Method.DELETE)


  // try to change mutable collection to immutable collection

  // remove empty params or filters from request
  private def formatRequest(request: Request, method: Method): String = {
    def decodeBase64(msg: String): String =
      val decoded = Base64.getDecoder.decode(msg)
      new String(decoded, StandardCharsets.UTF_8)

    def encodeBase64(auth: Map[String, String]): String =
      Base64.getEncoder.encodeToString(
        auth.map((x,y) => s""""$x":"$y"""".trim)
          .mkString("{", ",", "}")
          .getBytes(StandardCharsets.UTF_8)
        )

    def convertParams(params: A): Option[String] =
      Try(params.map((x,y) => s"""$x=$y""".trim).mkString("?", "&", "")) match
        case Success(params) => Some(params)
        case Failure(e) =>
          println("[Client.convertParams] failed to convert parameters from Map to url params"); Some("")

    def convertFilters(filters: A): Option[String] =
      if (filters == Map()) Some("")
      else {Try(filters.map((x,y) => s""""$x":["$y"]""".trim).mkString("{", ",", "}")) match
        case Success(filters) => println(s"&filters=$filters");Some(filters)
        case Failure(e) =>
          println("[Client.convertFilters] failed to convert filters from Map to JsonString"); Some("")}

    request match
      case Request(endpoint, host, null, null, null, null) =>
        s"$method $endpoint HTTP/1.1\r\nHost: $host\r\n\r\n"

      case Request(endpoint, host, params, null, null, null) =>
        s"$method $endpoint${convertParams(params).get} HTTP/1.1\r\nHost: $host\r\n\r\n"

      case Request(endpoint, host, params, filters, null, null) =>
        s"$method $endpoint${convertParams(params).get}${convertFilters(filters).get} HTTP/1.1\r\nHost: $host\r\n\r\n"

      case Request(endpoint, host, params, null, body, null) =>
        s"$method $endpoint${convertParams(params).get} HTTP/1.1\r\nHost: $host\r\nContent-Type: application/json\r\nContent-Length: ${body.length}\r\n\r\n$body"

      case Request(endpoint, host, params, null, body, auth) =>
        s"""$method $endpoint${convertParams(params).get}
        HTTP/1.1\r\nHost: $host\r\n
        X-Registry-Auth: ${encodeBase64(auth)}\r\n
        Content-Type: application/json\r\n
        Content-Length: ${body.length}\r\n\r\n$body""".trim
  }


  private def parseHeader(headerString: String): Option[Header] = {
    def mapToHeader(headerMap: collection.mutable.Map[String, String]) : Option[Header] = {
      val obj = Try{Header(headerMap.getOrElse("HTTP", "noHTTP"),
                           headerMap.getOrElse("Api-Version", "noAPIv"),
                           headerMap.getOrElse("Content-Type", "noContentType"),
                           headerMap.getOrElse("Docker-Experimental", "noDockerExpr"),
                           headerMap.getOrElse("Ostype", "noOstype"),
                           headerMap.getOrElse("Server", "noServer"),
                           headerMap.getOrElse("Date", "noDate"),
                           headerMap.getOrElse("Transfer-Encoding", "notTransferEncoding"))}
      obj match {
        case Success(obj) => Some(obj)
        case Failure(e) => println(s"[HTTP]: Failed to parse header with value: $headerMap"); None
      }
    }

    val headerMap: collection.mutable.Map[String, String] = collection.mutable.Map()
    headerString.split("\r\n").foreach{ line =>
      val keyValues = line.split(":")
      keyValues.length match {
        case 1 =>
          headerMap("HTTP") = keyValues.head.trim
        case _ =>
          headerMap(keyValues.head.trim) =
            keyValues.tail.reduce((c, c2) => if (keyValues.tail.length > 1)  c.trim + ":" + c2.trim
                                 else c.trim + c2.trim)
      }
    }

    mapToHeader(headerMap)
  }

  // receive
  private def sendAndReceive(request: Request, method: Method): (Option[Header], Option[String]) = {
    //println(request)
    val req = formatRequest(request, method)
    println(req)
    write(req)
    val response = read()
    println(response)
    val headerString = response.substring(0, response.indexOf("\r\n\r\n"))
    val body = response.substring(response.indexOf("\r\n\r\n"), response.length)

    // check if "[" is existing and its before "{" same thing for "{"
    (body.contains("["), body.contains("{")) match {
      case (true, false) => (parseHeader(headerString),
                              Some(body.substring(body.indexOf("["), body.lastIndexOf("]") + 1)))
      case (false, true) =>
        (parseHeader(headerString), Some(body.substring(body.indexOf("{"), body.lastIndexOf("}") + 1)))
      case (false, false) => (parseHeader(headerString), None)
      case (true, true) =>
        if body.indexOf("[") < body.indexOf("{") then
          (parseHeader(headerString), Some(body.substring(body.indexOf("["), body.lastIndexOf("]") + 1)))
        else (parseHeader(headerString), Some(body.substring(body.indexOf("{"), body.lastIndexOf("}") + 1)))
    }
  }

  def close(): Unit = release()
}

class Socket(path: Path){
  private val _file = Try(new java.io.File(path.path)) match {
    case Success(fileAddress) => fileAddress
    case Failure(e) => e match {
      case e: NullPointerException =>
        throw new IOException(s"[Socket]: Could not find file $path\n$e") with NoStackTrace
      case e: IllegalArgumentException =>
        throw new IOException(s"[Socket]: Could not find file $path\n$e") with NoStackTrace
    }
  }
  private val _socketAddress = new UnixSocketAddress(_file)
  private val _channel = UnixSocketChannel.open(_socketAddress)
  private val _reader = new InputStreamReader(Channels.newInputStream(_channel))
  private val _writer = new PrintWriter(Channels.newOutputStream(_channel))

  @tailrec
  private final def recurseReader(reader: InputStreamReader,
                                  buffer: CharBuffer,
                                  result: StringBuilder,
                                  canRead: Boolean,
                                  endOfBuffer: Int): String = {
      if (!canRead && endOfBuffer < 2048) result.toString
      else {
        val maybeEob = Try(reader.read(buffer)) match {
          case Success(endOfBuffer) => endOfBuffer
          case Failure(e) => e match {
            case e: IOException => throw new IOException
            case e: NullPointerException => throw new NullPointerException
            case e: ReadOnlyBufferException => throw new ReadOnlyBufferException
          }
        }
        val canReadFromSocket = Try(reader.ready) match {
          case Success(canReadInfo) => canReadInfo
          case Failure(e) => throw new IOException
        }

        result ++= buffer.flip.toString
        recurseReader(reader, buffer, result, canReadFromSocket, maybeEob)
      }
  }

  protected def read(): String = recurseReader(_reader, CharBuffer.allocate(2048), StringBuilder(), true, 0)
  protected def write(request: String): Unit = { _writer.write(request); _writer.flush() }
  protected def release(): Unit = { _channel.close(); _writer.close(); _reader.close() }
}

