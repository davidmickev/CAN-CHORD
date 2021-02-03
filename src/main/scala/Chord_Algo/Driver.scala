package Chord_Algo

import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object Simulation {
  def main(args: Array[String]): Unit = {
    val system: ActorSystem[Driver.lookup] =
      ActorSystem(Driver(), "driver")

    system ! Driver.lookup("ToyStory")
    system ! Driver.lookup("MoneyTrain")
  }
}

object Driver {

  final case class lookup(key: String)

  def apply(): Behavior[lookup] =
    Behaviors.setup { context =>
      val chord = context.spawn(Chord(), "chord")

      Behaviors.receiveMessage { message =>
        val replyTo = context.spawn(User(), message.key)
        chord ! Chord.keyLookup("MoneyTrain", replyTo)
        val value = Some(Chord.getChordActor.dictionary(message.key))
        replyTo ! User.queryResponse(message.key, value)
        Behaviors.same
      }
    }

}