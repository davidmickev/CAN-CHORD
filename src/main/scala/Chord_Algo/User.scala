package Chord_Algo

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

object User{
  def apply(): Behavior[Command] =
    Behaviors.setup(new User(_))

  trait Command
  final case class queryResponse(key: String, value: Option[String]) extends Command
}


class User(context: ActorContext[User.Command]) extends AbstractBehavior[User.Command](context) {
  import Chord.{getChordActor, keyLookup}
  import User._
  val chord: ActorRef[Chord.Command] = getChordActor.getReference

  override def onMessage(msg: Command): Behavior[User.Command] = {
    msg match {
      case queryResponse(key, value) =>
        value match {
          case None =>
            context.log.info("KEY: " + key + " is not in distributed map")
          case Some(v) =>
            context.log.info("KEY: " + key + " found, VALUE: " + v)
        }
    }
    this
  }
  /*
      Asynchronous function:
        Sends keyLookup command to Chord
   */
  def queryKey(key: String): Unit = {
    context.log.info("USER sent query to CHORD, KEY: " + key)
    chord ! keyLookup(key, context.self)
  }


  /* Signal handling */
  override def onSignal: PartialFunction[Signal, Behavior[User.Command]] = {
    case PostStop =>
      context.log.info("User actor stopped")
      this
  }

}
