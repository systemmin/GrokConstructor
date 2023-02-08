package net.stoerr.grokconstructor.forms

import java.util.logging.Logger

import net.stoerr.grokconstructor.GrokPatternLibrary
import net.stoerr.grokconstructor.webframework.WebForm

import scala.xml.NodeSeq

/**
 * Input(s) for grok patterns
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 17.02.13
 */
trait GrokPatternFormPart extends WebForm {

  private val logger = Logger.getLogger("GrokPatternFormPart")

  lazy val grokPatternLibrary: Map[String, String] =
    GrokPatternLibrary.mergePatternLibraries(groklibs.values, grokadditionalinput.value)
  val groklibs = InputMultipleChoice("groklibs", GrokPatternLibrary.grokPatternLibraryNames.map(keyToGrokLink).toMap, GrokPatternLibrary.grokPatternLibraryNames)
  val grokadditionalinput = InputText("grokadditional")

  if (grokadditionalinput.value.isDefined) logger.fine("grokadditionalinput: " + grokadditionalinput.value)

  def grokpatternEntry =
    <div class="ym-fbox-text">
      <label>请标记 您要使用的 来自 <a href="http://logstash.net/">logstash</a> v.2.4.0 的 <a href="http://logstash.net/docs/latest/filters/grok">grok Patterns</a>库。
      如果您使用任何其他模式，您可能想要使用 grok-patterns，因为它们依赖于那里定义的基本模式。</label>

    </div> ++ groklibs.checkboxes ++
      grokadditionalinput.inputTextArea("您还可以以与上面链接的模式文件相同的格式提供一些额外的 grok 模式库。在每一行你给出一个模式名称，一个空格和模式。例如：WORD \\b\\w+\\b", 180, 5)

  def grokhiddenfields: NodeSeq = groklibs.hiddenField ++ grokadditionalinput.hiddenField

  private def keyToGrokLink(key: String): (String, NodeSeq) =
    key -> <a href={request.getContextPath + "/groklib/" + key}>
      {key}
    </a>

}
