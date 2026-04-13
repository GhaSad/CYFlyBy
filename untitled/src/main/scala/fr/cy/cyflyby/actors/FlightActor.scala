package fr.cy.cyflyby.actors

import fr.cy.cyflyby.domain.*
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.*

object FlightActor:

  sealed trait Command
  final case class StartOperation(operation: OperationType) extends Command
  final case class AuthorizationGranted(
      operation: OperationType,
      reason: String,
      snapshot: ControlSnapshot
  ) extends Command
  final case class AuthorizationDenied(
      operation: OperationType,
      reason: String,
      snapshot: ControlSnapshot
  ) extends Command

  def apply(id: String, tower: ActorRef[ControlTowerActor.Command]): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      Behaviors.receiveMessage[Command] {
        case StartOperation(operation) =>
          context.log.info("[{}] Demande de {}", id, operation.label)
          tower ! ControlTowerActor.RequestAuthorization(id, operation, context.self)
          Behaviors.same

        case AuthorizationGranted(operation, reason, snapshot) =>
          context.log.info("[{}] Autorisation reçue: {} | {} | {}", id, operation.label, reason, snapshot.summary)
          context.scheduleOnce(1.second, tower, ControlTowerActor.OperationCompleted(id, operation))
          Behaviors.same

        case AuthorizationDenied(operation, reason, snapshot) =>
          context.log.info("[{}] Refus reçu: {} | {} | {}", id, operation.label, reason, snapshot.summary)
          Behaviors.same
      }
    }
