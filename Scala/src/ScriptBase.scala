/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.analysis.AnalysisLauncher
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.maths.Coord3d
import org.jzy3d.plot3d.primitives.Scatter
import org.jzy3d.plot3d.rendering.canvas.Quality
import breeze.plot._


/* Set of helpers definitions for scala scripting
 * Created 18.12.2017 author CAB */

trait ScriptBase extends App{
  //Types
  type D = Double
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
    val f = Figure()
    val p = f.subplot(0)
    val (xs, ys) = data.unzip
    p += plot(xs, ys)
    p.xlabel = "X"
    p.ylabel = "Y"
    //Methods
    def show(): Unit = f.refresh()}}
