package fr.cy.cyflyby.actors

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object SecuritySupervisor:

  sealed trait Command
  final case class SetCommunication(isOk: Boolean) extends Command
  final case class SetTechnicalStatus(flightId: String, isOk: Boolean) extends Command
  final case class SetIntrusion(detected: Boolean) extends Command

  def apply(tower: ActorRef[ControlTowerActor.Command]): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match
        case SetCommunication(isOk) =>
          context.log.info("[SecuritySupervisor] communication ok={} transmise à la tour", Boolean.box(isOk))
          tower ! ControlTowerActor.UpdateCommunication(isOk)
          Behaviors.same

        case SetTechnicalStatus(flightId, isOk) =>
          context.log.info("[SecuritySupervisor] statut technique {} => {}", flightId, Boolean.box(isOk))
          tower ! ControlTowerActor.UpdateTechnicalStatus(flightId, isOk)
          Behaviors.same

        case SetIntrusion(detected) =>
          context.log.info("[SecuritySupervisor] intrusion détectée={} transmise à la tour", Boolean.box(detected))
          tower ! ControlTowerActor.UpdateIntrusion(detected)
          Behaviors.same
    }
