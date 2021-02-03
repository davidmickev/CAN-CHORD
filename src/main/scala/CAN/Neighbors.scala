package CAN

import Zone.default

object Neighbors{
  def apply(): Neighbors = new Neighbors()
}
class Neighbors {
  /*
  *   Entry | Direction
  *     0   | Left
  *     1   | Up
  *     2   | Right
  *     3   | Down
  *
  */
  var neighbors: Array[Neighbor] = newNeighborTable()

  def newNeighborTable(): Array[Neighbor] =
    Array.fill(4)(new Neighbor(null, (0,0), default))

}
