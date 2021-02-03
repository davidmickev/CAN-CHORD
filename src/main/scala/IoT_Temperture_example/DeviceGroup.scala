package IoT_Temperture_example

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

/*
    When a DeviceManager receives a request with a group and device id:
    If the manager already has an actor for the device group, it forwards the request to it.
    Otherwise, it creates a new device group actor and then forwards the request.
    The DeviceGroup actor receives the request to register an actor for the given device:
    If the group already has an actor for the device it replies with the ActorRef of the existing device actor.
    Otherwise, the DeviceGroup actor first creates a device actor and replies with the ActorRef of the newly created device actor.
    The sensor will now have the ActorRef of the device actor to send messages directly to it.
*/

object DeviceGroup {
  def apply(groupId: String): Behavior[Command] =
    Behaviors.setup(context => new DeviceGroup(context, groupId))

  trait Command


  private final case class DeviceTerminated(device: ActorRef[Device.Command], groupId: String, deviceId: String)
    extends Command

}

class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
  extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice}
  // Creating a Map for children: Device Actors (Device)
  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]
  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      // @ symbol: Annotations and variable binding on pattern matching, essentially one-liner matching and down-casting
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        // Obtaining Map entry for deviceId
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) => // Device Actor already registered
            replyTo ! DeviceRegistered(deviceActor)
          case None =>              // Device Actor created for new device
            context.log.info("Creating device actor for {}", trackMsg.deviceId)
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
            context.watchWith(deviceActor, DeviceTerminated(deviceActor, groupId, deviceId))
            deviceIdToActor += deviceId -> deviceActor // Adding to Group map
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      // group ID did not match with this actors group ID
      case RequestTrackDevice(gId, _, _) =>
        context.log.warn2("Ignoring TrackDevice request for {}. This actor is responsible for {}.", gId, groupId)
        this

      // Responding the actors contained in the group's map
      case RequestDeviceList(requestId, gId, replyTo) =>
        if (gId == groupId) {
          replyTo ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
          this
        } else
          Behaviors.unhandled

      // Custom terminated to include deviceId for removal
      case DeviceTerminated(_, _, deviceId) =>
        context.log.info("Device actor for {} has been terminated", deviceId)
        //deviceIdToActor.get(deviceId) ! Device.Passivate
        deviceIdToActor -= deviceId // Removing from Group map
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this

  }
}
