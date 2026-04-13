package fr.cy.cyflyby.actors

import fr.cy.cyflyby.domain.OperationType
import fr.cy.cyflyby.domain.WeatherLevel
import org.apache.pekko.actor.typed.{Behavior, PostStop}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.*

object GuardianActor:

  sealed trait Command
  private case object Boot extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      val tower = context.spawn(ControlTowerActor(), "control-tower")
      val meteo = context.spawn(MeteoSupervisor(tower), "meteo-supervisor")
      val traffic = context.spawn(TrafficSupervisor(tower), "traffic-supervisor")
      val security = context.spawn(SecuritySupervisor(tower), "security-supervisor")

      val af447 = context.spawn(FlightActor("AF447", tower), "flight-af447")
      val af112 = context.spawn(FlightActor("AF112", tower), "flight-af112")

      context.self ! Boot

      Behaviors
        .receiveMessage[Command] {
          case Boot =>
            context.log.info("=== Démarrage de la simulation CYFlyBy ===")

            meteo ! MeteoSupervisor.SetWeather(WeatherLevel.Bonne)
            traffic ! TrafficSupervisor.SetRunwayFree(true)
            traffic ! TrafficSupervisor.SetNearbyAircraft(false)
            security ! SecuritySupervisor.SetCommunication(true)
            security ! SecuritySupervisor.SetTechnicalStatus("AF447", true)
            security ! SecuritySupervisor.SetTechnicalStatus("AF112", true)
            security ! SecuritySupervisor.SetIntrusion(false)

            context.scheduleOnce(500.millis, af447, FlightActor.StartOperation(OperationType.Decollage))

            context.scheduleOnce(2.seconds, meteo, MeteoSupervisor.SetWeather(WeatherLevel.Critique))
            context.scheduleOnce(2500.millis, af112, FlightActor.StartOperation(OperationType.Atterrissage))

            context.scheduleOnce(4.seconds, meteo, MeteoSupervisor.SetWeather(WeatherLevel.Bonne))
            context.scheduleOnce(4200.millis, security, SecuritySupervisor.SetCommunication(false))
            context.scheduleOnce(4500.millis, af112, FlightActor.StartOperation(OperationType.Atterrissage))

            context.scheduleOnce(6.seconds, security, SecuritySupervisor.SetCommunication(true))
            context.scheduleOnce(6200.millis, security, SecuritySupervisor.SetIntrusion(true))
            context.scheduleOnce(6500.millis, af112, FlightActor.StartOperation(OperationType.Atterrissage))

            context.scheduleOnce(8.seconds, security, SecuritySupervisor.SetIntrusion(false))
            context.scheduleOnce(8200.millis, traffic, TrafficSupervisor.SetRunwayFree(true))
            context.scheduleOnce(8500.millis, af112, FlightActor.StartOperation(OperationType.Atterrissage))

            Behaviors.same
        }
        .receiveSignal {
          case (context, PostStop) =>
            context.log.info("Arrêt du guardian CYFlyBy")
            Behaviors.same
        }
    }
