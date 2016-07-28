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
import api.Niantic
import api.Niantic.Location
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

  val unionStation = Location(43.7934712, -79.14967639999999)
  val token = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImE5NzAyMjQ0YWE3YjMyYTBjZjM4MWNjNjVhZDk4OGYyMzllYmIzOWYifQ.eyJpc3MiOiJhY2NvdW50cy5nb29nbGUuY29tIiwiYXVkIjoiODQ4MjMyNTExMjQwLTdzbzQyMWpvdHIyNjA5cm1xYWtjZXV1MWx1dXEwcHRiLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwic3ViIjoiMTAxODM4Nzk3NDgwMDQ3NTg0ODM3IiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImF6cCI6Ijg0ODIzMjUxMTI0MC0zdmRydHJmZG50bGpmMnU0bWxndG5ubGhuaWduMzVkNS5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsImVtYWlsIjoia2F5YW5pLnBvZ29AZ21haWwuY29tIiwiaWF0IjoxNDY5Njg1MDY2LCJleHAiOjE0Njk2ODg2NjZ9.neZZI8Ao2jHJL-_lNd4zAWepi39ME9U9DLRn6b2RPnS3X-WKwCjCARL3P3Y6f4jFRqB4u5A28CVdq-FZshdDOpjX7OBZwWxcMcBQA9qZ9csZd_T8Cf7csPXa0M0P4Qg79b9wrRuzLLao65Se5eFpocYaW02TfhSL4_muVwKWfvDp7ju2MX-JazjQEPPBb2BlBCRepTiYMJgu5_q5LqCDmmAyx2J0NdnVm1f_G5NKrfPLZLG0KtbwAU6R5fCqdNDgv6alfk6I5WHGr5Z3ZRMsETjQ94k3BqTZl9x31wLjxC7xtpswmfxQiWzQiOfHmi8_DYNwzczXSPWvSxVzCIqCoQ"

  val playerDataFuture = for {
    session <- Niantic.authenticate("google", token, unionStation)
    api = new Niantic(session, unionStation)
    player <- api.getPlayer
  } yield player.playerData

  playerDataFuture.onSuccess({
    case Some(playerData) => println(playerData)
  })

}