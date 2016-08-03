package actors

import actors.Coordinator.RequestLocation
import actors.Player._
import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import api.NianticApi
import api.NianticApi.AuthSession
import akka.pattern.pipe
import utils.Geo
import utils.Geo.Location

import scala.concurrent.duration._


/**
  * Created on 2016-07-30.
  */
class Player(authSession: AuthSession) extends Actor with ActorLogging {

  implicit val system = context.system
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val http = Http()
  val api = new NianticApi(authSession)
  var timer: Option[Cancellable] = None

  def receive: Receive = {
    case SearchInitiated =>
      log.info("Starting Search")
      timer = Some(system.scheduler.schedule(0 milliseconds, 10 seconds, self, Tick))
      context.become(searching)
  }

  def searching: Receive = {
    case GetPokemon(location) =>
      log.info("Getting pokemon")
      val cellIds = Geo.getCellIds(location, 10)

      val pokemons = for {
        mapObjects <- api.getMapObjects(location)(cellIds, List.fill(cellIds.size)(0L))
        pokemons = for {
          cells <- mapObjects.mapCells
          pokemon <- cells.wildPokemons
        } yield Pokemon(pokemon.pokemonData.get.pokemonId.toString,
          Location(pokemon.latitude, pokemon.longitude), pokemon.timeTillHiddenMs)
      } yield GetPokemonResult(pokemons)

      pokemons pipeTo sender()

    case Tick =>
      log.info("Tick")
      context.parent ! RequestLocation

    case SearchEnded =>
      log.info("Search Ended")
      timer.map(t => t.cancel())
      context.unbecome()
  }

}

object Player {
  def props(authSession: AuthSession) = Props(new Player(authSession))

  case class Pokemon(id: String, location: Location, timeTillHiddenMs: Long)

  case class GetPokemon(loc: Location)
  case class GetPokemonResult(pokemons: Seq[Pokemon])
  case object SearchInitiated
  case object SearchEnded
  case object Tick
}