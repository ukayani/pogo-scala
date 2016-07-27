import POGOProtos.Networking.Envelopes.RequestEnvelope.RequestEnvelope
import POGOProtos.Networking.Envelopes.RequestEnvelope.RequestEnvelope.AuthInfo
import POGOProtos.Networking.Envelopes.RequestEnvelope.RequestEnvelope.AuthInfo.JWT
import POGOProtos.Networking.Requests.Messages.DownloadSettingsMessage.DownloadSettingsMessage
import POGOProtos.Networking.Requests.Request.Request
import POGOProtos.Networking.Requests.RequestType.RequestType
import com.google.protobuf.ByteString
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

/**
  * Created on 2016-07-26.
  */
case class Location(lat: Double, lng: Double)

object Main extends App {
  import Req._

  val  headers = Map(
    "User-Agent" -> "Niantic App",
    "Accept" -> "*/*",
    "Content-Type" -> "application/x-www-form-urlencoded")

  val endpoint = "https://pgorelease.nianticlabs.com/plfe/rpc"

  val token = ""
  val settingsHash = "05daf51635c82611d1aac95c0b051d3ec088a930"
  val unionStation = Location(43.6452, 79.3806)

  val getPlayers = Request(RequestType.GET_PLAYER)
  val getHatchedEggs = Request(RequestType.GET_HATCHED_EGGS)
  val getInventory = Request(RequestType.GET_INVENTORY)
  val checkAwardedBadges = Request(RequestType.CHECK_AWARDED_BADGES)
  val downloadSettings = Request(RequestType.DOWNLOAD_SETTINGS, encode(DownloadSettingsMessage(settingsHash)))

  val initialRequests = List(getPlayers, getHatchedEggs, getInventory, checkAwardedBadges, downloadSettings)

  val req = createEnvelope(token, unionStation, initialRequests)

  println(req)

}

object Req {
  def encode(message: GeneratedMessage): ByteString = {
    ByteString.copyFrom(message.toByteArray)
  }

  def createEnvelope(authToken: String, loc: Location, requests: Seq[Request]) = {

    val statusCode = 2
    val requestId = 8145806132888207460L
    val unknown12 = 989
    val unknown2 = 59
    val authProvider = "google"

    val authInfo = AuthInfo(authProvider, Some(JWT(authToken, unknown2)))


    RequestEnvelope(statusCode, requestId, requests, None, loc.lat, loc.lng, 0.0, Some(authInfo), None, unknown12)
  }
}
