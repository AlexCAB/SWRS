/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package scripts.mixing.example

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, Outlet, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import org.jzy3d.colors.Color
import tools.{GraphVisualization, Plotting}
import utils.ScriptBase


/* Naive reactive implementation of mixing problem
 *   onenote:https://onedrive.live.com/view.aspx?resid=8E55450F976AC566%21545&id=documents&
 *   wd=target%28%D0%A1%D1%82%D0%B0%D1%82%D1%8C%D0%B8.one%7C40964F6B-F1E6-40E5-93D6-D7D464B4D57F%2FSWRS%20v0.2.1%
 *   3A%20Logical%20processors%7C4155DDB2-718A-4C14-AFC1-5103ED2BCCA3%2F%29
 * Created 28.04.2018 author CAB */

object ReactiveNaiveImplementation extends ScriptBase with Plotting with GraphVisualization{
  println(
    """ #### Naive reactive implementation of mixing problem ####
      | X = [t]
      | Y = [ω_1, ω_2̂]
      | G = [v_1, v_2, q_1, q_2, q_3, q_4, ω_3]
    """.stripMargin)

  // Data definition
  trait SX_q_
  case class SX_1_(t: D, ω_1: D) extends SX_q_
  case class SX_2_(t: D, ω_2: D) extends SX_q_
  case class M(ss: SX_q_) extends SX_q_
  case class P_1_(v_1: D, q_1: D, q_2: D, q_3: D, ω_3: D)
  case class P_2_(v_2: D, q_2: D, q_3: D, q_4: D)
  case class G_(v_1: D, v_2: D, q_1: D, q_2: D, q_3: D, q_4: D, ω_3: D){
    def toP_1_ = P_1_(v_1, q_1, q_2, q_3, ω_3)
    def toP_2_ = P_2_(v_2, q_2, q_3, q_4)}

  // Input definition
  class SPrimeInput_
    ( S_prime: SX_q_,
      w: Int,
      c: UniformFanInShape[M, _])
    (implicit builder: GraphDSL.Builder[_])
  extends VizNodeLike {
    // Blocks definition
    private val source = Source.single(M(S_prime))
    private val bcast = builder.add(Broadcast[M](3))
    // Wiring
    import GraphDSL.Implicits._
    source ~> bcast
    bcast.out(0) ~> c
    // Visualization
    val viz_id = s"IN_w=$w"
    val viz_edges = List()
    val viz_xy = Tuple2(0, w)
    viz_color = "blue"
    viz_size = 5
    // Outputs
    val r1: (Outlet[M], S) = (bcast.out(1), viz_id)
    val r2: (Outlet[M], S) = (bcast.out(2), viz_id)}

  // Logical processor definition
  class LPe_
    ( k1: (Outlet[M], S),
      k2: (Outlet[M], S),
      f_t: D⇒D,
      f_ω: (D,D,D,D) ⇒ D,
      d: Int,
      w: Int,
      c: UniformFanInShape[M, _])
    (implicit builder: GraphDSL.Builder[_])
  extends VizNodeLike {
    // Blocks definition
    private val zip = builder.add(Zip[M, M])
    private val map = builder.add(Flow[(M, M)]
      .map[M]{
        case (M(ss1: SX_1_), M(ss2: SX_2_)) ⇒
          //Eval next t and ω
          assert(ss1.t == ss2.t, "ss1.t != ss2.t")
          val t = f_t(ss1.t)
          val ω = f_ω(ss1.ω_1, ss2.ω_2, ss1.t, t)
          //Visualisation
          Thread.sleep(100)
          viz_color = "black"
          //Build sub state
          w match{
            case 1 ⇒ M(SX_1_(t, ω))
            case 2 ⇒ M(SX_2_(t, ω))
            case _ ⇒ throw new AssertionError(s"w should be 1 or 2, w = $w")}
        case (m1, m2) ⇒ throw new AssertionError(s"M_1 not SX_1_ and/or M_2 not SX_2_, M_1 = $m1, M_2 = $m2")}
      .async)
    private val bcast = builder.add(Broadcast[M](3))
    // Wiring
    import GraphDSL.Implicits._
    k1._1 ~> zip.in0
    k2._1 ~> zip.in1
    zip.out ~> map ~> bcast
    bcast.out(0) ~> c
    // Visualization
    val viz_id = s"LP_d=$d,w=$w"
    val viz_edges = List((k1._2, viz_id), (k2._2, viz_id))
    val viz_xy = Tuple2(d, w)
    viz_color = "gray"
    viz_size = 5
    // Outputs
    val r1: (Outlet[M], S) = (bcast.out(1), viz_id)
    val r2: (Outlet[M], S) = (bcast.out(2), viz_id)}

