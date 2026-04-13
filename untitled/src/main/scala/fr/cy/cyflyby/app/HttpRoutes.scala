package fr.cy.cyflyby.app

import fr.cy.cyflyby.api.{ActionResult, DashboardStateView}
import fr.cy.cyflyby.domain.{OperationType, WeatherLevel}
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.{ActorSystem, Scheduler}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

final class HttpRoutes(manager: ActorRef[AirportManagerActor.Command])(using system: ActorSystem[?]):
  import JsonSupport.*

  private given Scheduler = system.scheduler
  private given ExecutionContext = system.executionContext
  private given Timeout = Timeout(3.seconds)

  def routes: Route =
    pathPrefix("api") {
      concat(
        path("state") {
          get {
            complete(manager.ask[DashboardStateView](replyTo => AirportManagerActor.GetState(replyTo)))
          }
        },
        path("reset") {
          post {
            complete(action(reply => AirportManagerActor.Reset(reply)))
          }
        },
        path("complete") {
          post {
            complete(action(reply => AirportManagerActor.CompleteActiveOperation(reply)))
          }
        },
        path("scenario" / Segment) { scenario =>
          post {
            complete(action(reply => AirportManagerActor.RunScenario(scenario, reply)))
          }
        },
        path("weather" / Segment) { level =>
          post {
            decodeWeather(level) match
              case Some(value) => complete(action(reply => AirportManagerActor.SetWeather(value, reply)))
              case None        => complete(StatusCodes.BadRequest, s"Météo inconnue: $level")
          }
        },
        path("runway" / Segment) { mode =>
          post {
            mode match
              case "free"     => complete(action(reply => AirportManagerActor.SetRunwayFree(true, reply)))
              case "occupied" => complete(action(reply => AirportManagerActor.SetRunwayFree(false, reply)))
              case _            => complete(StatusCodes.BadRequest, s"Mode piste inconnu: $mode")
          }
        },
        path("communication" / Segment) { mode =>
          post {
            mode match
              case "ok"   => complete(action(reply => AirportManagerActor.SetCommunication(true, reply)))
              case "lost" => complete(action(reply => AirportManagerActor.SetCommunication(false, reply)))
              case _        => complete(StatusCodes.BadRequest, s"Mode communication inconnu: $mode")
          }
        },
        path("intrusion" / Segment) { mode =>
          post {
            mode match
              case "on"  => complete(action(reply => AirportManagerActor.SetIntrusion(true, reply)))
              case "off" => complete(action(reply => AirportManagerActor.SetIntrusion(false, reply)))
              case _       => complete(StatusCodes.BadRequest, s"Mode intrusion inconnu: $mode")
          }
        },
        path("nearby" / Segment) { mode =>
          post {
            mode match
              case "on"  => complete(action(reply => AirportManagerActor.SetNearbyAircraft(true, reply)))
              case "off" => complete(action(reply => AirportManagerActor.SetNearbyAircraft(false, reply)))
              case _       => complete(StatusCodes.BadRequest, s"Mode séparation inconnu: $mode")
          }
        },
        path("technical" / Segment / Segment) { (flightId, mode) =>
          post {
            mode match
              case "ok"    => complete(action(reply => AirportManagerActor.SetTechnicalStatus(flightId.toUpperCase, true, reply)))
              case "fault" => complete(action(reply => AirportManagerActor.SetTechnicalStatus(flightId.toUpperCase, false, reply)))
              case _         => complete(StatusCodes.BadRequest, s"Mode technique inconnu: $mode")
          }
        },
        path("request" / Segment / Segment) { (flightId, operation) =>
          post {
            decodeOperation(operation) match
              case Some(value) => complete(action(reply => AirportManagerActor.RequestOperation(flightId.toUpperCase, value, reply)))
              case None        => complete(StatusCodes.BadRequest, s"Opération inconnue: $operation")
          }
        }
      )
    } ~
      pathSingleSlash {
        getFromResource("public/index.html")
      } ~
      getFromResourceDirectory("public")

  private def action(build: ActorRef[ActionResult] => AirportManagerActor.Command) =
    manager.ask[ActionResult](build)

  private def decodeWeather(raw: String): Option[WeatherLevel] =
    raw.toLowerCase match
      case "bonne" | "good"        => Some(WeatherLevel.Bonne)
      case "mauvaise" | "bad"      => Some(WeatherLevel.Mauvaise)
      case "critique" | "critical" => Some(WeatherLevel.Critique)
      case _                          => None

  private def decodeOperation(raw: String): Option[OperationType] =
    raw.toLowerCase match
      case "takeoff" | "decollage"    => Some(OperationType.Decollage)
      case "landing" | "atterrissage" => Some(OperationType.Atterrissage)
      case _                             => None
