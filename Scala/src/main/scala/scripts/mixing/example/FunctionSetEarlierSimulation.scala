/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package scripts.mixing.example

import tools.Plotting
import org.jzy3d.colors.Color
import utils.ScriptBase


/* Function set representation of mixing problem
 *   onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.5%20Basic%20modeling&
 *   section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={AFB5685E-E61D-4707-B829-BABB190BC41D}&
 *   object-id={489EDFFB-40E0-073C-3F49-7F005EDECE76}&64
 * Created 27.04.2018 author CAB */

object FunctionSetEarlierSimulation  extends ScriptBase with Plotting { import math._
  println(
    """ #### Earlier function set representation and simulation ####
      | X = [t]
      | Y = [ω_1, ω_2̂]
      | G = [v_1, v_2, q_1, q_2, q_3, q_4, ω_3]
    """.stripMargin)
  //Definitions
  case class valX(t: D)
  case class valY(ω_1: D, ω_2: D)
  case class valG(v_1: D, v_2: D, q_1: D, q_2: D, q_3: D, q_4: D, ω_3: D)
  val G = valG(
    v_1 = 4,  // L
    v_2 = 8,  // L
    q_1 = 3,  // L/m
    q_2 = 2,  // L/m
    q_3 = 5,  // L/m
    q_4 = 3,  // L/m
    ω_3 = 10) // g/l
  val dt = .1
  val valX_0 = valX(t = 0)
  val valY_0 = valY(ω_1 = 0, ω_2 = 20)
  //Model
  def F(X: valX)(G: valG, dt: D, valX_0: valX, valY_0: valY): valY = {
    var t = valX_0.t
    var ω_1 = valY_0.ω_1
    var ω_2 = valY_0.ω_2
    while (t <= X.t){
      val ω_1_m1 = ω_1
      val ω_2_m1 = ω_2
      ω_1 = ω_1_m1 + dt * (((G.q_1 * G.ω_3) + (G.q_2 * ω_2_m1) - (G.q_3 * ω_1_m1)) / G.v_1)
      ω_2 = ω_2 + dt * (((G.q_3 * ω_1_m1) - (G.q_2 * ω_2_m1) - (G.q_4 * ω_2_m1)) / G.v_2)
      t += dt}
    valY(ω_1, ω_2)}
  //Simulations
  def simulation(setX: Vector[valX])(G: valG): Vector[valY] = setX.map(X ⇒ F(X)(G, dt, valX_0, valY_0))
  //Run simulation
  val setX = (0 to 100).map(i ⇒ valX(t = i / 10.0)).toVector
  val setY = simulation(setX)(G)
  //Plot
  val (ωs1, ωs2) = setY.map(Y ⇒ (Y.ω_1, Y.ω_2)).unzip
  MultiYPlot2D(setX.map(_.t), Seq((ωs1, Color.RED), (ωs2, Color.GREEN))).show()}