  // Graph C builder
  def buildC[R](
    n: Int,
    Δt: D,
    G: G_,
    X_transition: D⇒D⇒D,
    S_transition: (P_1_,P_2_)⇒((D,D,D,D)⇒D, (D,D,D,D)⇒D),
    S_prime_1: SX_1_,
    S_prime_2: SX_2_,
    S_X_G: Sink[M, R],
    viz_graph: VizGraph)
  :RunnableGraph[R] = {
    // Build transitions
    val f_t = X_transition(Δt)
    val (f_ω_1, f_ω_2) = S_transition(G.toP_1_, G.toP_2_)
    // Build graph
    RunnableGraph.fromGraph(GraphDSL.create(S_X_G){ implicit b ⇒ S_X_G ⇒
      import GraphDSL.Implicits._
      // Collector LP's
      val LPc_1 = b.add(Merge[M](n * 2))
      LPc_1 ~> S_X_G
      // Build IN nodes
      val in_1 = viz_graph.addNode(new SPrimeInput_(S_prime_1, w=1, LPc_1))
      val in_2 = viz_graph.addNode(new SPrimeInput_(S_prime_2, w=2, LPc_1))
      // Init LP's
      var LPe_1 = viz_graph.addNode(new LPe_(in_1.r1, in_2.r1, f_t, f_ω_1, d=1, w=1, LPc_1))
      var LPe_2 = viz_graph.addNode(new LPe_(in_1.r2, in_2.r2, f_t, f_ω_2, d=1, w=2, LPc_1))
      // Build chine
      for(i ← 2 until n){
        // Next LP's
        val next_LPe_1 = viz_graph.addNode(new LPe_(LPe_1.r1, LPe_2.r1, f_t, f_ω_1, d=i, w=1, LPc_1))
        val next_LPe_2 = viz_graph.addNode(new LPe_(LPe_1.r2, LPe_2.r2, f_t, f_ω_2, d=i, w=2, LPc_1))
        LPe_1 = next_LPe_1
        LPe_2 = next_LPe_2}
      // Chine end
      LPe_1.r1._1 ~> b.add(Sink.ignore)
      LPe_1.r2._1 ~> b.add(Sink.ignore)
      LPe_2.r1._1 ~> b.add(Sink.ignore)
      LPe_2.r2._1 ~> b.add(Sink.ignore)
      ClosedShape})}

  // Parameters
  val n = 100
  val Δt = 0.1
  val S_prime_1 = SX_1_(t = 0.0, ω_1 = 0.0)
  val S_prime_2 = SX_2_(t = 0.0, ω_2 = 20.0)
  val G = G_(
    v_1 = 4,  // L
    v_2 = 8,  // L
    q_1 = 3,  // L/m
    q_2 = 2,  // L/m
    q_3 = 5,  // L/m
    q_4 = 3,  // L/m
    ω_3 = 10) // g/l
  val use_earlier_transition_function = true

  //Transition implementation
  def X_transition(Δt: D): D⇒D = t ⇒ t + Δt
  def S_earlier_transition(P_1: P_1_, P_2: P_2_): ((D,D,D,D)⇒D, (D,D,D,D)⇒D) = {
    def f_ω_1(ω_1: D, ω_2: D, t: D, t_next: D) =
      ω_1 + (t_next - t) * (((P_1.q_1 * P_1.ω_3) + (P_1.q_2 * ω_2) - (P_1.q_3 * ω_1)) / P_1.v_1)
    def f_ω_2(ω_1: D, ω_2: D, t: D, t_next: D) =
      ω_2 + (t_next - t) * (((P_2.q_3 * ω_1) - (P_2.q_2 * ω_2) - (P_2.q_4 * ω_2)) / P_2.v_2)
    (f_ω_1, f_ω_2)}
  def S_functional_transition(P_1: P_1_, P_2: P_2_): ((D,D,D,D)⇒D, (D,D,D,D)⇒D) = {
    val q = math.sqrt(105.0)
    def em(t: D) = math.exp(((q - 15.0) * t) / 16.0)
    def ep(t: D) = math.exp(-(((q + 15.0) * t) / 16.0))
    def f_ω_1(ω_1: D, ω_2: D, t_prev: D, t: D) =
      ((13.0 * em(t) * q) / 21.0) - ((13.0 * ep(t) * q) / 21.0) - (5.0 * em(t)) - (5.0 * ep(t)) + 10.0
    def f_ω_2(ω_1: D, ω_2: D, t_prev: D, t: D) =
      -((5.0 * em(t) * q) / 21.0) + ((5.0 * ep(t) * q) / 21.0) + (5.0 * em(t)) + (5.0 * ep(t)) + 10.0
      (f_ω_1, f_ω_2)}

  // Visualization
  val viz_graph = new VizGraph("C graph", width = 1200, height = 100)
  val chart = ChartRecorder2D(lines = Seq(("ω_1", Color.GREEN), ("ω_2", Color.RED), ("ω_3", Color.GRAY)), maxRange = 21)

  // Output
  val sink_S_X_G = Sink
    .fold[Set[M], M](Set()){
      case (set, M(ss1: SX_1_)) if set.exists{ case M(ss2: SX_2_) ⇒ ss1.t == ss2.t case _ ⇒ false} ⇒
        val ss2 = set.find{ case M(ss2: SX_2_) ⇒ ss1.t == ss2.t case _ ⇒ false}.get.ss.asInstanceOf[SX_2_]
        chart.addPoints(ss1.t, Seq(ss1.ω_1, ss2.ω_2, G.ω_3))
        println("M = " + M(ss1))
        set + M(ss1)
      case (set, M(ss2: SX_2_)) if set.exists{ case M(ss1: SX_1_) ⇒ ss2.t == ss1.t case _ ⇒ false} ⇒
        val ss1 = set.find{ case M(ss1: SX_1_) ⇒ ss2.t == ss1.t case _ ⇒ false}.get.ss.asInstanceOf[SX_1_]
        chart.addPoints(ss1.t, Seq(ss1.ω_1, ss2.ω_2, G.ω_3))
        println("M = " + M(ss2))
        set + M(ss2)
      case (set, m) ⇒
        println("M = " + m)
        set + m}

  // Build graph
  val C = if(use_earlier_transition_function)
    buildC(n, Δt, G, X_transition, S_earlier_transition, S_prime_1, S_prime_2, sink_S_X_G, viz_graph)
  else
    buildC(n, Δt, G, X_transition, S_functional_transition, S_prime_1, S_prime_2, sink_S_X_G, viz_graph)

  // Show graph
  viz_graph.show()

  // Run processing
  val system = ActorSystem("ExampleSys")
  val materializer = ActorMaterializer()(system)
  val mat = C.run()(materializer)
}
