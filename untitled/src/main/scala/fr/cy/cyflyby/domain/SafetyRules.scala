package fr.cy.cyflyby.domain

object SafetyRules:

  def evaluate(operation: OperationType, snapshot: ControlSnapshot): AuthorizationDecision =
    operation match
      case OperationType.Decollage    => evaluateTakeoff(snapshot)
      case OperationType.Atterrissage => evaluateLanding(snapshot)

  def evaluateTakeoff(snapshot: ControlSnapshot): AuthorizationDecision =
    if !snapshot.communicationOk then
      denied("P3", "Communication perdue: procédure de sécurité activée", snapshot)
    else if snapshot.intrusionDetected then
      denied("P4", "Intrusion détectée: piste bloquée", snapshot)
    else if !snapshot.runwayFree then
      denied("P1", "Piste occupée: décollage refusé", snapshot)
    else if snapshot.weather == WeatherLevel.Critique || snapshot.weather == WeatherLevel.Mauvaise then
      denied("P1", s"Météo ${snapshot.weather.label}: décollage refusé", snapshot)
    else if snapshot.nearbyAircraft then
      denied("P1", "Séparation non assurée: avion proche détecté", snapshot)
    else if !snapshot.technicalOk then
      denied("P1", "Dysfonctionnement technique: décollage interdit", snapshot)
    else
      AuthorizationDecision(
        authorized = true,
        reason = "Décollage autorisé: toutes les conditions critiques sont satisfaites",
        propertyId = "P1",
        snapshot = snapshot
      )

  def evaluateLanding(snapshot: ControlSnapshot): AuthorizationDecision =
    if !snapshot.communicationOk then
      denied("P3", "Communication perdue: guidage impossible pour l'atterrissage", snapshot)
    else if snapshot.intrusionDetected then
      denied("P4", "Intrusion détectée: approche interrompue", snapshot)
    else if !snapshot.runwayFree then
      denied("P2", "Piste occupée: atterrissage différé", snapshot)
    else if snapshot.weather == WeatherLevel.Critique then
      denied("P2", "Météo critique: atterrissage refusé", snapshot)
    else if snapshot.nearbyAircraft then
      denied("P2", "Conflit d'approche: séparation non assurée", snapshot)
    else if !snapshot.technicalOk then
      denied("P2", "Dysfonctionnement technique: atterrissage non sécurisé", snapshot)
    else
      AuthorizationDecision(
        authorized = true,
        reason = "Atterrissage autorisé: approche sécurisée",
        propertyId = "P2",
        snapshot = snapshot
      )

  def globalInvariants(
      snapshot: ControlSnapshot,
      takeoffAuthorized: Boolean,
      landingAuthorized: Boolean
  ): List[InvariantResult] =
    List(
      InvariantResult(
        id = "I1",
        description = "Exclusion mutuelle sur la piste",
        passed = !(takeoffAuthorized && landingAuthorized),
        details = "Décollage et atterrissage ne doivent pas être autorisés simultanément"
      ),
      InvariantResult(
        id = "I2",
        description = "Météo critique bloque les autorisations",
        passed = snapshot.weather != WeatherLevel.Critique || (!takeoffAuthorized && !landingAuthorized),
        details = "En météo critique, aucune opération ne doit être autorisée"
      ),
      InvariantResult(
        id = "I3",
        description = "Perte de communication => alerte globale / blocage",
        passed = snapshot.communicationOk || (!takeoffAuthorized && !landingAuthorized),
        details = "Sans communication, le système doit refuser les opérations"
      ),
      InvariantResult(
        id = "I4",
        description = "Intrusion => piste bloquée",
        passed = !snapshot.intrusionDetected || (!takeoffAuthorized && !landingAuthorized),
        details = "Une intrusion implique un blocage opérationnel"
      ),
      InvariantResult(
        id = "I5",
        description = "Pas d'autorisation sans piste libre",
        passed = snapshot.runwayFree || (!takeoffAuthorized && !landingAuthorized),
        details = "La piste doit être libre pour toute autorisation"
      )
    )

  private def denied(propertyId: String, reason: String, snapshot: ControlSnapshot): AuthorizationDecision =
    AuthorizationDecision(
      authorized = false,
      reason = reason,
      propertyId = propertyId,
      snapshot = snapshot
    )
