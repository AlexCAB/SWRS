/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package scripts.mixing.example

import org.jzy3d.colors.Color
import tools.{Input, Plotting}
import utils.ScriptBase


/* Function representation of mixing problem and interactive simulation
 *   onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.5%20Basic%20modeling&
 *   section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={AFB5685E-E61D-4707-B829-BABB190BC41D}&
 *   object-id={8EF68737-7BF8-08E1-0BCA-7F9EFBD51B94}&3E
 * Created 28.04.2018 author CAB */


object FunctionSetInteractiveSimulation extends ScriptBase with Plotting with Input{
  println(
    """ #### Function set interactive simulation ####
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
  val G_0 = valG(
    v_1 = 4,  // L
    v_2 = 8,  // L
    q_1 = 3,  // L/m
    q_2 = 2,  // L/m
    q_3 = 5,  // L/m
    q_4 = 3,  // L/m
    ω_3 = 10) // g/l
  val dt = .1 // seconds
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
  //Simulations
  def simulation(barX_i: valX)(M_i: valX⇒valY): valY = M_i(barX_i)
  //Helpers functions
  object H{
    //Variables
    private var input: Option[Char] = None
    private var currentParameters = G_0
    private var timeCounter = .0
    //Functions
    def not_terminated(): Boolean = {
      input = swCheckAndReadChar()
      ! input.contains('q')}
    def get_parameters(): valG  = {
      if(input.contains('u')) {
        currentParameters = currentParameters.copy(ω_3 = currentParameters.ω_3 + 1)
        println("Up, new ω_3 = " + currentParameters.ω_3)}
      if(input.contains('d')) {
        currentParameters = currentParameters.copy(ω_3 = currentParameters.ω_3 - 1)
        println("Down, new ω_3 = " + currentParameters.ω_3)}
      input = None
      currentParameters}
    def get_next_real_time(): D = {
      val currentTime = timeCounter
      timeCounter += dt //Next time
      Thread.sleep((dt * 1000).toLong)
      currentTime}
    def get_model(G: valG, barX: valX, hatY: valY): valX⇒valY =
      F(G, dt, valX_0 = barX, valY_0 = hatY)
    def show(barX: valX, hatY: valY): Unit =
        chart.addPoints(barX.t, Seq(hatY.ω_1, hatY.ω_2, currentParameters.ω_3))}
  //Interactive simulation (regard pseudo-code 4)
  var G = H.get_parameters()
  var barX = valX_0
  var hatY = valY_0
  var M = H.get_model(G, barX, hatY)
  while (H.not_terminated()){
    val t_real = H.get_next_real_time()
    val G_new = H.get_parameters()
    if (G_new != G){
      M = H.get_model(G_new, barX, hatY)
      G = G_new}
    barX = valX(t_real)
    hatY = simulation(barX)(M)
    H.show(barX, hatY)}
  //Exit
  System.exit(0)}

