package fr.cy.cyflyby.formal

final case class Transition(
    name: String,
    inputs: Map[String, Int],
    outputs: Map[String, Int]
)

final case class PetriNet(
    places: Set[String],
    transitions: Map[String, Transition],
    marking: Map[String, Int]
):

  def enabledTransitions: List[String] =
    transitions.values.collect {
      case t if isEnabled(t) => t.name
    }.toList.sorted

  def isEnabled(transition: Transition): Boolean =
    transition.inputs.forall { case (place, needed) => marking.getOrElse(place, 0) >= needed }

  def fire(transitionName: String): Option[PetriNet] =
    transitions.get(transitionName).filter(isEnabled).map { transition =>
      val afterInputs = transition.inputs.foldLeft(marking) { case (acc, (place, count)) =>
        acc.updated(place, acc.getOrElse(place, 0) - count)
      }
      val afterOutputs = transition.outputs.foldLeft(afterInputs) { case (acc, (place, count)) =>
        acc.updated(place, acc.getOrElse(place, 0) + count)
      }
      copy(marking = afterOutputs)
    }

  def isDeadlock: Boolean = enabledTransitions.isEmpty
