package fr.cy.cyflyby.actors

import fr.cy.cyflyby.domain.*
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object ControlTowerActor:

  sealed trait Command
  final case class UpdateWeather(level: WeatherLevel) extends Command
  final case class UpdateRunwayFree(isFree: Boolean) extends Command
  final case class UpdateNearbyAircraft(isNearby: Boolean) extends Command
  final case class UpdateCommunication(isOk: Boolean) extends Command
  final case class UpdateTechnicalStatus(flightId: String, isOk: Boolean) extends Command
  final case class UpdateIntrusion(detected: Boolean) extends Command
  final case class RequestAuthorization(
      flightId: String,
      operation: OperationType,
      replyTo: ActorRef[FlightActor.Command]
  ) extends Command
  final case class OperationCompleted(flightId: String, operation: OperationType) extends Command

  final case class State(
      runwayFree: Boolean = true,
      weather: WeatherLevel = WeatherLevel.Bonne,
      nearbyAircraft: Boolean = false,
      communicationOk: Boolean = true,
      technicalByFlight: Map[String, Boolean] = Map.empty,
      intrusionDetected: Boolean = false
  ):
    def snapshotFor(flightId: String): ControlSnapshot =
      ControlSnapshot(
        runwayFree = runwayFree,
        weather = weather,
        nearbyAircraft = nearbyAircraft,
        communicationOk = communicationOk,
        technicalOk = technicalByFlight.getOrElse(flightId, true),
        intrusionDetected = intrusionDetected
      )

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("Tour de contrôle initialisée")
      running(State())
    }

  private def running(state: State): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match
        case UpdateWeather(level) =>
          context.log.info("[Tower] Mise à jour météo: {}", level.label)
          running(state.copy(weather = level))

        case UpdateRunwayFree(isFree) =>
          context.log.info("[Tower] État piste: libre={}", Boolean.box(isFree))
          running(state.copy(runwayFree = isFree))

        case UpdateNearbyAircraft(isNearby) =>
          context.log.info("[Tower] Séparation: avion proche={}", Boolean.box(isNearby))
          running(state.copy(nearbyAircraft = isNearby))

        case UpdateCommunication(isOk) =>
          context.log.info("[Tower] Communication: ok={}", Boolean.box(isOk))
          running(state.copy(communicationOk = isOk))

        case UpdateTechnicalStatus(flightId, isOk) =>
          context.log.info("[Tower] Statut technique {} => {}", flightId, Boolean.box(isOk))
          running(state.copy(technicalByFlight = state.technicalByFlight.updated(flightId, isOk)))

        case UpdateIntrusion(detected) =>
          context.log.info("[Tower] Intrusion détectée={}", Boolean.box(detected))
          running(state.copy(intrusionDetected = detected))

        case RequestAuthorization(flightId, operation, replyTo) =>
          val snapshot = state.snapshotFor(flightId)
          val decision = SafetyRules.evaluate(operation, snapshot)

          val takeoffAuthorized = operation == OperationType.Decollage && decision.authorized
          val landingAuthorized = operation == OperationType.Atterrissage && decision.authorized
          SafetyRules
            .globalInvariants(snapshot, takeoffAuthorized, landingAuthorized)
            .foreach { invariant =>
              context.log.info(
                s"[${invariant.id}] ${invariant.description} => ${if invariant.passed then "OK" else "KO"}"
              )
            }

          if decision.authorized then
            context.log.info(
              "[Tower] {} autorisé pour {} ({})",
              operation.label,
              flightId,
              decision.reason
            )
            replyTo ! FlightActor.AuthorizationGranted(operation, decision.reason, snapshot)
            running(state.copy(runwayFree = false))
          else
            context.log.info(
              "[Tower] {} refusé pour {} ({})",
              operation.label,
              flightId,
              decision.reason
            )
            replyTo ! FlightActor.AuthorizationDenied(operation, decision.reason, snapshot)
            Behaviors.same

        case OperationCompleted(flightId, operation) =>
          context.log.info("[Tower] {} terminé pour {} -> piste libérée", operation.label, flightId)
          running(state.copy(runwayFree = true))
    }
