package api

import Niantic.AuthSession
import POGOProtos.Inventory.Item.ItemId.ItemId
import POGOProtos.Networking.Envelopes.AuthTicket.AuthTicket
import POGOProtos.Networking.Envelopes.RequestEnvelope.RequestEnvelope
import POGOProtos.Networking.Envelopes.RequestEnvelope.RequestEnvelope.AuthInfo
import POGOProtos.Networking.Envelopes.RequestEnvelope.RequestEnvelope.AuthInfo.JWT
import POGOProtos.Networking.Envelopes.ResponseEnvelope.ResponseEnvelope
import POGOProtos.Networking.Requests.Messages.CatchPokemonMessage.CatchPokemonMessage
import POGOProtos.Networking.Requests.Messages.DownloadSettingsMessage.DownloadSettingsMessage
import POGOProtos.Networking.Requests.Messages.EncounterMessage.EncounterMessage
import POGOProtos.Networking.Requests.Messages.FortDetailsMessage.FortDetailsMessage
import POGOProtos.Networking.Requests.Messages.FortSearchMessage.FortSearchMessage
import POGOProtos.Networking.Requests.Messages.GetInventoryMessage.GetInventoryMessage
import POGOProtos.Networking.Requests.Messages.GetMapObjectsMessage.GetMapObjectsMessage
import POGOProtos.Networking.Requests.Messages.PlayerUpdateMessage.PlayerUpdateMessage
import POGOProtos.Networking.Requests.Request.Request
import POGOProtos.Networking.Requests.RequestType.RequestType
import POGOProtos.Networking.Responses.CatchPokemonResponse.CatchPokemonResponse
import POGOProtos.Networking.Responses.CheckAwardedBadgesResponse.CheckAwardedBadgesResponse
import POGOProtos.Networking.Responses.DownloadSettingsResponse.DownloadSettingsResponse
import POGOProtos.Networking.Responses.EncounterResponse.EncounterResponse
import POGOProtos.Networking.Responses.FortDetailsResponse.FortDetailsResponse
import POGOProtos.Networking.Responses.FortSearchResponse.FortSearchResponse
import POGOProtos.Networking.Responses.GetHatchedEggsResponse.GetHatchedEggsResponse
import POGOProtos.Networking.Responses.GetInventoryResponse.GetInventoryResponse
import POGOProtos.Networking.Responses.GetMapObjectsResponse.GetMapObjectsResponse
import POGOProtos.Networking.Responses.GetPlayerResponse.GetPlayerResponse
import POGOProtos.Networking.Responses.PlayerUpdateResponse.PlayerUpdateResponse
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

import scala.concurrent.{ExecutionContext, Future}

import Niantic._
/**
  * Created on 2016-07-28.
  */
class Niantic(session: AuthSession, location: Location)
             (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext) {


  import NianticResponse._
  import NianticRequests._

  def updateLocation(loc: Location) = new Niantic(session, loc)

  private def createRequestEnvelope(request: Request) =
    requestWithTicket(session.authTicket)
      .withLatitude(location.lat)
      .withLongitude(location.lng)
      .withRequests(List(request))

  private def send[A <: GeneratedMessage with Message[A]](responseParser: GeneratedMessageCompanion[A])(request: Request): Future[A] = {
    val req = createRequestEnvelope(request)
    sendRequest(session.host, session.path, req).map(handleResponse(_, responseParser))
  }

  def getPlayer = send(GetPlayerResponse)(Request(RequestType.GET_PLAYER))
  def getHatchedEggs = send(GetHatchedEggsResponse)(Request(RequestType.GET_HATCHED_EGGS))
  def getInventory = send(GetInventoryResponse)(Request(RequestType.GET_INVENTORY))
  def checkAwardedBadges = send(CheckAwardedBadgesResponse)(Request(RequestType.CHECK_AWARDED_BADGES))
  def downloadSettings = send(DownloadSettingsResponse)(Request(RequestType.DOWNLOAD_SETTINGS,
    encodeMessage(DownloadSettingsMessage(settingsHash))))

  def playerUpdate() =
    send(PlayerUpdateResponse)(Request(RequestType.PLAYER_UPDATE,
      encodeMessage(PlayerUpdateMessage(location.lat, location.lng))))

  def getInventory(lastTimestamp: Long = 0) =
    send(GetInventoryResponse)(Request(RequestType.GET_INVENTORY, encodeMessage(GetInventoryMessage(lastTimestamp))))

  def fortSearch(fortId: String, fortLocation: Location) =
    send(FortSearchResponse)(Request(RequestType.FORT_SEARCH,
      encodeMessage(FortSearchMessage(fortId, location.lat, location.lng, fortLocation.lat,
        fortLocation.lng))))

  def encounter(encounterId: Long, spawnPointId: String) =
    send(EncounterResponse)(Request(RequestType.ENCOUNTER, encodeMessage(EncounterMessage(encounterId, spawnPointId,
      location.lat, location.lng))))


  def catchPokemon(encounterId: Long, pokeballItemId: ItemId, normalizedReticleSize: Double, spawnPointId: String,
                   hitPokemon: Boolean, spinModifier: Double, normalizedHitPosition: Double) =
    send(CatchPokemonResponse)(Request(RequestType.CATCH_POKEMON,
      encodeMessage(CatchPokemonMessage(encounterId, pokeballItemId,
        normalizedReticleSize, spawnPointId, hitPokemon, spinModifier, normalizedHitPosition))))


  def fortDetails(fortId: String, fortLocation: Location) =
    send(FortDetailsResponse)(Request(RequestType.FORT_DETAILS,
      encodeMessage(FortDetailsMessage(fortId, fortLocation.lat, fortLocation.lng))))

  def getMapObjects(cellIds: Seq[Long], sinceTimestamps: Seq[Long]) =
    send(GetMapObjectsResponse)(Request(RequestType.GET_MAP_OBJECTS,
      encodeMessage(GetMapObjectsMessage(cellIds, sinceTimestamps, location.lat, location.lng))))

}

