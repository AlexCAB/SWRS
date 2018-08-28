/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package scripts.mixing.example

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import org.jzy3d.colors.Color
import tools.Plotting
import utils.ScriptBase
import scala.collection.immutable


/* Optimized reactive implementation of mixing problem
 * onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.1%20Logical%20processors&
 *   section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={4155DDB2-718A-4C14-AFC1-5103ED2BCCA3}&
 *   object-id={667A74AE-2CBE-04DF-1B0C-B3D3C90B410F}&10
 * Created 27.08.2018 author CAB */

object OptimizedReactiveImplementation  extends ScriptBase with Plotting {
  println(
    """ #### Optimized reactive implementation of mixing problem ####
      | X = [t]
      | Y = [ω_1, ω_2̂]
      | G = [v_1, v_2, q_1, q_2, q_3, q_4, ω_3]
    """.stripMargin)

  // Data definition
  trait SX_q_
  case class SX_1_(t: D, ω_1: D) extends SX_q_
  case class SX_2_(t: D, ω_2: D) extends SX_q_
  case class M(i: Long, ss: SX_q_) extends SX_q_
  case class P_1_(v_1: D, q_1: D, q_2: D, q_3: D, ω_3: D)
  case class P_2_(v_2: D, q_2: D, q_3: D, q_4: D)
  case class G_(v_1: D, v_2: D, q_1: D, q_2: D, q_3: D, q_4: D, ω_3: D){
    def toP_1_ = P_1_(v_1, q_1, q_2, q_3, ω_3)
    def toP_2_ = P_2_(v_2, q_2, q_3, q_4)}

  // Input definition
  class SPrime_(S_prime: SX_q_) {
    // Ship definition
    case class SPrimeShape(out1: Outlet[M], out2: Outlet[M], c: Outlet[M]) extends Shape {
      override val inlets: immutable.Seq[Inlet[_]] = List()
      override val outlets: immutable.Seq[Outlet[_]] = List(out1, out2, c)
      override def deepCopy() = SPrimeShape(out1.carbonCopy(), out2.carbonCopy(), c.carbonCopy())}
    // Graph definition
    val g: Graph[SPrimeShape, _] = GraphDSL.create(){ implicit b ⇒ import GraphDSL.Implicits._
      // Blocks definition
      val source = Source.single(M(i = 0, S_prime))
      val bcast = b.add(Broadcast[M](3))
      // Wiring
      source ~> bcast
      // expose port
      SPrimeShape(bcast.out(0), bcast.out(1), bcast.out(2))}}

  // Logical processor definition
  class LPe_(f_t: D⇒D, f_ω: (D,D,D,D) ⇒ D, w: Int) {
    // Ship definition
    case class LPeShape(
      in1: Inlet[M], li1: Inlet[M], in2: Inlet[M], li2: Inlet[M], lo1: Outlet[M], lo2: Outlet[M], c: Outlet[M])
    extends Shape {
      override val inlets: immutable.Seq[Inlet[_]] = List(in1, li1, in2, li2)
      override val outlets: immutable.Seq[Outlet[_]] = List(lo1, lo2, c)
      override def deepCopy() = LPeShape(
        in1.carbonCopy(), li1.carbonCopy(), in2.carbonCopy(), li2.carbonCopy(),
        lo1.carbonCopy(), lo2.carbonCopy(), c.carbonCopy())}
    // Graph definition
    val g: Graph[LPeShape, _] = GraphDSL.create(){ implicit b ⇒ import GraphDSL.Implicits._
      // Blocks definition
      val merge = b.add(Merge[M](4))
      val log = b.add(Flow[M].map{ m ⇒
        println(s"LPe_$w receive M = $m")
        m})
      val group = b.add(Flow[M]
        .statefulMapConcat{() ⇒ var ssMap = Map[Long, M]()
          m ⇒ m match {
            case M(i, ss1: SX_1_) if ssMap.contains(i) ⇒
              val m2 = ssMap(i)
              assert(m2.ss.isInstanceOf[SX_2_], "Second sub-state should have type SX_2_")
              List((M(i, ss1), m2))
            case M(i, ss2: SX_2_) if ssMap.contains(i) ⇒
              val m1 = ssMap(i)
              assert(m1.ss.isInstanceOf[SX_1_], "Second sub-state should have type SX_1_")
              List((m1,  M(i, ss2)))
            case nm ⇒
              ssMap += nm.i → nm
              List()}})
      val map = b.add(Flow[(M, M)]
        .map[M]{
        case (M(i1, ss1: SX_1_), M(i2, ss2: SX_2_)) ⇒
          // Eval next t and ω
          assert(i1 == i2, "i1 != i2")
          assert(ss1.t == ss2.t, "ss1.t != ss2.t")
          val i = i1 + 1
          val t = f_t(ss1.t)
          val ω = f_ω(ss1.ω_1, ss2.ω_2, ss1.t, t)
          // Visualisation
          Thread.sleep(100)
          // Build sub state
          w match{
            case 1 ⇒ M(i, SX_1_(t, ω))
            case 2 ⇒ M(i, SX_2_(t, ω))
            case _ ⇒ throw new AssertionError(s"w should be 1 or 2, w = $w")}
        case (m1, m2) ⇒ throw new AssertionError(s"M_1 not SX_1_ and/or M_2 not SX_2_, M_1 = $m1, M_2 = $m2")}
        .async)
      val bcast = b.add(Broadcast[M](3))
      // Wiring
      merge ~> log ~> group ~> map ~> bcast
      // Expose port
      LPeShape(merge.in(0), merge.in(1), merge.in(2), merge.in(3), bcast.out(0), bcast.out(1), bcast.out(2))}}

