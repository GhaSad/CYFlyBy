package fr.cy.cyflyby.actors

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object TrafficSupervisor:

  sealed trait Command
  final case class SetRunwayFree(isFree: Boolean) extends Command
  final case class SetNearbyAircraft(isNearby: Boolean) extends Command

  def apply(tower: ActorRef[ControlTowerActor.Command]): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match
        case SetRunwayFree(isFree) =>
          context.log.info("[TrafficSupervisor] piste libre={} transmise à la tour", Boolean.box(isFree))
          tower ! ControlTowerActor.UpdateRunwayFree(isFree)
          Behaviors.same

        case SetNearbyAircraft(isNearby) =>
          context.log.info("[TrafficSupervisor] avion proche={} transmis à la tour", Boolean.box(isNearby))
          tower ! ControlTowerActor.UpdateNearbyAircraft(isNearby)
          Behaviors.same
    }