object Niantic {

  import NianticRequests._

  case class Location(lat: Double, lng: Double)
  case class AuthSession(host: String, path: String, authTicket: AuthTicket)

  val EntryHost = "pgorelease.nianticlabs.com"
  val EntryPath = "/plfe/rpc"

  private val requestEnvelope = {

    // magic values from APK
    val statusCode = 2
    val requestId = 8145806132888207460L
    val unknown12 = 989

    RequestEnvelope(
      statusCode = statusCode,
      requestId = requestId,
      unknown12 = unknown12
    )
  }

  private def requestWithToken(provider: String, token: String) = {
    val unknown2 = 59
    requestEnvelope.withAuthInfo(AuthInfo(provider, Some(JWT(token, unknown2))))
  }

  private def requestWithTicket(ticket: AuthTicket) = {
    requestEnvelope.withAuthTicket(ticket)
  }

  // Retrieve new session details with provided token
  def authenticate(provider: String, token: String, location: Location)
                  (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Future[AuthSession] = {

    // send intial batch of requests (these do not get a response)
    val getPlayer = Request(RequestType.GET_PLAYER)
    val getHatchedEggs = Request(RequestType.GET_HATCHED_EGGS)
    val getInventory = Request(RequestType.GET_INVENTORY)
    val checkAwardedBadges = Request(RequestType.CHECK_AWARDED_BADGES)
    val downloadSettings = Request(RequestType.DOWNLOAD_SETTINGS, encodeMessage(DownloadSettingsMessage(settingsHash)))

    val req = requestWithToken(provider, token)
      .withLatitude(location.lat)
      .withLongitude(location.lng)
      .withRequests(
        List(getPlayer, getHatchedEggs, getInventory, checkAwardedBadges, downloadSettings)
      )

    // extract new api URL from response and the auth ticket
    sendRequest(EntryHost, EntryPath, req)
      .map(r => {
        val List(domain, path) = r.apiUrl.split("/", 2).toList
        AuthSession(domain, s"/$path/rpc", r.authTicket.get)
      })
  }

  def sendRequest(host: String, path: String, request: RequestEnvelope)
                 (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Future[ResponseEnvelope] = {

    val requestHeaders = List(
      headers.`User-Agent`("Niantic App"),
      headers.Accept(List(MediaRanges.`*/*`))
    )

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(host)

    Source
      .single(HttpRequest(HttpMethods.POST, uri = path, entity = HttpEntity(request.toByteArray), headers = requestHeaders))
      .via(connectionFlow)
      .runWith(Sink.head)
      .flatMap({
        case HttpResponse(StatusCodes.OK, headers, entity, _) =>
          entity.dataBytes.runFold(ByteString(""))(_ ++ _)
        case HttpResponse(code, _, _, _) =>
          Future.failed(new Exception("Server responded with error"))
      })
      .map(encodedByteString => ResponseEnvelope.parseFrom(encodedByteString.toArray))
  }
}


object NianticRequests {

  import com.google.protobuf.{ByteString => BString}

  def encodeMessage(message: GeneratedMessage): BString = {
    BString.copyFrom(message.toByteArray)
  }

  val settingsHash = "05daf51635c82611d1aac95c0b051d3ec088a930"

}

object NianticResponse {

  def handleResponse[A <: GeneratedMessage with Message[A]]
  (res: ResponseEnvelope, companionA: GeneratedMessageCompanion[A]): A = {
    companionA.parseFrom(res.returns.head.toByteArray)
  }

  def handleResponse[A1 <: GeneratedMessage with Message[A1],
  A2 <: GeneratedMessage with Message[A2]](res: ResponseEnvelope, c1: GeneratedMessageCompanion[A1],
                                           c2: GeneratedMessageCompanion[A2]): (A1, A2) = {
    if (res.returns.length != 2) throw new IllegalArgumentException("Response returns size does not match request")
    else (c1.parseFrom(res.returns.head.toByteArray), c2.parseFrom(res.returns(1).toByteArray))
  }
}