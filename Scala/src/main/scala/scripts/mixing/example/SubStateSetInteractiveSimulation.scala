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
  trait domS
  case class valS1(ω_1: D) extends domS
  case class valS2(ω_2: D) extends domS
  trait domS_X{ val X: valX; val S: domS; val j: Int }
  case class S_X_1(X: valX, S: valS1, j: Int = 1) extends domS_X
  case class S_X_2(X: valX, S: valS2, j: Int = 2) extends domS_X
  //Parameters
  val G = valG(
    v_1 = 4,  // L
    v_2 = 8,  // L
    q_1 = 3,  // L/m
    q_2 = 2,  // L/m
    q_3 = 5,  // L/m
    q_4 = 3,  // L/m
    ω_3 = 10) // g/l
  val dt = .1 // X[t] step
  val ds = .5 //Simulation step
  val valX_0 = valX(t = 0)
  val valY_0 = valY(ω_1 = 0, ω_2 = 20)
  //Model
  def F(G: valG, dt: D, valX_0: valX, valY_0: valY)(X: valX): valY = {
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
  //Sub-state set generator (generate for key range [valX_0, valX_n] with dt step)
  def gen_S_X_G(G: valG, dt: D, valX_0: valX, valY_0: valY, valX_n: valX): Set[domS_X] = {
    val n = ((valX_n.t - valX_0.t) / dt).toInt
    (0 to n).toSet[Int].flatMap{ i ⇒
      val X = valX(t = valX_0.t + (i * dt))
      val Y = F(G, dt, valX_0, valY_0)(X)
      Set(S_X_1(X, valS1(Y.ω_1)), S_X_2(X, valS2(Y.ω_2)))}}
  //Simulations
  def simulation(setX: Vector[valX])(S_X_G: Set[domS_X]): Vector[valY] = setX.map{ valX ⇒
    //Collect all ub-state with same key
    val setS_valX = S_X_G.filter(_.X == valX)
    assume(setS_valX.size == 2, "Should be 2 sub-states for each valX key")
    //Build Y value
    valY(
      ω_1 = setS_valX.filter(_.j == 1).head.asInstanceOf[S_X_1].S.ω_1,
      ω_2 = setS_valX.filter(_.j == 2).head.asInstanceOf[S_X_2].S.ω_2)}
  //Simulation loop
  var quit = false
  var currentG = G
  var lastSetX = Vector[valX](valX_0)
  var lastSetY = Vector[valY](valY_0)
  chart.addPoints(valX_0.t, Seq(valY_0.ω_1, valY_0.ω_2, G.ω_3))
  while (! quit){
    //Pick next model
    val G = currentG
    val valX_0 = lastSetX.last
    val valY_0 = lastSetY.last
    val valX_n = valX(lastSetX.last.t + ds)
    val S_X_G: Set[domS_X] = gen_S_X_G(G, dt, valX_0, valY_0, valX_n)
    //Print S_X_G model
    println(s"Next S_X_G (valX_0 = $valX_0, valY_0 = $valY_0, valX_n = $valX_n, G = $G, |S_X_G| = ${S_X_G.size}):")
    S_X_G.foreach(ss ⇒ println("          " + ss))
    //Run simulation for current iteration with current parameters
    val setX: Vector[valX] = (0 to ((valX_n.t - valX_0.t) / dt).toInt).map(i ⇒ valX(t = valX_0.t + (i * dt))).toVector
    val setY = simulation(setX)(S_X_G)
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