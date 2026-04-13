package fr.cy.cyflyby.domain

enum OperationType:
  case Decollage, Atterrissage

  def label: String = this match
    case Decollage    => "décollage"
    case Atterrissage => "atterrissage"
