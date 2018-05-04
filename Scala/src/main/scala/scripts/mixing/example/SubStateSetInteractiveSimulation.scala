/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package scripts.mixing.example

import org.jzy3d.colors.Color
import tools.{Input, Plotting}
import utils.ScriptBase


/* Sub-state representation of mixing problem
 *   onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.5%20Basic%20modeling&
 *   section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={AFB5685E-E61D-4707-B829-BABB190BC41D}&
 *   object-id={8EF68737-7BF8-08E1-0BCA-7F9EFBD51B94}&3E
 * Created 28.04.2018 author CAB */


object SubStateSetInteractiveSimulation extends ScriptBase with Plotting with Input{ import math._
  println(
    """ #### Sub-state set interactive simulation ####
      | X = [t]
      | Y = [ω_1, ω_2̂]
      | G = [v_1, v_2, q_1, q_2, q_3, q_4, ω_3]
    """.stripMargin)
  //Helpers
  val chart = ChartRecorder2D(lines = Seq(("ω_1", Color.GREEN), ("ω_2", Color.RED), ("ω_3", Color.GRAY)), maxRange = 30)
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
  def F(dt: D, valX_0: valX, valY_0: valY)(X: valX, G: valG): valY = {
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
  def simulation(setX: Vector[valX], G: valG, M_i: (valX, valG)⇒valY): Vector[valY] = setX.map(X ⇒ M_i(X, G))
  //Simulation loop
  var quit = false
  var currentG = G
  var lastSetX = Vector[valX](valX_0)
  var lastSetY = Vector[valY](valY_0)
  chart.addPoints(valX_0.t, Seq(valY_0.ω_1, valY_0.ω_2, G.ω_3))
  while (! quit){
    //Pick next model
    val M_i: (valX, valG)⇒valY = F(dt, valX_0 = lastSetX.last, valY_0 = lastSetY.last)
    //Run simulation for current iteration with current parameters
    val setX = (0 to 5).map(i ⇒ valX(t = lastSetX.last.t + (i / 10.0))).toVector
    val setY = simulation(setX, G = currentG, M_i)
    //Plot points
    setX.zip(setY).foreach{ case (valX, valY) ⇒
      chart.addPoints(valX.t, Seq(valY.ω_1, valY.ω_2, currentG.ω_3))}
    //Store last X's and Y's
    lastSetX = setX
    lastSetY = setY
    //Sleep for 0.1 second
    Thread.sleep(200L)
    //Check input for next iteration
    swCheckAndReadChar() match{
      case Some('u') ⇒
        currentG = currentG.copy(ω_3 = currentG.ω_3 + 1)
      case Some('d') ⇒
        currentG = currentG.copy(ω_3 = currentG.ω_3 - 1)
      case Some('q') ⇒
        quit = true
      case Some(c) ⇒ println("Wrong input: " + c.toInt)
      case None ⇒}}
  //Exit
  System.exit(0)}

