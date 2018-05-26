/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package tools

import java.awt.Dimension

import utils.ScriptBase
import breeze.plot._
import org.jzy3d.analysis.{AbstractAnalysis, AnalysisLauncher}
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.maths.Coord3d
import org.jzy3d.plot3d.primitives.Scatter
import org.jzy3d.plot3d.rendering.canvas.Quality
import info.monitorenter.gui.chart.Chart2D
import info.monitorenter.gui.chart.IAxis.AxisTitle
import info.monitorenter.gui.chart.rangepolicies.RangePolicyMinimumViewport
import info.monitorenter.gui.chart.traces.Trace2DLtd
import info.monitorenter.util.Range
import javax.swing.JFrame
import javax.swing.WindowConstants


/* Set of plotting tools
 * Created 27.04.2018 author CAB */

trait Plotting { _: ScriptBase ⇒
  //Plotting
  case class Scatter3D(data: Seq[(D, D, D)], pointColor: Color = Color.RED) extends AbstractAnalysis {  //data: List[(X, Y, Z)]
    //Methods
    def init() {
      //Data
      val points = data.map{ case (x,y,z) ⇒ new Coord3d(x, y, z) }
      //Plot
      val colors = points.map(_ ⇒ pointColor).toArray
      val scatter = new Scatter(points.toArray, colors, 5f)
      chart = AWTChartComponentFactory.chart(Quality.Advanced)
      chart.getScene.add(scatter)}
    def show(): Unit = AnalysisLauncher.open(this)}
  case class Plot2D(data: Seq[(D, D)]) {  //data: List[(X, Y)]
    //Init
    private val f = Figure()
    private val p = f.subplot(0)
    private val (xs, ys) = data.unzip
    p += plot(xs, ys)
    p.xlabel = "X"
    p.ylabel = "Y"
    //Methods
    def show(): Unit = f.refresh()}
  case class MultiYPlot2D(xs: Seq[D], yss: Seq[(Seq[D], Color)]) {  //data: List[(X, Y)]
    //Init
    private val f = Figure()
    private val p = f.subplot(0)
    yss.foreach{ case (ys, c) ⇒
      p += plot(xs, ys, colorcode = (c.r * 255).toInt + "," + (c.g * 255).toInt + "," + (c.b * 255).toInt) }
    p.xlabel = "X"
    p.ylabel = "Y"
    //Methods
    def show(): Unit = f.refresh()}
  case class ChartRecorder2D(lines: Seq[(String, Color)], minRange: D = 0, maxRange: D = 1) {  //lines: number and parameters of the lines (name, color)
    //Init
    private val frame = new JFrame("ChartRecorder2D")
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    private val chart = new Chart2D
    chart.setPreferredSize(new Dimension(800, 400))
    chart.getAxisX.setAxisTitle(new AxisTitle("X"))
    chart.getAxisY.setAxisTitle(new AxisTitle("Y"))
    chart.getAxisX.setPaintGrid(true)
    chart.getAxisY.setPaintGrid(true)
    chart.getAxisY.setRangePolicy(new RangePolicyMinimumViewport(new Range(minRange, maxRange)))
    chart.setGridColor(java.awt.Color.LIGHT_GRAY)
    private val traces = lines.map{ case (name, color) ⇒
      val trace = new Trace2DLtd(200)
      trace.setName(name)
      trace.setColor(new java.awt.Color((color.r * 255).toInt, (color.g * 255).toInt, (color.b * 255).toInt))
      chart.addTrace(trace)
      trace}
    frame.getContentPane.add(chart)
    frame.pack()
    frame.setVisible(true)
    //Methods
    def addPoints(x: D, ys: Seq[D]): Unit =
      traces.zip(ys).foreach{case (t, y) ⇒ t.addPoint(x,y)}}

  //TODO Add more

}
