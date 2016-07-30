import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import api.{GoogleProvider, NianticApi}
import api.NianticApi.Location

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created on 2016-07-26.
  */

object Main extends App {

  implicit val system = ActorSystem("streams")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val http = Http()

  val unionStation = Location(43.6452, -79.3806)
  val email = "kayani.pogo@gmail.com"
  val password = "Md9-YtX-UnP-4NA"

  val mapData = for {
    providerSession <- GoogleProvider.login(email, password)
    nianticSession <- NianticApi.authenticate(providerSession, unionStation)
    api = new NianticApi(nianticSession, unionStation)
    cellIds = api.getCellIds(unionStation, 10)
    mapObjects <- api.getMapObjects(cellIds, List.fill(cellIds.size)(0L))
  } yield mapObjects

  mapData.onSuccess({
    case delta =>
      println("Num Cells " + delta.mapCells.size)
      println(delta.mapCells.flatMap(c => c.forts).map(_.id))
  })
}