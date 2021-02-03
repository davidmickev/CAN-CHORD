package Chord_Algo

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._


object Chord{
  // Allowing one instances of chord actor for now.
  private var ChordSingleton: Option[Chord] = None
  // Gets the Chord class
  def getChordActor: Chord = ChordSingleton.get
  // Apply function called by Chord(...)
  def apply(): Behavior[Command] =
    Behaviors.setup(context => {
      ChordSingleton = Some(new Chord(context))
      ChordSingleton.get
    })
  // trait Command to generalize the onMessage function
  trait Command
  /*
     Chord.scala:
     this Command in this level forwards the lookup to the node that kick starts algorithm (finger table usage)
     Node.scala:
     this Command in Node.scala file, hashes the key to use finger table
  */
  final case class keyLookup(key: String, user: ActorRef[User.Command]) extends Command with Node.Command
  /*
      this Command is meant to import map values from application.config
   */
  final case class initializeNodesWithConfig(config: Config, replyTo: ActorRef[distributedMapInitialized]) extends Command
  // Used for testing createTestProbe[distributedMapInitialized]
  final case class distributedMapInitialized(dictionary: Map[String, String])
}

class Chord(context: ActorContext[Chord.Command]) extends AbstractBehavior[Chord.Command](context) {
  // For immediate access to case classes
  import Chord._

  var dictionary: Map[String, String] = Map.empty[String, String]
  // Map for Node Actors. accessed by NodeID which is the hashed key
  private var nodes = Map.empty[Int, ActorRef[Node.Command]]
  // Node in charge of triggering finger table updates.
  var managerNode: Option[ActorRef[Node.Command]] = None

  override def onMessage(msg: Chord.Command): Behavior[Chord.Command] = {
    import Node.join
    msg match {

      case keyLookup(key, user) =>
        /* Pass to node with lowest hash id to start CHORD algorithm */
        managerNode.get ! keyLookup(key, user)

      case initializeNodesWithConfig(config, replyTo) =>
        // Creating dictionary defined in config file: application.conf
        dictionary = config.as[Map[String, String]]("dictionary")
        // Calculating m bit identifier
        val m: Int = (Math.log(dictionary.size) / Math.log(2)).toInt
        // For each entry created a Node Actor and append to map
        dictionary.foreach( entry => {
          // Creating the hashed key
          val hashedKey: Int = Hash.encrypt(entry._1, m)
          // Spawning a new node with id being the hashedKey
          val newNode = context.spawn(Node(entry._1, entry._2, m, hashedKey), s"node-$hashedKey")
          // Setting start node (for queries) to node with lowest hash
          if (managerNode.isEmpty) managerNode = Some(newNode)
          // Adding node to map for back up (if start node fails or exits)
          nodes += hashedKey -> newNode
          // logging completed action
          context.log.info("Entry: (" + entry._1 + ", "+ entry._2 +") from dictionary added to Chord. With Node Hash ID: " + hashedKey)
          // Send to manager node
          newNode ! join(managerNode.get)
        })

        // For testing and Driver Actor. See ChordSpec.scala under test folder
        replyTo ! distributedMapInitialized(dictionary)
    }
    this
  }


  /* User obtain ActorRef to Chord Singleton via Chord.getChordActor.getReference */
  def getReference: ActorRef[Command] = context.self


  /* Signal handling */
  override def onSignal: PartialFunction[Signal, Behavior[Chord.Command]] = {
    case PostStop =>
      context.log.info("Chord actor stopped")
      this
  }
}
