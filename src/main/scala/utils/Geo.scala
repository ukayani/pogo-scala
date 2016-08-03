package utils

import com.google.common.geometry.{S2CellId, S2LatLng}

/**
  * Created on 2016-08-02.
  */
object Geo {

  case class Location(lat: Double, lng: Double) {
    def toRadians:(Double, Double) = (math.toRadians(lat), math.toRadians(lng))
  }

  object Location {
    def fromRadians(lat: Double, lng: Double): Location = Location(math.toDegrees(lat), math.toDegrees(lng))
  }


  val EarthRadiusInKm = 6378.1

  object Bearing {
    val North = math.toRadians(0.0)
    val East = math.toRadians(90.0)
    val South = math.toRadians(180.0)
    val West = math.toRadians(270.0)
  }

  type Kilometers = Double
  type Radians = Double

  def getCellIds(location: Location, radius: Int) = {
    val cellId = S2CellId.fromLatLng(S2LatLng.fromDegrees(location.lat, location.lng))
    val parentCell = cellId.parent(15)
    val previousCell = parentCell.prev()
    val nextCell = parentCell.next()

    val (cellIds, _, _) = (1 to radius).foldLeft((Vector(parentCell.id()), previousCell, nextCell)) {
      (acc, _) => acc match {
        case (ids, previous, next) =>
          (previous.id() +: ids :+ next.id(), previous.prev(), next.next())
      }
    }

    cellIds
  }


  def calculateCoordinate(origin: Location, distance: Kilometers, bearing: Radians): Location = {
    val (lat, lng) = origin.toRadians
    val distanceRatio = distance / EarthRadiusInKm

    val newLat = math.asin(math.sin(lat) * math.cos(distanceRatio) +
      math.cos(lat) * math.sin(distanceRatio) * math.cos(bearing))

    val newLng = lng + math.atan2(math.sin(bearing) * math.sin(distanceRatio) * math.cos(lat),
      math.cos(distanceRatio) - math.sin(lat) * math.sin(newLat))

    Location.fromRadians(newLat, newLng)
  }

  def generateHexagonRing(origin: Location, radius: Double, numRings: Int): List[Location] = {

    type Transformation = (Location, Kilometers, Kilometers) => Location

    def calculateDiagonalCoords(yBearing: Option[Radians], xBearing: Radians)(l: Location, yDelta: Kilometers,
                                                                      xDelta: Kilometers) = {
      val yTranslation = yBearing.fold(l)(yBearing => calculateCoordinate(l, yDelta, yBearing))
      calculateCoordinate(yTranslation, xDelta / 2.0, xBearing)
    }

    // Given a list, duplicate each element num times
    def duplicate[T](list: List[T], num: Int) = for {
      elem <- list
      _ <- 1 to num
    } yield elem

    // Given a transformation list, merge the first transformation with the one provided
    def mergeFront(tf: Transformation,
                   transformations: List[Transformation]) = {
      transformations match {
        case Nil => Nil
        case h::t =>
          val mergedTransformation: Transformation = (l, y, x) => h(tf(l, y, x), y, x)
          mergedTransformation :: t
      }
    }

    val northWest = calculateDiagonalCoords(Some(Bearing.North), Bearing.West)_
    val east:Transformation = (l, yDelta, xDelta) =>
      calculateDiagonalCoords(None, Bearing.East)(l, yDelta, xDelta * 2.0)
    val southEast = calculateDiagonalCoords(Some(Bearing.South), Bearing.East)_
    val southWest = calculateDiagonalCoords(Some(Bearing.South), Bearing.West)_
    val west:Transformation = (l, yDelta, xDelta) =>
      calculateDiagonalCoords(None, Bearing.West)(l, yDelta, xDelta * 2.0)
    val northEast = calculateDiagonalCoords(Some(Bearing.North), Bearing.East)_

    val xDelta = math.sqrt(3) * radius // distance between column centers
    val yDelta = 3 * (radius / 2) // distance between row centers
    // create a list of transformation steps to produce points along
    val ringTransformations = List(east, southEast, southWest, west, northWest, northEast)

    val allRingTransformations = for {
      ring <- 1 to numRings
      transformation <- mergeFront(northWest, duplicate(ringTransformations, ring))
    } yield transformation


    val ringLocations = allRingTransformations.foldLeft((origin, List(origin))) {
      (acc, transformation) => acc match {
        case (prevLocation, locations) =>
          val l = transformation(prevLocation, yDelta, xDelta)
          (l, l :: locations)
      }
    }

    ringLocations._2.reverse
  }

}
