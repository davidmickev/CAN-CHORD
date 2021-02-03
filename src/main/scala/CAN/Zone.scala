package CAN

import akka.actor.typed.ActorRef
import com.typesafe.config.ConfigFactory

object Zone extends Enumeration {
  type direction = Value
  val Left: direction = Value(0)
  val Up: direction = Value(1)
  val Right: direction = Value(2)
  val Down: direction = Value(3)
  val default: direction = Value(-1)

  val m: Int = ConfigFactory.load("application.conf").getInt("matrix_size")

  def apply(X_range: (Double , Double), Y_range: (Double , Double)): Zone = new Zone(X_range, Y_range)

  def findLocation(movieTitle: String): (Double, Double) = {
// encrypt using SHA-1 Format
    val md = java.security.MessageDigest.getInstance("SHA-1")
    println(md.digest(movieTitle.getBytes("UTF-8")).map("%02x".format(_)).mkString)

    // take the first four after SHA1 conversion
    val firstFour = md.digest(movieTitle.getBytes("UTF-8")).map("%02x".format(_)).mkString.slice(0,4).map(_.toUpper)
    //println(firstFour)

    // convert SHA-1 to coordinate plane of 16x16 for (x,y)
    // X,Y = Hash(Sum(hash(0,2)), Hash(Sum(hash(2,4))
    var x =
    (firstFour.slice(0,1).
      replace("A","10").replace("B","11").replace("C","12").
      replace("D","14").replace("E","15").replace("F","16").toDouble
      +
      firstFour.slice(1,2).
        replace("A","10").replace("B","11").replace("C","12").
        replace("D","14").replace("E","15").replace("F","16").toDouble)/2.0

    var y =
      (firstFour.slice(2, 3).
        replace("A", "10").replace("B", "11").replace("C", "12").
        replace("D", "14").replace("E", "15").replace("F", "16").toDouble
        +
        firstFour.slice(3, 4).
          replace("A", "10").replace("B", "11").replace("C", "12").
          replace("D", "14").replace("E", "15").replace("F", "16").toDouble) / 2.0

    // if x or y lay on the border, we always go x+1, or y+1 to designate it into proper zone
    if(y == 7.0){ y += 1 }
    if(x == 7.0){ x += 1 }
    (x,y)
  }
}
  class Zone(var X_range: (Double, Double),var Y_range: (Double, Double)) {
    // Ordering: Split -> x then y
    var split = 'x'
    var neighborTable: Neighbors = Neighbors()

    import Zone.{Up, Down, Left, Right, default}
    

    var occupant: Option[ActorRef[Node.Command]] = None
    /*
    *   Entry | Direction
    *     0   | Left
    *     1   | Up
    *     2   | Right
    *     3   | Down
*/
   /* var zones: Array[Zone] = Array.fill(4)(Zone((0.0,0.0), (0.0,0.0)))

    def setZone(z: Zone, index: Int): Unit =
      this.zones(index) = z

    def bottomZone(): Zone =
      zones(3)
*/
    /** Sets reference to node that owns the defined zone */
    def setReference(occupant: ActorRef[Node.Command]): Unit = this.occupant = Some(occupant)
    /** Returns: Actor Reference. Ensures Fault Tolerance */
    def getReference: Option[ActorRef[Node.Command]] = this.occupant
    /** Updates a neighbor entry of the nodes that owns the zone */
    def setNeighborTable(index: Int, entry: Neighbor): Unit = this.neighborTable.neighbors(index) = entry
    /** Returns: X-axis range of zone */
    def get_XRange: (Double, Double) = X_range
    /** Returns: Y-axis range of zone*/
    def get_YRange: (Double, Double) = Y_range
    /** Returns: formatted string of the defined zone */
    def formatZone: String = s"X Range: $X_range Y Range: $Y_range"
    /** Returns: Boolean signifying if P is in the defined zone */
    def containsP(P: (Double, Double)): Boolean = P._1 >= get_XRange._1 && P._1 <= get_XRange._2 && P._2 >= get_YRange._1 && P._2 <= get_YRange._2
    /** In place split of X range. New node always gets right side. */
    def splitX(newX2: Double): Unit = X_range = (get_XRange._1, newX2)
    /** In place split of Y range. New node always gets top side. */
    def splitY(newY2: Double): Unit = Y_range = (get_YRange._1, newY2)

    /** Returns Boolean
     * Check if P's Y value is in this nodes YRange */
    def P_In_YRange(P: (Double, Double)): Boolean = get_YRange._1 <= P._2 && P._2 <= get_YRange._2
    /** Return Boolean
     * Check if P's X value is in this nodes XRange */
    def P_In_XRange(P: (Double, Double)): Boolean = get_XRange._1 <= P._1 && P._1 <= get_XRange._2
    /** Returns Direction
     * Find direction to rout. P's X value is in this nodes X range,
     * while P's Y value is in this nodes Y range. Next node is either UP or DOWN */
    def optimal_YDirection(P: (Double, Double)): Zone.direction = if(P._2 > get_YRange._2)  Up else  Down
    /** Returns Direction
     * Find direction to rout. P's Y value is in this nodes Y range,
     * while P's X value is in this nodes X range. Next node is either LEFT or RIGHT */
    def optimal_XDirection(P: (Double, Double)): Zone.direction = if (P._1 > get_XRange._2) Right else Left


    /** Returns: Boolean. True if closest corner in zone to point P is the top-left corner. Otherwise, False.*/
    def P_towardsTopLeft(P: (Double, Double)): Boolean = (P._1 < get_XRange._1) && (P._2 > get_YRange._2)
    /** Returns: Boolean. True if closest corner in zone to point P is the top-right corner. Otherwise, False.*/
    def P_towardsTopRight(P: (Double, Double)): Boolean = (get_XRange._2 < P._1) && (get_YRange._2 < P._2)
    /** Returns: Boolean. True if closest corner in zone to point P is the bottom-left corner. Otherwise, False.*/
    def P_towardsBottomLeft(P: (Double, Double)): Boolean = (P._1 < get_XRange._1) && (P._2 < get_YRange._1)
    /** Returns: Boolean. True if closest corner in zone to point P is the bottom-right corner. Otherwise, False.*/
    def P_towardsBottomRight(P: (Double, Double)): Boolean = (get_XRange._2 < P._1) && (P._2 < get_YRange._1)

    /** Returns a list of optimal neighbors to forward procedure associated with the find zone command */
    def closestPointToP(procedure: Procedure[Node.Command]): List[ActorRef[Node.Command]] = {
      // If this function is executed, P was not in this node's zone
      val P: (Double, Double) = procedure.getLocation.get
      // If in one of this nodes ranges, optimal forward is only one node
      if(P_In_XRange(P)) {
        val dir = optimal_YDirection(P)
        val neighbor = neighborTable.neighbors(dir.id).getNode
        if (neighbor != null && !procedure.wasVisited(neighbor))
          return List(neighbor)
      }
      if(P_In_YRange(P)) {
        val dir = optimal_XDirection(P)
        val neighbor = neighborTable.neighbors(dir.id).getNode
        if (neighbor != null && !procedure.wasVisited(neighbor))
          return List(neighbor)
      }
      // We are finding optimal neighbors to forward the procedure (Min: 1, Max: 0)
      var validNeighborsToForward: List[ActorRef[Node.Command]] = List()
      if (P_towardsTopRight(P)){
        validNeighborsToForward +:= neighborTable.neighbors(Up.id).getNode
        validNeighborsToForward +:= neighborTable.neighbors(Right.id).getNode
      }
      else if (P_towardsBottomRight(P)){
        validNeighborsToForward +:= neighborTable.neighbors(Down.id).getNode
        validNeighborsToForward +:= neighborTable.neighbors(Right.id).getNode
      }
      else if (P_towardsBottomLeft(P)){
        validNeighborsToForward +:= neighborTable.neighbors(Left.id).getNode
        validNeighborsToForward +:= neighborTable.neighbors(Down.id).getNode
      }
      else if (P_towardsTopLeft(P)){
        validNeighborsToForward +:= neighborTable.neighbors(Up.id).getNode
        validNeighborsToForward +:= neighborTable.neighbors(Left.id).getNode
      }
      validNeighborsToForward.filter( n => n != null && !procedure.wasVisited(n))
    }

    def _splitZone: Zone = {
      val ((x1,x2),(y1,y2)) = (get_XRange, get_YRange)
      var halfPoint = 0.0
      var newZone: Zone = null
      if (split == 'x'){
        // New X Border
        halfPoint = (x1 + x2)/2
        // Reassignment of this zone
        splitX(halfPoint)
        // New node gets right half
        newZone = Zone((halfPoint, x2), get_YRange)
        split = 'y'
      }
      else if(split == 'y'){
        // New Y Border
        halfPoint = (y1 + y2)/2
        // Reassignment of this zone
        splitY(halfPoint)
        // New node gets top half
        newZone = Zone(get_YRange, (halfPoint, y2))
        split = 'x'
      }
      newZone.copyNeighborTable(neighborTable)
    }

    /** Returns: Zone. This zone belongs to the new node.
     * Ensures old zones neighbor table is NOT overwritten */
    def copyNeighborTable(fromSplitNeighborTable: Neighbors): Zone = {
      // Copying neighbor table FROM node giving half its zone TO new node
      val directions = List(Up, Down, Left, Right)
      for (d <- directions){
        this.neighborTable.neighbors(d.id) = fromSplitNeighborTable.neighbors(d.id)
      }
      this
    }

    /** Returns a direction. Used in set_neighbor function */
    def findDirection(zone: Zone): Zone.direction = {
      val X_axis = zone.get_XRange
      val Y_axis = zone.get_YRange
      if(X_axis._1 < get_XRange._1 && X_axis._2 == get_XRange._1 && Y_axis == get_YRange) Left
      else if(X_axis._1 == get_XRange._2 && X_axis._2 > get_XRange._1 && Y_axis == get_YRange) Right
      else if(X_axis == get_XRange && Y_axis._1 == get_YRange._2 && Y_axis._2 > get_YRange._2) Up
      else Down
    }
    /** Returns: Boolean. True if zones are identical. Otherwise, False. */
    def identicalZone(zone: Zone): Boolean = get_XRange == zone.get_XRange && get_YRange == zone.get_YRange

    /** Updates neighbor table, only if passed in zone is a valid neighbor */
    def set_neighbor(node: ActorRef[Node.Command], zone: Zone): Unit = {
      val X_axis = zone.get_XRange
      val Y_axis = zone.get_YRange
      var direction = default
      var entry = Neighbor(node, (0, 0), direction)
      // Same Zone
      if (identicalZone(zone)) return
      /* Find Direction of neighbor */
      direction = findDirection(zone)
      // Set Up Entry
      if (direction == Up || direction == Down) {
        entry = Neighbor(node, X_axis, direction)
        if (direction == Up) neighborTable.neighbors(1) = entry
        else neighborTable.neighbors(3) = entry
      }
      if (direction == Left || direction == Right) {
        entry = Neighbor(node, Y_axis, direction)
        if (direction == Left) neighborTable.neighbors(0) = entry
        else neighborTable.neighbors(2) = entry
      }
    }
  }

