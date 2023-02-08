package net.stoerr.grokconstructor.incremental

import javax.servlet.http.HttpServletRequest

import net.stoerr.grokconstructor.forms.{GrokPatternFormPart, LoglinesFormPart, MultilineFormPart}
import net.stoerr.grokconstructor.webframework.WebForm

/**
 * 用于增量查找 grok 表达式的基本数据输入表单
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 01.03.13
 */
case class IncrementalConstructionForm(request: HttpServletRequest) extends WebForm with GrokPatternFormPart with MultilineFormPart with LoglinesFormPart {

  /** Contains the part of the regular expression that is constructed so far. Starts with \A and matches all loglines. */
  val constructedRegex = InputText("pattern")

  /** The next part of the regular expression. */
  val nextPart = InputText("nextPart")

  /** In case of grok expressions the name we want to give the next part */
  val nameOfNextPart = InputText("nameOfNextPart")

  /** If the user wants to input a regular expression per hand */
  val nextPartPerHand = InputText("nextPartPerHand")

  /** Value for nextPart that marks that nextPartPerHand is choosen. */
  val nextPartPerHandMarker = "ksdf8wej2349j_ThisIsAMarkerForNextPartThatNextPartPerHandWasChoosen"

}
