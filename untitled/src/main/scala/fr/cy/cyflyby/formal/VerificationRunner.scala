package fr.cy.cyflyby.formal

import fr.cy.cyflyby.domain.*

object VerificationRunner:

  private final case class PropertyCase(
      id: String,
      operation: OperationType,
      snapshot: ControlSnapshot,
      expectedAuthorization: Boolean,
      expectation: String
  )

  private val propertyCases = List(
    PropertyCase(
      id = "P1",
      operation = OperationType.Decollage,
      snapshot = ControlSnapshot(
        runwayFree = true,
        weather = WeatherLevel.Bonne,
        nearbyAircraft = false,
        communicationOk = true,
        technicalOk = true,
        intrusionDetected = false
      ),
      expectedAuthorization = true,
      expectation = "Un décollage nominal doit être autorisé"
    ),
    PropertyCase(
      id = "P1",
      operation = OperationType.Decollage,
      snapshot = ControlSnapshot(
        runwayFree = false,
        weather = WeatherLevel.Bonne,
        nearbyAircraft = false,
        communicationOk = true,
        technicalOk = true,
        intrusionDetected = false
      ),
      expectedAuthorization = false,
      expectation = "Un décollage doit être refusé si la piste est occupée"
    ),
    PropertyCase(
      id = "P2",
      operation = OperationType.Atterrissage,
      snapshot = ControlSnapshot(
        runwayFree = true,
        weather = WeatherLevel.Critique,
        nearbyAircraft = false,
        communicationOk = true,
        technicalOk = true,
        intrusionDetected = false
      ),
      expectedAuthorization = false,
      expectation = "Un atterrissage doit être refusé en météo critique"
    ),
    PropertyCase(
      id = "P3",
      operation = OperationType.Atterrissage,
      snapshot = ControlSnapshot(
        runwayFree = true,
        weather = WeatherLevel.Bonne,
        nearbyAircraft = false,
        communicationOk = false,
        technicalOk = true,
        intrusionDetected = false
      ),
      expectedAuthorization = false,
      expectation = "Une perte de communication bloque l'opération"
    ),
    PropertyCase(
      id = "P4",
      operation = OperationType.Decollage,
      snapshot = ControlSnapshot(
        runwayFree = true,
        weather = WeatherLevel.Bonne,
        nearbyAircraft = false,
        communicationOk = true,
        technicalOk = true,
        intrusionDetected = true
      ),
      expectedAuthorization = false,
      expectation = "Une intrusion bloque la piste"
    )
  )

  private val ltlCatalog = List(
    "P1: G(decollage_autorise -> (piste_libre && meteo_ok && separation_ok && communication_ok && technique_ok && !intrusion))",
    "P2: G(atterrissage_autorise -> (piste_libre && meteo_ok && separation_ok && communication_ok && technique_ok && !intrusion))",
    "P3: G(com_perdue -> F(alerte_envoyee))",
    "P4: G(intrusion_detectee -> X(piste_bloquee))",
    "P5: G(!(decollage_autorise && atterrissage_autorise))"
  )

  def runAll(): Unit =
    println("\n=== Vérification logique métier ===")
    propertyCases.foreach { testCase =>
      val decision = SafetyRules.evaluate(testCase.operation, testCase.snapshot)
      val verdict = decision.authorized == testCase.expectedAuthorization
      println(s"[${testCase.id}] ${testCase.expectation}")
      println(s"  - verdict=${if verdict then "OK" else "KO"}")
      println(s"  - décision=${if decision.authorized then "autorisé" else "refusé"}")
      println(s"  - raison=${decision.reason}")
      println(s"  - état=${testCase.snapshot.summary}")
    }

    println("\n=== Vérification via réseau de Pétri ===")
    propertyCases.take(2).foreach { testCase =>
      val net = CYFlyByPetriModel.fromSnapshot(testCase.operation, testCase.snapshot)
      println(s"[${testCase.id}] transitions activées au départ: ${net.enabledTransitions.mkString(", ")}")
      val next = net.enabledTransitions.headOption.flatMap(net.fire)
      println(s"  - deadlock initial = ${net.isDeadlock}")
      println(s"  - marquage initial = ${net.marking}")
      next.foreach(after => println(s"  - marquage après 1 tir = ${after.marking}"))
    }

    println("\n=== Catalogue LTL pour le rapport ===")
    ltlCatalog.foreach(formula => println(s"  - $formula"))
