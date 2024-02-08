package client

import java.io.IOException
import java.io.{InputStreamReader, PrintWriter}
import java.nio.CharBuffer
import java.nio.channels.Channels

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.nio.ReadOnlyBufferException
import jnr.unixsocket.{UnixSocketAddress, UnixSocketChannel}

import scala.annotation.tailrec
import scala.util.control.NoStackTrace
import scala.util.{Failure, Success, Try}
import upickle.default._

import client.Method.GET

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
  params: Map[String, Boolean | String | Int] = null,
  body: String = null,
  auth: Map[String, String] = null)

class HttpSocket(implicit path: Path) extends Socket(path) {
  def post(request: Request): (Option[Header], Option[String]) = sendAndReceive(request, Method.POST)
  def get(request: Request): (Option[Header], Option[String]) = sendAndReceive(request, Method.GET)
  def put(request: Request): (Option[Header], Option[String]) = sendAndReceive(request, Method.PUT)

  private def mapToHeader(headerMap: collection.mutable.Map[String, String]) : Option[Header] = {
    val obj = Try {Header(
      headerMap.getOrElse("HTTP", "noHTTP"),
      headerMap.getOrElse("Api-Version", "noAPIv"),
      headerMap.getOrElse("Content-Type", "noContentType"),
      headerMap.getOrElse("Docker-Experimental", "noDockerExpr"),
      headerMap.getOrElse("Ostype", "noOstype"),
      headerMap.getOrElse("Server", "noServer"),
      headerMap.getOrElse("Date", "noDate"),
      headerMap.getOrElse("Transfer-Encoding", "notTransferEcoding"))}

  def post(uri: String)(body: String): String = {
//    write(formatRequest())
    val (header, body) = parseResponse(read())
    header
  }

  def get(uri: String): String = {
//    write(formatRequest())
    val (header, body) = parseResponse(read())
    header
  }

  private def parseResponse(response: String): (String, Option[String]) = {
    // parses the response and returns the header and body
    val header = ""
    // parse the header and get the Content-type And length then decide what to do with the body if it exists
    val body = if (response.indexOf("[") < response.indexOf("{")) {
      Try(response.substring(response.indexOf("["), response.lastIndexOf("0"))) match {
        case Success(json) => json
        case Failure(e) => println(s"$e maybe empty string check request format"); """{"failed": "failed to parse body"}"""
      }
    } else {
      Try(response.substring(response.indexOf("{"), response.lastIndexOf("0"))) match {
        case Success(json) => json
        case Failure(e) => println(s"$e maybe empty string check request format"); """{"failed": "failed to parse body"}"""
      }
    }

    (header, Option(body))
  }

//  private def formatRequest(host: String,
//                            method: String,
//                            request: String,
//                            body: String = "",
//                            auth: String = ""): String = {
//    (auth.nonEmpty, body.nonEmpty) match {
//      case (false, true) =>
//        s"""$method $request HTTP/1.1
//        \rHost: $host
//        \rContent-Type: application/json
//        \r\n\n$body\r\n\r\n""".stripMargin
//      case (true, false) =>
//        s"""$method $request HTTP/1.1
//        \rHost: $host
//        \rX-Registry-Auth: ${encodeBase64(auth)}\r\n\r\n""".stripMargin
//      case (true, true) =>
//        s"""$method $request HTTP/1.1
//        \rHost: $host
//        \rX-Registry-Auth: ${encodeBase64(auth)}
//        \rContent-Type: application/json
//        \r\n\n$body\r\n\r\n""".stripMargin
//      case _ =>
//    }
//  }
private def formatRequest(request: Request): String = {
  request match {
    case Request(method, host, req, null, null, null) => s"$method $req HTTP/1.1\r\nHost: $host\r\n\r\n"
    case Request(method, host, req, params, null, null) => {
      val sParams = params.map((x,y) => s"""$x=$y""".trim).mkString("?", "&", "")
      s"$method $req$sParams HTTP/1.1\r\nHost: $host\r\n\r\n"
    }
    case Request(method, host, req, params, body, null) => {
      val sParams = params.map((x, y) => s"""$x=$y""".trim).mkString("?", "&", "")
      s"""$method $req$sParams
         HTTP/1.1\r\nHost: $host\r\n
         Content-Type: application/json
         \r\nContent-Length:${body.length}\r\n\r\n$body\r\n\r\n""".trim
    }
    case Request(method, host, req, params, body, auth) => {
      val sParams = params.map((x, y) => s"""$x=$y""".trim).mkString("?", "&", "")
      s"""$method $req$sParams
         HTTP/1.1\r\nHost: $host\r\n
         X-Registry-Auth: ${encodeBase64(auth)}\r\n
         Content-Type: application/json\r\n
         Content-Length:${body.length}\r\n\r\n$body\r\n\r\n""".trim
    }
  }
}


  private def encodeBase64(auth: Map[String, String]): String =
    Base64.getEncoder.encodeToString(
      auth.map((x,y) => s""""$x":"$y"""".trim)
        .mkString("{", ",", "}")
        .getBytes(StandardCharsets.UTF_8)
    )
  private def decodeBase64(msg: String): String = {
    val decoded = Base64.getDecoder.decode(msg)
    new String(decoded, StandardCharsets.UTF_8)
  }

  def close(): Unit = release()
}

class Socket(path: Path){
  private val _file = Try(new java.io.File(path.path)) match {
    case Success(fileAddress) => fileAddress
    case Failure(e) => e match {
      case e: NullPointerException => throw new IOException(s"Could not find file $path\n$e") with NoStackTrace
      case e: IllegalArgumentException => throw new IOException(s"Could not find file $path\n$e") with NoStackTrace
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
  protected def write(request: String): Unit = _writer.write(request); _writer.flush()

  protected def release(): Unit = {
    _channel.close()
    _writer.close()
    _reader.close()
  }
}
