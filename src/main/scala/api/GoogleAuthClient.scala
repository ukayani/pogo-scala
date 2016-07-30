package api

import akka.http.scaladsl.{HttpExt}
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, _}
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created on 2016-07-28.
  */
class GoogleAuthClient(implicit http: HttpExt, mat: ActorMaterializer, ec: ExecutionContext) {

  val AuthUri = "https://android.clients.google.com/auth"
  val UserAgent = "Dalvik/2.1.0 (Linux; U; Android 5.1.1; Andromax I56D2G Build/LMY47V"

  case class GoogleLoginResponse(androidId: String, masterToken: String)
  case class GoogleSession(token: String)

  def login(email: String, password: String, androidId: String): Future[GoogleLoginResponse] = {

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

  def oauth(email: String, masterToken: String, androidId: String, service: String, app: String, clientSig: String): Future[GoogleSession] = {

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

  private def makeRequest(data: Map[String, String]): Future[Map[String, String]] = {

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

    http.singleRequest(HttpRequest(HttpMethods.POST, uri = AuthUri, entity = entity, headers = requestHeaders))
      .flatMap({
        case HttpResponse(StatusCodes.OK, headers, resEntity, _) =>
          resEntity.dataBytes.runFold(ByteString(""))(_ ++ _)
        case HttpResponse(code, _, _, _) =>
          Future.failed(new Exception("Server responded with error"))
      })
      .map(encodedByteString => parseResponse(encodedByteString.decodeString("utf-8")))
  }
}
