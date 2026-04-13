package fr.cy.cyflyby.app

import com.typesafe.config.ConfigFactory
import fr.cy.cyflyby.formal.VerificationRunner
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.adapter.*
import org.apache.pekko.http.*
import org.apache.pekko.http.scaladsl.Http

import scala.util.{Failure, Success}

object ServerMain:

  def main(args: Array[String]): Unit =
    VerificationRunner.runAll()

    given ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "CYFlyByWebSystem")
    val manager = summon[ActorSystem[Nothing]].systemActorOf(AirportManagerActor(), "airport-manager")
    val config = ConfigFactory.load()
    val host = config.getString("cyflyby.http.host")
    val port = config.getInt("cyflyby.http.port")

    val routes = HttpRoutes(manager).routes
    val binding = Http()(using summon[ActorSystem[Nothing]].classicSystem).newServerAt(host, port).bind(routes)

    binding.onComplete {
      case Success(bound) =>
        println()
        println(s"CYFlyBy Web prêt sur http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}")
        println("Ouvre cette URL dans ton navigateur.")
        println("Utilise Ctrl+C dans le terminal pour arrêter le serveur.")
      case Failure(exception) =>
        println(s"Échec du démarrage du serveur: ${exception.getMessage}")
        summon[ActorSystem[Nothing]].terminate()
    }(using summon[ActorSystem[Nothing]].executionContext)
