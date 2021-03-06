/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package scripts.transaction.function

import tools.Plotting
import utils.ScriptBase


/* Transition function example
 * Created 18.12.2017 author CAB */

object TransitionFunctionExample extends ScriptBase with Plotting{
  println("#### Transition function example ####")
  //Definitions
  case class S_(t: D, g: D, x: D, y: D)
  //Parameter
  val u = 5.0   //Init speed in m/s
  val ts = (0 to 10 by 1).map(_ / 10.0)
  val gs = (0 to 10 by 1).map(_ / 10.0)
  val Δt = 0.1
  //Parametric model
  def x(t: D) = t * u
  def y(t: D, g: D) = t * u - (g * t * t) / 2
  //Plot model
  val modelPoints = gs.flatMap(g ⇒ ts.map(t ⇒ (g, t))).map{ case (g, t) ⇒ (x(t), y(t, g), g)}
  Scatter3D(modelPoints).show()
  //Step functions
  def υt(t: D) = t + Δt
  def υg(x: D) = x / (10.0 * 5.0)
  //Trace
  var ss = Vector(S_(t = .0, g = .1, x = .0, y = .0)) //Init state S'
  while (ss.last.t <= 10.0) {
    val ps = ss.last
    val nt = υt(ps.t)   //Next t base on prev t
    val ng = υg(ps.x)   //Next g date on prev x
    val nx = x(nt)      //Next y eval by model
    val ny = y(nt, ng)  //Next x eval by model
    ss :+= S_(nt, ng, nx, ny)} //Put net state into list
  //Plot x, y
  Plot2D(ss.map(s ⇒ (s.x, s.y)))
}
