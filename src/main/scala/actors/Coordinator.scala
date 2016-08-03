package actors

import java.util.concurrent.TimeUnit

import actors.Player.{GetPokemon, GetPokemonResult}
import akka.actor.{Actor, ActorLogging, Cancellable, Props, Scheduler}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import api.{GoogleProvider, NianticApi}
import api.NianticApi.{AuthSession, Location}
import akka.pattern.pipe

import scala.concurrent.duration._

/**
  * Created on 2016-07-31.
  */
class Coordinator(username: String, password: String, location: Location) extends Actor with ActorLogging {

  implicit val system = context.system
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val http = Http()
  var timer: Option[Cancellable] = None

  override def preStart(): Unit = {
    log.info("Authenticating")
    authenticate()
  }

  def receive: Receive = {
    case auth:AuthSession =>
      log.info("Got Session")
      log.info("Session: {}", auth)
      val player = context.actorOf(Player.props(auth, location), "player")

      timer.map(c => c.cancel())

      timer = Some(system.scheduler.schedule(
        0 seconds,
        10 seconds,
        player,
        GetPokemon(location)))

    case GetPokemonResult(pokemons) =>
      pokemons.foreach(p => log.info("Found {} at {} hidden at: {}ms", p.id, p.location, despawnTime(p.timeTillHiddenMs)))
  }

  def authenticate() = {
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
  def props(username: String, password: String, location: Location) = Props(new Coordinator(username, password, location))

  case class Search(location: Location)

}