package fr.cy.cyflyby.app

import fr.cy.cyflyby.api.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat

object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol:
  implicit val flightViewFormat: RootJsonFormat[FlightView] = jsonFormat6(FlightView.apply)
  implicit val invariantViewFormat: RootJsonFormat[InvariantView] = jsonFormat4(InvariantView.apply)
  implicit val petriViewFormat: RootJsonFormat[PetriView] = jsonFormat3(PetriView.apply)
  implicit val dashboardStateViewFormat: RootJsonFormat[DashboardStateView] = jsonFormat11(DashboardStateView.apply)
  implicit val actionResultFormat: RootJsonFormat[ActionResult] = jsonFormat4(ActionResult.apply)