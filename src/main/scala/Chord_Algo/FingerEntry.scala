package Chord_Algo

import akka.actor.typed.ActorRef


class FingerEntry(start: Int, interval: Interval, var node: ActorRef[Node.Command]) {
  def setNode(node: ActorRef[Node.Command]): Unit =
    this.node = node
  def getNode: ActorRef[Node.Command] =
    this.node
  def getStart: Int =
    this.start
  def getInterval: Interval =
    this.interval
}
