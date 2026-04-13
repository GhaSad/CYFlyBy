package fr.cy.cyflyby.domain

enum WeatherLevel:
  case Bonne, Mauvaise, Critique

  def label: String = this match
    case Bonne     => "bonne"
    case Mauvaise  => "mauvaise"
    case Critique  => "critique"
