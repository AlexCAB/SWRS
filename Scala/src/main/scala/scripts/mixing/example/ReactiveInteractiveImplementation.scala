/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package scripts.mixing.example

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import org.jzy3d.colors.Color
import tools.{Input, Plotting}
import utils.ScriptBase

import scala.collection.immutable


/* Interactive optimized reactive implementation of mixing problem
 * onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.1%20Logical%20processors&
 *   section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={4155DDB2-718A-4C14-AFC1-5103ED2BCCA3}&
 *   object-id={667A74AE-2CBE-04DF-1B0C-B3D3C90B410F}&10
 * Created 27.08.2018 author CAB */

object ReactiveInteractiveImplementation extends ScriptBase with Plotting with Input {
  println(
    """ #### Interactive optimized reactive implementation of mixing problem ####
      | X = [t]
      | Y = [ω_1, ω_2̂]
      | G = [v_1, v_2, q_1, q_2, q_3, q_4, ω_3]
    """.stripMargin)

  // Data definition
  trait SX_q_{val t: D}
  case class SX_1_(t: D, ω_1: D) extends SX_q_
  case class SX_2_(t: D, ω_2: D) extends SX_q_
  case class SX_3_(t: D, ω_3: D) extends SX_q_
  case class M(i: Long, ss: SX_q_)
  case class P_1_(v_1: D, q_1: D, q_2: D, q_3: D)
  case class P_2_(v_2: D, q_2: D, q_3: D, q_4: D)
  case class G_(v_1: D, v_2: D, q_1: D, q_2: D, q_3: D, q_4: D){
    def toP_1_ = P_1_(v_1, q_1, q_2, q_3)
    def toP_2_ = P_2_(v_2, q_2, q_3, q_4)}

  // Input definition
  class SPrimeInput_(S_prime: SX_q_) {
    // Ship definition
    case class SPrimeShape(out1: Outlet[M], out2: Outlet[M], c: Outlet[M]) extends Shape {
      override val inlets: immutable.Seq[Inlet[_]] = List()
      override val outlets: immutable.Seq[Outlet[_]] = List(out1, out2, c)
      override def deepCopy() = SPrimeShape(out1.carbonCopy(), out2.carbonCopy(), c.carbonCopy())}
    // Graph definition
    val g: Graph[SPrimeShape, _] = GraphDSL.create(){ implicit b ⇒ import GraphDSL.Implicits._
      // Blocks definition
      val source = Source.single(M(i = 1, S_prime))
      val bcast = b.add(Broadcast[M](3))
      // Wiring
      source ~> bcast
      // expose port
      SPrimeShape(bcast.out(0), bcast.out(1), bcast.out(2))}}

  class TimeInput_(Δt: D, ω_3: ⇒D) {
    // Ship definition
    case class TimeShape(out1: Outlet[M], out2: Outlet[M], c: Outlet[M]) extends Shape {
      override val inlets: immutable.Seq[Inlet[_]] = List()
      override val outlets: immutable.Seq[Outlet[_]] = List(out1, out2, c)
      override def deepCopy() = TimeShape(out1.carbonCopy(), out2.carbonCopy(), c.carbonCopy())}
    // Graph definition
    val g: Graph[TimeShape, _] = GraphDSL.create(){ implicit b ⇒
      import GraphDSL.Implicits._, scala.concurrent.duration._
      // Blocks definition
      val source = Source
        .tick(initialDelay = 0.milli, interval = Δt.seconds, tick = Unit)
        .zipWithIndex
      val firstMsgSource = source.filter(_._2 <= 0).map(_ ⇒ M(0, SX_3_(.0, ω_3)))
      val restMsgSource = source.filter(_._2 > 0).map{case (_, i) ⇒ M(i, SX_3_(i * Δt, ω_3))}
      val bcast = b.add(Broadcast[M](3))
      val merge = b.add(Merge[M](2))
      // Wiring
      firstMsgSource ~> merge
      restMsgSource ~> bcast
      bcast.out(0) ~> merge
      // expose port
      TimeShape(bcast.out(1), bcast.out(2), merge.out)}}

