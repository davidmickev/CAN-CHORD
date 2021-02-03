package CAN

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import net.ceedubs.ficus.Ficus._
import com.typesafe.config.Config

object User{
  def apply(): Behavior[Command] =
    Behaviors.setup(new User(_))

  trait Command

 //final case class insert(key: String, value: Option[String]) extends Command
  final case class queryResponse(key: String, value: Option[String]) extends Command
  // Insert (K, V) pairs from config
  final case class insertConfig(config: Config) extends Command
  case class insertConfirmed(key: String, value: String) extends Command
  // Used to search movie titiles
  final case class lookup(key: String) extends Command with DNS.Command with Bootstrap.Command
}


class User(context: ActorContext[User.Command]) extends AbstractBehavior[User.Command](context) {
  import User._
  import DNS.{insert, keyLookup}
  import Procedure.{KEY_STORE, KEY_LOOKUP}
  val dns: ActorRef[DNS.Command] = DNS.getDNSActor.connectToDNS
  var dictionary: Map[String, String] = Map.empty[String, String]

  override def onMessage(msg: Command): Behavior[User.Command] = {
    msg match {

      case lookup(key) =>
        dns ! keyLookup(Procedure[Node.Command]()
          .withKeyToFind(key)
          .withLocation(Zone.findLocation(key))
          .withRoutingPurpose(KEY_LOOKUP)
          .withUser(context.self))

      // Insert (K, V) pairs from config
      case insertConfig(config) =>
        // Creating dictionary defined in config file: application.conf
        dictionary = config.as[Map[String, String]]("dictionary")
        dictionary.foreach( pair => {
          // Send message to insert (K, V) pair
          dns ! insert(Procedure[Node.Command]()
            .withUser(context.self)
            .withKeyValueToStore(pair)
            .withLocation(Zone.findLocation(pair._1))
            .withRoutingPurpose(KEY_STORE))
        })

      case insertConfirmed(key: String, value: String) =>
        context.log.info("KEY: " + key + " , VALUE: " + value + " INSERTED IN DISTRIBUTED HASH MAP")

      case queryResponse(key, value) =>
        value match {
          case None =>
            context.log.info("KEY: " + key + " IS NOT IN DISTRIBUTED HASH MAP")
          case Some(v) =>
            context.log.info("KEY: " + key + " FOUND. VALUE: " + v)
        }
    }
    this
  }

  /* Signal handling */
  override def onSignal: PartialFunction[Signal, Behavior[User.Command]] = {
    case PostStop =>
      context.log.info("User actor stopped")
      this
  }

}

