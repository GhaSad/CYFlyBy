package fr.cy.cyflyby.actors

import fr.cy.cyflyby.domain.WeatherLevel
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object MeteoSupervisor:

  sealed trait Command
  final case class SetWeather(level: WeatherLevel) extends Command

  def apply(tower: ActorRef[ControlTowerActor.Command]): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match
        case SetWeather(level) =>
          context.log.info("[MeteoSupervisor] météo={} transmise à la tour", level.label)
          tower ! ControlTowerActor.UpdateWeather(level)
          Behaviors.same
    }
