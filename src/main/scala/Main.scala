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
import POGOProtos.Networking.Responses.GetPlayerProfileResponse.GetPlayerProfileResponse
import POGOProtos.Networking.Responses.GetPlayerResponse.GetPlayerResponse
import POGOProtos.Networking.Responses.PlayerUpdateResponse.PlayerUpdateResponse
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import api.{GoogleProvider, NianticApi}
import api.NianticApi.Location
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.util.control.NonFatal

/**
  * Created on 2016-07-26.
  */

object Main extends App {

  implicit val system = ActorSystem("streams")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  val unionStation = Location(43.6452, -79.3806)
  val email = ""
  val password = ""

  val inventoryDataFuture = for {
    providerSession <- GoogleProvider.login(email, password)
    nianticSession <- NianticApi.authenticate(providerSession, unionStation)
    api = new NianticApi(nianticSession, unionStation)
    inventory <- api.getInventory()
  } yield inventory.getInventoryDelta

  inventoryDataFuture.onSuccess({
    case delta => println(delta)
  })
}