package CAN

import akka.actor.typed.ActorRef

object Neighbor{
  def apply(node: ActorRef[Node.Command], range: (Double, Double), direction: Zone.direction): Neighbor = new Neighbor(node, range, direction)
}
class Neighbor(var node: ActorRef[Node.Command], var range: (Double, Double), var direction: Zone.direction) {
  var visited = false

  def setVisited(): Unit =
    this.visited = true
  def setNode(node: ActorRef[Node.Command]): Unit =
    this.node = node
  def getNode: ActorRef[Node.Command] =
    this.node
  def getRange: (Double, Double) =
    this.range
  def getDirection: Zone.direction =
    this.direction
  def setRange(range: (Double, Double)): Unit =
    this.range = range
}
