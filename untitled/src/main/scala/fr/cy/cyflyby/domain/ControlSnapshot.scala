package fr.cy.cyflyby.domain

final case class ControlSnapshot(
    runwayFree: Boolean = true,
    weather: WeatherLevel = WeatherLevel.Bonne,
    nearbyAircraft: Boolean = false,
    communicationOk: Boolean = true,
    technicalOk: Boolean = true,
    intrusionDetected: Boolean = false
):
  def summary: String =
    s"pisteLibre=$runwayFree, meteo=${weather.label}, avionProche=$nearbyAircraft, communicationOk=$communicationOk, techniqueOk=$technicalOk, intrusion=$intrusionDetected"
