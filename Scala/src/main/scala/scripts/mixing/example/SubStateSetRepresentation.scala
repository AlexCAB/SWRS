/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package scripts.mixing.example

import org.jzy3d.colors.Color
import tools.Plotting
import utils.ScriptBase


/* Sub-state representation of mixing problem
 *   onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.5%20Basic%20modeling&
 *   section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={AFB5685E-E61D-4707-B829-BABB190BC41D}&
 *   object-id={8EF68737-7BF8-08E1-0BCA-7F9EFBD51B94}&3E
 * Created 28.04.2018 author CAB */

object SubStateSetRepresentation extends ScriptBase with Plotting { import math._
  println(
    """ #### Sub-state set representation ####
      | X = [t]
      | Y = [ω_1, ω_2̂]
      | G = [v_1, v_2, q_1, q_2, q_3, q_4, ω_3]
    """.stripMargin)
  //Definitions
  case class valX(t: D)
  case class valY(ω_1: D, ω_2: D)
  trait domS
  case class valS1(ω_1: D) extends domS
  case class valS2(ω_2: D) extends domS
  trait domS_X{ val X: valX; val S: domS; val j: Int }
  case class S_X_1(X: valX, S: valS1, j: Int = 1) extends domS_X
  case class S_X_2(X: valX, S: valS2, j: Int = 2) extends domS_X
  //Functional model
  def F(X: valX): valY = {
    val q = sqrt(105.0)
    val em = ((q - 15.0) * X.t) / 16.0
    val ep = -(((q + 15.0) * X.t) / 16.0)
    valY(
      ω_1 = ((13.0 * exp(em) * q) / 21.0) - ((13.0 * exp(ep) * q) / 21.0) - (5.0 * exp(em)) - (5.0 * exp(ep)) + 10.0,
      ω_2 = -((5.0 * exp(em) * q) / 21.0) + ((5.0 * exp(ep) * q) / 21.0) + (5.0 * exp(em)) + (5.0 * exp(ep)) + 10.0)}
  //Generating of set of states
  val S_X_G: Set[domS_X] = (0 to 100).toSet[Int].flatMap{ i ⇒
    val X = valX(t = i / 10.0)
    val Y = F(X)
    Set(S_X_1(X, valS1(Y.ω_1)), S_X_2(X, valS2(Y.ω_2)))}
  println(s"Generated sub-states (|S_X_G| = ${S_X_G.size}):")
  S_X_G.foreach(S_X_j ⇒ println("    " + S_X_j))
  //Simulations
  def simulation(setX: Vector[valX]): Vector[valY] =setX.map{ valX ⇒
    //Collect all ub-state with same key
    val setS_valX = S_X_G.filter(_.X == valX)
    assume(setS_valX.size == 2, "Should be 2 sub-states for each valX key")
    //Build Y value
    valY(
      ω_1 = setS_valX.filter(_.j == 1).head.asInstanceOf[S_X_1].S.ω_1,
      ω_2 = setS_valX.filter(_.j == 2).head.asInstanceOf[S_X_2].S.ω_2)}
  //Run simulation in [0, 5]
  val setX: Vector[valX] = (0 to 50).map(i ⇒ valX(t = i / 10.0)).toVector
  val setY: Vector[valY] = simulation(setX)
  //Plot
  val (ωs1, ωs2) = setY.map(valY ⇒ (valY.ω_1, valY.ω_2)).unzip
  MultiYPlot2D(setX.map(_.t), Seq((ωs1, Color.RED), (ωs2, Color.GREEN))).show()}
