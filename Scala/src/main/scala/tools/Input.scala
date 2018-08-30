/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
*                   Simulation with reactive streams                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package tools

import java.awt.event.KeyEvent
import java.awt.{KeyEventDispatcher, KeyboardFocusManager}

import utils.ScriptBase


/* Set of user input tools
 * Created 27.04.2018 author CAB */

trait Input { self: ScriptBase ⇒
  //Variables
  private var keyInput: Option[KeyEvent] = None
  //Helpers
  KeyboardFocusManager.getCurrentKeyboardFocusManager
    .addKeyEventDispatcher(e => {
      self.synchronized{keyInput = Some(e)}
      false})
  //Tools
  def swCheckAndReadChar(): Option[Char] = self.synchronized{
    val c = keyInput.map(_.getKeyChar)
    keyInput = None
    c}
  def swReset(): Unit = self.synchronized{
    keyInput = None}

  //TODO Add more

}
