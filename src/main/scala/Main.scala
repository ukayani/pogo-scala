import actors.Coordinator
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import api.{GoogleProvider, NianticApi}
import api.NianticApi.Location
import com.typesafe.config.ConfigFactory
import utils.Geo

/**
  * Created on 2016-07-26.
  */

object Main extends App {

  implicit val system = ActorSystem("streams")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val http = Http()

  val config = ConfigFactory.load("credentials")

  val harbourFront = Location(43.638069, -79.380269)
  val email = config.getString("account.username")
  val password = config.getString("account.password")

  val coordinator = system.actorOf(Coordinator.props(email, password, harbourFront), "coordinator")

}