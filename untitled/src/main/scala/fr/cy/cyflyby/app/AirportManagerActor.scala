package fr.cy.cyflyby.app

import fr.cy.cyflyby.api.*
import fr.cy.cyflyby.domain.*
import fr.cy.cyflyby.formal.CYFlyByPetriModel
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import java.time.LocalTime
import java.time.format.DateTimeFormatter

object AirportManagerActor:

  sealed trait Command
  final case class GetState(replyTo: ActorRef[DashboardStateView]) extends Command
  final case class SetWeather(level: WeatherLevel, replyTo: ActorRef[ActionResult]) extends Command
  final case class SetRunwayFree(isFree: Boolean, replyTo: ActorRef[ActionResult]) extends Command
  final case class SetNearbyAircraft(isNearby: Boolean, replyTo: ActorRef[ActionResult]) extends Command
  final case class SetCommunication(isOk: Boolean, replyTo: ActorRef[ActionResult]) extends Command
  final case class SetIntrusion(detected: Boolean, replyTo: ActorRef[ActionResult]) extends Command
  final case class SetTechnicalStatus(flightId: String, isOk: Boolean, replyTo: ActorRef[ActionResult]) extends Command
  final case class RequestOperation(flightId: String, operation: OperationType, replyTo: ActorRef[ActionResult]) extends Command
  final case class CompleteActiveOperation(replyTo: ActorRef[ActionResult]) extends Command
  final case class Reset(replyTo: ActorRef[ActionResult]) extends Command
  final case class RunScenario(name: String, replyTo: ActorRef[ActionResult]) extends Command

  private final case class FlightRuntime(
                                          id: String,
                                          status: String,
                                          lastOperation: String,
                                          lastDecision: String,
                                          propertyId: String,
                                          technicalOk: Boolean
                                        )

  private final case class State(
                                  runwayFree: Boolean,
                                  weather: WeatherLevel,
                                  nearbyAircraft: Boolean,
                                  communicationOk: Boolean,
                                  intrusionDetected: Boolean,
                                  activeOperation: Option[(String, OperationType)],
                                  flights: Map[String, FlightRuntime],
                                  recentEvents: Vector[String]
                                ):
    def snapshotFor(flightId: String): ControlSnapshot =
      ControlSnapshot(
        runwayFree = runwayFree,
        weather = weather,
        nearbyAircraft = nearbyAircraft,
        communicationOk = communicationOk,
        technicalOk = flights.get(flightId).forall(_.technicalOk),
        intrusionDetected = intrusionDetected
      )

  private val clock = DateTimeFormatter.ofPattern("HH:mm:ss")

  private def initialState: State =
    State(
      runwayFree = true,
      weather = WeatherLevel.Bonne,
      nearbyAircraft = false,
      communicationOk = true,
      intrusionDetected = false,
      activeOperation = None,
      flights = Map(
        "AF447" -> FlightRuntime("AF447", "Au parking", "", "", "", technicalOk = true),
        "AF112" -> FlightRuntime("AF112", "En approche", "", "", "", technicalOk = true)
      ),
      recentEvents = Vector(stamped("Système CYFlyBy initialisé"))
    )

  def apply(): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      context.log.info("AirportManagerActor démarré")
      running(initialState)
    }

  private def positionOf(flight: FlightRuntime): String =
    val status = flight.status.toLowerCase
    if status.contains("en vol") then "en-vol"
    else if status.contains("approche") then "approche"
    else if status.contains("autorisé") then "piste"
    else if status.contains("piste") then "piste"
    else if status.contains("au sol") then "parking"
    else if status.contains("parking") then "parking"
    else "parking"

  private def canRequestOperation(flight: FlightRuntime, operation: OperationType): Boolean =
    operation match
      case OperationType.Decollage =>
        positionOf(flight) == "parking"
      case OperationType.Atterrissage =>
        val pos = positionOf(flight)
        pos == "en-vol" || pos == "approche"

  private def releaseRunwayConsistently(state: State): State =
    state.activeOperation match
      case None =>
        withEvent(
          state.copy(runwayFree = true),
          "Piste libérée manuellement"
        )

      case Some((flightId, operation)) =>
        state.flights.get(flightId) match
          case None =>
            withEvent(
              state.copy(runwayFree = true, activeOperation = None),
              "Piste libérée manuellement"
            )

          case Some(flight) =>
            val (newStatus, newDecision) =
              operation match
                case OperationType.Decollage =>
                  ("Au parking", "Décollage interrompu, retour au parking")
                case OperationType.Atterrissage =>
                  ("En approche", "Atterrissage interrompu, retour en approche")

            withEvent(
              state.copy(
                runwayFree = true,
                activeOperation = None,
                flights = state.flights.updated(
                  flightId,
                  flight.copy(
                    status = newStatus,
                    lastDecision = newDecision
                  )
                )
              ),
              s"Piste libérée manuellement, opération annulée pour $flightId"
            )

  private def running(state: State): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match
        case GetState(replyTo) =>
          replyTo ! toView(state)
          Behaviors.same

        case SetWeather(level, replyTo) =>
          val next = withEvent(state.copy(weather = level), s"Météo mise à jour: ${level.label}")
          context.log.info("Météo => {}", level.label)
          replyTo ! success(next, s"Météo réglée sur ${level.label}")
          running(next)

        case SetRunwayFree(isFree, replyTo) =>
          val next =
            if isFree then
              releaseRunwayConsistently(state)
            else
              withEvent(
                state.copy(runwayFree = false),
                "Piste occupée manuellement"
              )

          context.log.info("Piste libre => {}", Boolean.box(isFree))
          replyTo ! success(next, s"Piste ${if isFree then "libre" else "occupée"}")
          running(next)

        case SetNearbyAircraft(isNearby, replyTo) =>
          val next = withEvent(state.copy(nearbyAircraft = isNearby), s"Séparation: avion proche = $isNearby")
          context.log.info("Avion proche => {}", Boolean.box(isNearby))
          replyTo ! success(next, if isNearby then "Conflit d'approche activé" else "Conflit d'approche supprimé")
          running(next)

        case SetCommunication(isOk, replyTo) =>
          val next = withEvent(state.copy(communicationOk = isOk), s"Communication ${if isOk then "rétablie" else "perdue"}")
          context.log.info("Communication ok => {}", Boolean.box(isOk))
          replyTo ! success(next, if isOk then "Communication rétablie" else "Communication perdue")
          running(next)

        case SetIntrusion(detected, replyTo) =>
          val next = withEvent(state.copy(intrusionDetected = detected), s"Intrusion ${if detected then "détectée" else "levée"}")
          context.log.info("Intrusion => {}", Boolean.box(detected))
          replyTo ! success(next, if detected then "Intrusion activée" else "Intrusion levée")
          running(next)

        case SetTechnicalStatus(flightId, isOk, replyTo) =>
          state.flights.get(flightId) match
            case None =>
              replyTo ! failure(state, s"Vol inconnu: $flightId")
              Behaviors.same
            case Some(flight) =>
              val updatedFlight = flight.copy(technicalOk = isOk)
              val next = withEvent(
                state.copy(flights = state.flights.updated(flightId, updatedFlight)),
                s"Statut technique $flightId = ${if isOk then "OK" else "PANNE"}"
              )
              context.log.info("Technique {} => {}", flightId, Boolean.box(isOk))
              replyTo ! success(next, s"Statut technique de $flightId mis à jour")
              running(next)

        case RequestOperation(flightId, operation, replyTo) =>
          state.flights.get(flightId) match
            case None =>
              replyTo ! failure(state, s"Vol inconnu: $flightId")
              Behaviors.same

            case Some(flight) if !canRequestOperation(flight, operation) =>
              val positionLabel =
                positionOf(flight) match
                  case "parking"  => "parking"
                  case "approche" => "approche"
                  case "en-vol"   => "en vol"
                  case "piste"    => "piste"
                  case other      => other

              val message =
                operation match
                  case OperationType.Decollage =>
                    s"$flightId ne peut pas décoller depuis la position $positionLabel"
                  case OperationType.Atterrissage =>
                    s"$flightId ne peut pas atterrir depuis la position $positionLabel"

              replyTo ! failure(state, message)
              Behaviors.same

            case Some(flight) =>
              val snapshot = state.snapshotFor(flightId)
              val decision = SafetyRules.evaluate(operation, snapshot)

              val updatedFlight =
                if decision.authorized then
                  flight.copy(
                    status = s"${operation.label.capitalize} autorisé",
                    lastOperation = operation.label,
                    lastDecision = decision.reason,
                    propertyId = decision.propertyId
                  )
                else
                  flight.copy(
                    status = flight.status,
                    lastOperation = operation.label,
                    lastDecision = decision.reason,
                    propertyId = decision.propertyId
                  )

              val nextBase = state.copy(
                runwayFree = if decision.authorized then false else state.runwayFree,
                activeOperation = if decision.authorized then Some(flightId -> operation) else state.activeOperation,
                flights = state.flights.updated(flightId, updatedFlight)
              )

              val next = withEvent(
                nextBase,
                s"$flightId demande ${operation.label}: ${if decision.authorized then "AUTORISÉ" else "REFUSÉ"} (${decision.propertyId})"
              )

              context.log.info(
                "{} {} pour {}",
                operation.label.capitalize,
                if decision.authorized then "autorisé" else "refusé",
                flightId
              )

              if decision.authorized then
                replyTo ! success(next, decision.reason, decision.propertyId)
              else
                replyTo ! ActionResult(ok = false, message = decision.reason, propertyId = decision.propertyId, state = toView(next))

              running(next)

        case CompleteActiveOperation(replyTo) =>
          state.activeOperation match
            case None =>
              replyTo ! failure(state, "Aucune opération active à terminer")
              Behaviors.same

            case Some((flightId, operation)) =>
              val flight = state.flights(flightId)

              val completedStatus =
                operation match
                  case OperationType.Decollage    => "En vol"
                  case OperationType.Atterrissage => "Au parking"

              val next = withEvent(
                state.copy(
                  runwayFree = true,
                  activeOperation = None,
                  flights = state.flights.updated(
                    flightId,
                    flight.copy(
                      status = completedStatus,
                      lastDecision = s"${operation.label.capitalize} terminé avec succès"
                    )
                  )
                ),
                s"$flightId a terminé ${operation.label}; la piste est libérée"
              )

              context.log.info("Opération terminée pour {}", flightId)
              replyTo ! success(next, s"${operation.label.capitalize} terminé pour $flightId")
              running(next)

        case Reset(replyTo) =>
          val next = initialState
          context.log.info("Réinitialisation complète")
          replyTo ! success(next, "Système réinitialisé")
          running(next)

        case RunScenario(name, replyTo) =>
          scenarioState(name) match
            case Some(next) =>
              context.log.info("Scénario appliqué: {}", name)
              replyTo ! success(next, s"Scénario appliqué: $name")
              running(next)
            case None =>
              replyTo ! failure(state, s"Scénario inconnu: $name")
              Behaviors.same
    }

  private def scenarioState(name: String): Option[State] =
    name match
      case "nominal" =>
        Some(withEvent(initialState, "Scénario nominal chargé"))

      case "meteo-critique" =>
        Some(withEvent(initialState.copy(weather = WeatherLevel.Critique), "Scénario météo critique chargé"))

      case "communication-perdue" =>
        Some(withEvent(initialState.copy(communicationOk = false), "Scénario perte de communication chargé"))

      case "intrusion" =>
        Some(withEvent(initialState.copy(intrusionDetected = true), "Scénario intrusion chargé"))

      case "conflit-approche" =>
        Some(
          withEvent(
            initialState.copy(
              nearbyAircraft = true,
              flights = initialState.flights.updated(
                "AF447",
                initialState.flights("AF447").copy(status = "En vol")
              )
            ),
            "Scénario conflit d'approche chargé"
          )
        )

      case "panne-af447" =>
        Some(
          withEvent(
            initialState.copy(
              flights = initialState.flights.updated(
                "AF447",
                initialState.flights("AF447").copy(technicalOk = false)
              )
            ),
            "Scénario panne technique AF447 chargé"
          )
        )

      case "panne-af112" =>
        Some(
          withEvent(
            initialState.copy(
              flights = initialState.flights.updated(
                "AF112",
                initialState.flights("AF112").copy(technicalOk = false)
              )
            ),
            "Scénario panne technique AF112 chargé"
          )
        )

      case _ => None

  private def success(state: State, message: String, propertyId: String = ""): ActionResult =
    ActionResult(ok = true, message = message, propertyId = propertyId, state = toView(state))

  private def failure(state: State, message: String): ActionResult =
    ActionResult(ok = false, message = message, propertyId = "", state = toView(state))

  private def withEvent(state: State, event: String): State =
    state.copy(recentEvents = (state.recentEvents :+ stamped(event)).takeRight(20))

  private def stamped(event: String): String =
    s"[${LocalTime.now().format(clock)}] $event"

  private def toView(state: State): DashboardStateView =
    val takeoffSnapshot = state.snapshotFor("AF447")
    val landingSnapshot = state.snapshotFor("AF112")

    val takeoffDecision = SafetyRules.evaluate(OperationType.Decollage, takeoffSnapshot)
    val landingDecision = SafetyRules.evaluate(OperationType.Atterrissage, landingSnapshot)

    val invariants = SafetyRules
      .globalInvariants(
        ControlSnapshot(
          runwayFree = state.runwayFree,
          weather = state.weather,
          nearbyAircraft = state.nearbyAircraft,
          communicationOk = state.communicationOk,
          technicalOk = true,
          intrusionDetected = state.intrusionDetected
        ),
        takeoffAuthorized = takeoffDecision.authorized,
        landingAuthorized = landingDecision.authorized
      )
      .map(result => InvariantView(result.id, result.description, result.passed, result.details))

    val petriTakeoff = CYFlyByPetriModel.fromSnapshot(OperationType.Decollage, takeoffSnapshot)
    val petriLanding = CYFlyByPetriModel.fromSnapshot(OperationType.Atterrissage, landingSnapshot)

    DashboardStateView(
      runwayFree = state.runwayFree,
      weather = state.weather.label,
      nearbyAircraft = state.nearbyAircraft,
      communicationOk = state.communicationOk,
      intrusionDetected = state.intrusionDetected,
      activeOperation = state.activeOperation.map { case (flightId, operation) => s"$flightId - ${operation.label}" }.getOrElse("Aucune"),
      flights = state.flights.values.toList.sortBy(_.id).map { flight =>
        FlightView(
          id = flight.id,
          status = flight.status,
          lastOperation = flight.lastOperation,
          lastDecision = flight.lastDecision,
          propertyId = flight.propertyId,
          technicalOk = flight.technicalOk
        )
      },
      invariants = invariants,
      petriViews = List(
        PetriView("décollage", petriTakeoff.enabledTransitions, petriTakeoff.isDeadlock),
        PetriView("atterrissage", petriLanding.enabledTransitions, petriLanding.isDeadlock)
      ),
      recentEvents = state.recentEvents.reverse.toList,
      summary = ControlSnapshot(
        runwayFree = state.runwayFree,
        weather = state.weather,
        nearbyAircraft = state.nearbyAircraft,
        communicationOk = state.communicationOk,
        technicalOk = true,
        intrusionDetected = state.intrusionDetected
      ).summary
    )