  // Logical processor definition
  class LPe_(f_ω: (D,D,D,D,D) ⇒ D, w: Int) {
    // Ship definition
    case class LPeShape(
      si1: Inlet[M], li1: Inlet[M], si2: Inlet[M], li2: Inlet[M], ti: Inlet[M],
      lo1: Outlet[M], lo2: Outlet[M], c: Outlet[M])
    extends Shape {
      override val inlets: immutable.Seq[Inlet[_]] = List(si1, li1, si2, li2, ti)
      override val outlets: immutable.Seq[Outlet[_]] = List(lo1, lo2, c)
      override def deepCopy() = LPeShape(
        si1.carbonCopy(), li1.carbonCopy(), si2.carbonCopy(), li2.carbonCopy(), ti.carbonCopy(),
        lo1.carbonCopy(), lo2.carbonCopy(), c.carbonCopy())}
    // Graph definition
    val g: Graph[LPeShape, _] = GraphDSL.create(){ implicit b ⇒ import GraphDSL.Implicits._
      // Blocks definition
      val merge = b.add(Merge[M](5))
      val log = b.add(Flow[M].map{ m ⇒
        println(s"LPe_$w receive M = $m")
        m})
      val group = b.add(Flow[M]
        .statefulMapConcat{() ⇒
          var ss1Map = Map[Long, M]()
          var ss2Map = Map[Long, M]()
          var ss3Map = Map[Long, M]()
          m ⇒ {
            m match {
              case M(i, ss1: SX_1_) ⇒ ss1Map += i → M(i, ss1)
              case M(i, ss2: SX_2_) ⇒ ss2Map += i → M(i, ss2)
              case M(i, ss3: SX_3_) ⇒ ss3Map += i → M(i, ss3)
              case _ ⇒ throw new AssertionError(s"Unknown message M = $m")}
            if(ss1Map.contains(m.i) && ss2Map.contains(m.i) && ss3Map.contains(m.i)) {
              val r =  List((ss1Map(m.i), ss2Map(m.i), ss3Map(m.i)))
              ss1Map -= m.i
              ss2Map -= m.i
              ss3Map -= m.i
              r}
            else {
              List()}}})
      val map = b.add(Flow[(M, M, M)]
        .map[M]{
          case (M(i1, ss1: SX_1_), M(i2, ss2: SX_2_), M(i3, ss3: SX_3_)) ⇒
            // Eval next t and ω
            assert(i1 == i2 && i2 == i3, "i1 != i2 or i2 != i3")  //All input messages should have same iteration ID
            assert(ss1.t == ss2.t, "ss1.t != ss2.t")  //S' input should have previous time
            assert(ss1.t != ss3.t, "ss1.t == ss3.t")  //T input should have next time
            val i = i1 + 1
            val ω = f_ω(ss1.ω_1, ss2.ω_2, ss3.ω_3, ss1.t, ss3.t)
            // Build sub state
            w match{
              case 1 ⇒ M(i, SX_1_(ss3.t, ω))
              case 2 ⇒ M(i, SX_2_(ss3.t, ω))
              case _ ⇒ throw new AssertionError(s"w should be 1 or 2, w = $w")}
          case (m1, m2, m3) ⇒ throw new AssertionError(
            s"M_1 not SX_1_ and/or M_2 not SX_2_ and/or M_3 not SX_3_, M_1 = $m1, M_2 = $m2, M_3 = $m3")}
        .async)
      val bcast = b.add(Broadcast[M](3))
      // Wiring
      merge ~> log ~> group ~> map ~> bcast
      // Expose port
      LPeShape(
        merge.in(0), merge.in(1), merge.in(2), merge.in(3), merge.in(4),
        bcast.out(0), bcast.out(1), bcast.out(2))}}

