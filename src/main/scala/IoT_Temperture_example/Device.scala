package IoT_Temperture_example

import akka.actor.typed._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, LoggerOps}

object Device {
  // Apply function called by Device(...)
  def apply(groupId: String, deviceId: String): Behavior[Command] =
    Behaviors.setup(context => new Device(context, groupId, deviceId))
  // trait Command to generalize the onMessage function
  sealed trait Command
  // For case class instance to be received:  thisActor ! ReadTemperature(...)
  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
  // For updating currentTemperature field when the actor receives a message that contains the temperature
  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded]) extends Command
  // For case class instance to be sent back:  requestingActor !  RespondTemperature(...)
  final case class RespondTemperature(requestId: Long, value: Option[Double])
  // For sending an acknowledgment to the sender once we have updated our last temperature recording
  final case class TemperatureRecorded(requestId: Long)
  // Command to gracefully stop the Actor
  case object Passivate extends Command
}

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String)
  extends AbstractBehavior[Device.Command](context) /* super constructor */{
  // For immediate access to case classes
  import Device._

  var lastTemperatureReading: Option[Double] = None
  context.log.info2("Device actor {}-{} started", groupId, deviceId)

  /* Command trait is sealed, must catch every extension of Command. type safety*/
  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case ReadTemperature(id, replyTo) =>
        context.log.info("Command to read temperature received")
        replyTo ! RespondTemperature(id, lastTemperatureReading)
        this
      case  RecordTemperature(id, value, replyTo) =>
        context.log.info("Updating temperature")
        lastTemperatureReading = Some(value)
        replyTo ! TemperatureRecorded(id)
        this
      case Passivate =>
        Behaviors.stopped
    }
  }

  /* Signal handling */
  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info2("Device actor {}-{} stopped", groupId, deviceId)
      this
  }

}
