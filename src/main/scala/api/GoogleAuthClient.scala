package api

import POGOProtos.Networking.Envelopes.ResponseEnvelope.ResponseEnvelope
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created on 2016-07-28.
  */
object GoogleAuthClient {



  val AuthHost = "android.clients.google.com"
  val AuthPath = "/auth"
  val UserAgent = "Dalvik/2.1.0 (Linux; U; Android 5.1.1; Andromax I56D2G Build/LMY47V"

  case class GoogleLoginResponse(androidId: String, masterToken: String)
  case class GoogleSession(token: String)

  def login(email: String, password: String, androidId: String)
           (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Future[GoogleLoginResponse] = {

    val data = Map(
      "accountType" -> "HOSTED_OR_GOOGLE",
      "Email" -> email.trim,
      "has_permission" -> "1",
      "add_account" -> "1",
      "Passwd" -> password,
      "service" -> "ac2dm",
      "source" -> "android",
      "androidId" -> androidId,
      "device_country" -> "us",
      "operatorCountry" -> "us",
      "lang" -> "en",
      "sdk_version" -> "17"
    )

    makeRequest(data).map(res => GoogleLoginResponse(androidId, res("Token")))
  }

  def oauth(email: String, masterToken: String, androidId: String, service: String, app: String, clientSig: String)
           (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Future[GoogleSession] = {

    val data = Map(
      "accountType" -> "HOSTED_OR_GOOGLE",
      "Email" -> email.trim,
      "EncryptedPasswd" -> masterToken,
      "has_permission" -> "1",
      "service" -> service,
      "source" -> "android",
      "androidId" -> androidId,
      "app" -> app,
      "client_sig" -> clientSig,
      "device_country" -> "us",
      "operatorCountry" -> "us",
      "lang" -> "en",
      "sdk_version" -> "17"
    )

    makeRequest(data).map(res => GoogleSession(res("Auth")))
  }

  private def makeRequest(data: Map[String, String])
                 (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Future[Map[String, String]] = {

    def makeQueryString(data: Map[String, String]): String =
      data.map({ case (key, value) => s"$key=$value"}).mkString("&")

    def parseResponse(data: String): Map[String, String] =
      data.split("\n").map(line => {
        val List(key, value) = line.trim.split("=", 2).toList
        (key, value)
      }).toMap

    val requestHeaders = List(headers.`User-Agent`(UserAgent))
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
      makeQueryString(data))

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(AuthHost)

    Source
      .single(HttpRequest(HttpMethods.POST, uri = AuthPath, entity = entity, headers = requestHeaders))
      .via(connectionFlow)
      .runWith(Sink.head)
      .flatMap({
        case HttpResponse(StatusCodes.OK, headers, resEntity, _) =>
          resEntity.dataBytes.runFold(ByteString(""))(_ ++ _)
        case HttpResponse(code, _, _, _) =>
          Future.failed(new Exception("Server responded with error"))
      })
      .map(encodedByteString => parseResponse(encodedByteString.decodeString("utf-8")))
  }
}