  // Graph C builder
  def buildC[R](
    Δt: D,
    ω_3: ⇒D,
    G: G_,
    S_transition: (P_1_,P_2_)⇒((D,D,D,D,D)⇒D, (D,D,D,D,D)⇒D),
    S_prime_1: SX_1_,
    S_prime_2: SX_2_,
    S_X_G: Sink[M, R])
  :RunnableGraph[R] = {
    // Build transitions
    val (f_ω_1, f_ω_2) = S_transition(G.toP_1_, G.toP_2_)
    // Build graph
    RunnableGraph.fromGraph(GraphDSL.create(S_X_G){ implicit b ⇒ S_X_G ⇒
      import GraphDSL.Implicits._
      // Collector LP's
      val LPc_1 = b.add(Merge[M](5))
      LPc_1 ~> S_X_G
      // Build IN nodes
      val si_1 = b.add(new SPrimeInput_(S_prime_1).g)
      val si_2 = b.add(new SPrimeInput_(S_prime_2).g)
      val ti = b.add(new TimeInput_(Δt, ω_3).g)
      // Build LP's
      val LPe_1 = b.add(new LPe_(f_ω_1, w=1).g)
      val LPe_2 = b.add(new LPe_(f_ω_2, w=2).g)
      // Wiring
      si_1.out1 ~> LPe_1.si1
      si_1.out2 ~> LPe_2.si1
      si_2.out1 ~> LPe_1.si2
      si_2.out2 ~> LPe_2.si2
      LPe_1.lo1 ~> LPe_2.li1
      LPe_1.lo2 ~> LPe_1.li1
      LPe_2.lo1 ~> LPe_2.li2
      LPe_2.lo2 ~> LPe_1.li2
      ti.out1 ~> LPe_1.ti
      ti.out2 ~> LPe_2.ti
      si_1.c ~> LPc_1
      si_2.c ~> LPc_1
      LPe_1.c ~> LPc_1
      LPe_2.c ~> LPc_1
      ti.c ~> LPc_1
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
    q_4 = 3)  // L/m
  val init_ω_3 = 10.0
  val up_down_step = 1

  //Transition implementation
  def S_transition(P_1: P_1_, P_2: P_2_): ((D,D,D,D,D)⇒D, (D,D,D,D,D)⇒D) = {
    def f_ω_1(ω_1: D, ω_2: D, ω_3: D, t: D, t_next: D) =
      ω_1 + (t_next - t) * (((P_1.q_1 * ω_3) + (P_1.q_2 * ω_2) - (P_1.q_3 * ω_1)) / P_1.v_1)
    def f_ω_2(ω_1: D, ω_2: D, ω_3: D, t: D, t_next: D) =
      ω_2 + (t_next - t) * (((P_2.q_3 * ω_1) - (P_2.q_2 * ω_2) - (P_2.q_4 * ω_2)) / P_2.v_2)
    (f_ω_1, f_ω_2)}

  // Visualization
  val chart = ChartRecorder2D(lines = Seq(("ω_1", Color.GREEN), ("ω_2", Color.RED), ("ω_3", Color.GRAY)), maxRange = 21)

  // Input
  object ω_3_input {
    //Variables
    private var cur_ω_3 = init_ω_3
    //Get current ω_3
    def ω_3: D = {
      swCheckAndReadChar() match{
        case Some('u') ⇒ cur_ω_3 += up_down_step
        case Some('d') ⇒ cur_ω_3 -= up_down_step
        case _ ⇒}
      cur_ω_3}}

  // Output
  val sink_S_X_G = Sink.fold[(Map[D, SX_1_], Map[D, SX_2_], Map[D, SX_3_]), M]((Map(), Map(), Map())){
    case ((s1, s2, s3), m) ⇒
    println(s"Collected M = $m")
    val (us1, us2, us3) = m match{
      case M(_, ss1: SX_1_) ⇒ (s1 + (ss1.t → ss1), s2, s3)
      case M(_, ss2: SX_2_) ⇒ (s1, s2 + (ss2.t → ss2), s3)
      case M(_, ss3: SX_3_) ⇒ (s1, s2, s3 + (ss3.t → ss3))
      case _ ⇒ throw new AssertionError(s"Unknown message, M = $m")}
    if(us1.contains(m.ss.t) && us2.contains(m.ss.t) && us3.contains(m.ss.t)){
      chart.addPoints(m.ss.t, Seq(us1(m.ss.t).ω_1, us2(m.ss.t).ω_2, us3(m.ss.t).ω_3))}
    (us1, us2, us3)}

  // Build graph
  val C = buildC(Δt, ω_3_input.ω_3, G, S_transition, S_prime_1, S_prime_2, sink_S_X_G)

  // Run processing
  val system = ActorSystem("ExampleSys")
  val materializer = ActorMaterializer()(system)
  val mat = C.run()(materializer)
}
