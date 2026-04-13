package fr.cy.cyflyby.formal

import fr.cy.cyflyby.domain.ControlSnapshot
import fr.cy.cyflyby.domain.OperationType
import fr.cy.cyflyby.domain.WeatherLevel

object CYFlyByPetriModel:

  def fromSnapshot(operation: OperationType, snapshot: ControlSnapshot): PetriNet =
    val places = Set(
      "demande_recue",
      "piste_libre",
      "meteo_ok",
      "separation_ok",
      "communication_ok",
      "technique_ok",
      "aucune_intrusion",
      "condition_non_sure",
      "autorisation_donnee",
      "operation_refusee",
      "operation_terminee"
    )

    val safeMarking = Map(
      "demande_recue" -> 1,
      "piste_libre" -> booleanToken(snapshot.runwayFree),
      "meteo_ok" -> booleanToken(snapshot.weather == WeatherLevel.Bonne),
      "separation_ok" -> booleanToken(!snapshot.nearbyAircraft),
      "communication_ok" -> booleanToken(snapshot.communicationOk),
      "technique_ok" -> booleanToken(snapshot.technicalOk),
      "aucune_intrusion" -> booleanToken(!snapshot.intrusionDetected),
      "condition_non_sure" -> booleanToken(!allConditionsSafe(snapshot))
    )

    val authorizationTransition = Transition(
      name = s"autoriser_${operation.label.replace('é', 'e')}",
      inputs = Map(
        "demande_recue" -> 1,
        "piste_libre" -> 1,
        "meteo_ok" -> 1,
        "separation_ok" -> 1,
        "communication_ok" -> 1,
        "technique_ok" -> 1,
        "aucune_intrusion" -> 1
      ),
      outputs = Map("autorisation_donnee" -> 1)
    )

    val rejectionTransition = Transition(
      name = s"refuser_${operation.label.replace('é', 'e')}",
      inputs = Map("demande_recue" -> 1, "condition_non_sure" -> 1),
      outputs = Map("operation_refusee" -> 1)
    )

    val completionTransition = Transition(
      name = s"terminer_${operation.label.replace('é', 'e')}",
      inputs = Map("autorisation_donnee" -> 1),
      outputs = Map("operation_terminee" -> 1, "piste_libre" -> 1)
    )

    PetriNet(
      places = places,
      transitions = Map(
        authorizationTransition.name -> authorizationTransition,
        rejectionTransition.name -> rejectionTransition,
        completionTransition.name -> completionTransition
      ),
      marking = safeMarking
    )

  private def booleanToken(value: Boolean): Int = if value then 1 else 0

  private def allConditionsSafe(snapshot: ControlSnapshot): Boolean =
    snapshot.runwayFree &&
      snapshot.weather == WeatherLevel.Bonne &&
      !snapshot.nearbyAircraft &&
      snapshot.communicationOk &&
      snapshot.technicalOk &&
      !snapshot.intrusionDetected
