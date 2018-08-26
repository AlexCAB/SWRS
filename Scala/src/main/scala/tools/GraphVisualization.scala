/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


package tools

import java.awt.Dimension

import utils.ScriptBase
import javax.swing.{JFrame, SwingUtilities, WindowConstants}
import org.graphstream.graph.{Edge, Node}
import org.graphstream.graph.implementations.SingleGraph
import org.graphstream.ui.view.Viewer


/* Set of graph visualization tools
 * Created 27.04.2018 author CAB */

trait GraphVisualization { _: ScriptBase ⇒
  //Interfaces
  trait VizNodeLike {
    //Variables
    private var node: Option[Node] = None
    private var _color = "gray"
    private var _size = 30
    //Hepers
    private def setAttr(attr: String): Unit =
      node.foreach{ n ⇒ SwingUtilities.invokeAndWait(() ⇒ n.setAttribute("ui.style", attr))}
    //Immutable properties
    val viz_id: String
    val viz_edges: List[(String, String)]
    val viz_xy: (Int, Int)
    //Mutable properties
    def viz_color: String = _color
    def viz_color_= (v: String):Unit = {
      _color = v
      setAttr(s"fill-color: $v;")}
    def viz_size: Int = _size
    def viz_size_= (v: Int):Unit = {
      _size = v
      setAttr(s"size: ${v}px;")}
    //Service methods
    private [tools] def setNode(n: Node): Unit = node = Some(n)}

  //Visualization implementations
  class VizGraph(title: String, width: Int = 800, height: Int = 600) {
    //Variables
    private var nodes = List[VizNodeLike]()
    //Init
    private val graph = new SingleGraph(title)
    //Methods
    def addNode[N <: VizNodeLike](node: N): N = {
      nodes :+= node
      node}
    def show(): Unit = {
      //Build graph
      nodes.foreach{ n ⇒
        //Add node
        val vn = graph.addNode(n.viz_id).asInstanceOf[Node]
        vn.setAttribute("xy", Integer.valueOf(n.viz_xy._1 * 100), Integer.valueOf(n.viz_xy._2 * 100))
        vn.setAttribute("ui.style", s"fill-color: ${n.viz_color}; size: ${n.viz_size}px;")
        n.setNode(vn)
        //Add edges
        n.viz_edges.foreach{ case (sId, tId) ⇒
          val e = graph.addEdge(sId + "_" + tId, sId, tId, true).asInstanceOf[Edge]
          e.setAttribute("ui.style", "fill-color: grey; size: 3px;")}}
      //Build window
      val viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD)
      val view = viewer.addDefaultView(false)
      view.setPreferredSize(new Dimension(width, height))
      val frame = new JFrame(title) {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
        setResizable(false)
        add(view)
        pack()}
      //Display
      frame.setVisible(true)
      frame.pack()}}

  //TODO Add more

}
