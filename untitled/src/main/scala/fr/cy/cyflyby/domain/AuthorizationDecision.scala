package fr.cy.cyflyby.domain

final case class AuthorizationDecision(
    authorized: Boolean,
    reason: String,
    propertyId: String,
    snapshot: ControlSnapshot
)

final case class InvariantResult(
    id: String,
    description: String,
    passed: Boolean,
    details: String
)