  // Graph C builder
  def buildC[R](
    n: Int,
    Δt: D,
    G: G_,
    X_transition: D⇒D⇒D,
    S_transition: (P_1_,P_2_)⇒((D,D,D,D)⇒D, (D,D,D,D)⇒D),
    S_prime_1: SX_1_,
    S_prime_2: SX_2_,
    S_X_G: Sink[M, R])
  :RunnableGraph[R] = {
    // Build transitions
    val f_t = X_transition(Δt)
    val (f_ω_1, f_ω_2) = S_transition(G.toP_1_, G.toP_2_)
    // Build graph
    RunnableGraph.fromGraph(GraphDSL.create(S_X_G){ implicit b ⇒ S_X_G ⇒
      import GraphDSL.Implicits._
      // Collector LP's
      val LPc_1 = b.add(Merge[M](4))
      LPc_1 ~> S_X_G
      // Build IN nodes
      val in_1 = b.add(new SPrime_(S_prime_1).g)
      val in_2 = b.add(new SPrime_(S_prime_2).g)
      // Build LP's
      val LPe_1 = b.add(new LPe_(f_t, f_ω_1, w=1).g)
      val LPe_2 = b.add(new LPe_(f_t, f_ω_2, w=2).g)
      // Wiring
      in_1.out1 ~> LPe_1.in1
      in_1.out2 ~> LPe_2.in1
      in_2.out1 ~> LPe_1.in2
      in_2.out2 ~> LPe_2.in2
      LPe_1.lo1 ~> LPe_2.li1
      LPe_1.lo2 ~> LPe_1.li1
      LPe_2.lo1 ~> LPe_2.li2
      LPe_2.lo2 ~> LPe_1.li2
      in_1.c ~> LPc_1
      in_2.c ~> LPc_1
      LPe_1.c ~> LPc_1
      LPe_2.c ~> LPc_1
      // Closed shape
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
  val use_earlier_transition_function = false

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

  // Inputs
  val source_S_prime_1 = Source.single(S_prime_1)
  val source_S_prime_2 = Source.single(S_prime_2)

  // Visualization
  val chart = ChartRecorder2D(lines = Seq(("ω_1", Color.GREEN), ("ω_2", Color.RED), ("ω_3", Color.GRAY)), maxRange = 21)

  // Output
  val sink_S_X_G = Sink
    .fold[Set[M], M](Set()){
    case (set, M(i, ss1: SX_1_)) if set.exists{ case M(_,ss2: SX_2_) ⇒ ss1.t == ss2.t case _ ⇒ false} ⇒
      val ss2 = set.find{ case M(_,ss2: SX_2_) ⇒ ss1.t == ss2.t case _ ⇒ false}.get.ss.asInstanceOf[SX_2_]
      chart.addPoints(ss1.t, Seq(ss1.ω_1, ss2.ω_2, G.ω_3))
      println(s"Collected M_1 = ${M(i, ss1)}, M_2 = ${M(i, ss2)}")
      set + M(i, ss1)
    case (set, M(i, ss2: SX_2_)) if set.exists{ case M(_, ss1: SX_1_) ⇒ ss2.t == ss1.t case _ ⇒ false} ⇒
      val ss1 = set.find{ case M(_, ss1: SX_1_) ⇒ ss2.t == ss1.t case _ ⇒ false}.get.ss.asInstanceOf[SX_1_]
      chart.addPoints(ss1.t, Seq(ss1.ω_1, ss2.ω_2, G.ω_3))
      println(s"Collected M_1 = ${M(i, ss1)}, M_2 = ${M(i, ss2)}")
      set + M(i, ss2)
    case (set, m) ⇒
      set + m}

  // Build graph
  val C = if(use_earlier_transition_function)
    buildC(n, Δt, G, X_transition, S_earlier_transition, S_prime_1, S_prime_2, sink_S_X_G)
  else
    buildC(n, Δt, G, X_transition, S_functional_transition, S_prime_1, S_prime_2, sink_S_X_G)

  // Run processing
  val system = ActorSystem("ExampleSys")
  val materializer = ActorMaterializer()(system)
  val mat = C.run()(materializer)
}
