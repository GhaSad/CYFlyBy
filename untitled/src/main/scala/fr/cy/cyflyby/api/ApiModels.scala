package fr.cy.cyflyby.api

final case class FlightView(
    id: String,
    status: String,
    lastOperation: String,
    lastDecision: String,
    propertyId: String,
    technicalOk: Boolean
)

final case class InvariantView(
    id: String,
    description: String,
    passed: Boolean,
    details: String
)

final case class PetriView(
    operation: String,
    enabledTransitions: List[String],
    deadlock: Boolean
)

final case class DashboardStateView(
    runwayFree: Boolean,
    weather: String,
    nearbyAircraft: Boolean,
    communicationOk: Boolean,
    intrusionDetected: Boolean,
    activeOperation: String,
    flights: List[FlightView],
    invariants: List[InvariantView],
    petriViews: List[PetriView],
    recentEvents: List[String],
    summary: String
)

final case class ActionResult(
    ok: Boolean,
    message: String,
    propertyId: String,
    state: DashboardStateView
)
