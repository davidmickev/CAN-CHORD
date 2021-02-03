package Chord_Algo

// [a,b)
// Left side is inclusive && Right is exclusive
object Interval{
  def apply(start: Int, end: Int): Interval = new Interval(start, end)
}

class Interval(start: Int, var end: Int) {
  def contains(nodeID: Int): Boolean = {
    val interval = Range(start, end)
    interval.contains(nodeID)
  }
  def get_end: Int =
    end
  def set_end(i: Int): Unit = {
    this.end = i
  }
}
