package actors

import java.util.concurrent.TimeUnit

import actors.Coordinator.{Authenticate, RequestLocation, Search}
import actors.Player.{GetPokemon, GetPokemonResult, SearchEnded, SearchInitiated}
import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import api.{GoogleProvider, NianticApi}
import api.NianticApi.AuthSession
import akka.pattern.pipe
import utils.Geo
import utils.Geo.Location


/**
  * Created on 2016-07-31.
  */
class Coordinator extends Actor with ActorLogging with Stash {

  implicit val system = context.system
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val http = Http()
  var players = List.empty[String]
  var searchQueue = List.empty[Location]

  def receive: Receive = {
    case auth:AuthSession =>
      log.info("Got Session")
      log.info("Session: {}", auth)
      players = auth.username :: players
      context.actorOf(Player.props(auth), auth.username)
      unstashAll()
      context.become(authenticated)

    case Authenticate(username, password, location) => {
      log.info("Authenticating")
      authenticate(username, password, location)
    }
    case _ => stash()
  }

  def authenticated: Receive = {
    case Search(location) => {
      searchQueue = Geo.generateHexagonRing(location, NianticApi.HeartBeatRadiusInKm, 4)
      context.children.foreach(c => c ! SearchInitiated)
    }

    case RequestLocation => {
      if (searchQueue.isEmpty) {
        sender() ! SearchEnded
      } else {
        val location = searchQueue.head
        log.info("Dequeing location {}", location)
        searchQueue = searchQueue.drop(1)
        sender() ! GetPokemon(location)
      }
    }

    case GetPokemonResult(pokemons) =>
      pokemons.foreach(p => log.info("Found {} at {} hidden at: {}ms", p.id, p.location, despawnTime(p.timeTillHiddenMs)))
  }

  def authenticate(username: String, password: String, location: Location) = {
    val authSession = for {
      providerSession <- GoogleProvider.login(username, password)
      authSession <- NianticApi.authenticate(providerSession, location)
    } yield authSession

    authSession pipeTo self
  }

  def despawnTime(timeInMillis: Long): String = {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInMillis))
    s"$minutes min, $seconds sec"
  }
}

object Coordinator {
  def props = Props(new Coordinator)

  case class Search(location: Location)
  case class Authenticate(username: String, password: String, location: Location)
  case object RequestLocation

}