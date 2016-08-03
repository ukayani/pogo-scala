package actors

import actors.Player.{GetPokemon, GetPokemonResult, Pokemon}
import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import api.{NianticApi}
import api.NianticApi.{AuthSession, Location}

import akka.pattern.pipe

/**
  * Created on 2016-07-30.
  */
class Player(authSession: AuthSession) extends Actor with ActorLogging {

  implicit val system = context.system
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val http = Http()
  val api = new NianticApi(authSession)

  def receive: Receive = {
    case GetPokemon(loc) =>
      val cellIds = api.getCellIds(loc, 10)

      val pokemons = for {
        mapObjects <- api.getMapObjects(loc)(cellIds, List.fill(cellIds.size)(0L))
        pokemons = for {
          cells <- mapObjects.mapCells
          pokemon <- cells.wildPokemons
        } yield Pokemon(pokemon.pokemonData.get.pokemonId.toString,
          Location(pokemon.latitude, pokemon.longitude), pokemon.timeTillHiddenMs)
      } yield GetPokemonResult(pokemons)

      pokemons pipeTo sender()
  }

}

object Player {
  def props(authSession: AuthSession, startingLocation: Location) = Props(new Player(authSession))

  case class GetPokemon(loc: Location)
  case class Pokemon(id: String, location: Location, timeTillHiddenMs: Long)
  case class GetPokemonResult(pokemons: Seq[Pokemon])
}