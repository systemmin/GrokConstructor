package net.stoerr.grokconstructor.incremental

import javax.servlet.http.HttpServletRequest

import net.stoerr.grokconstructor.RandomTryLibrary
import net.stoerr.grokconstructor.webframework.{WebView, WebViewWithHeaderAndSidebox}

import scala.xml.NodeSeq

/**
 * 输入 grok 模式增量构建的开始参数。
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 01.03.13
 */
class IncrementalConstructionInputView(val request: HttpServletRequest) extends WebViewWithHeaderAndSidebox {
  val title: String = "Grok 模式的增量构建"
  val form = IncrementalConstructionForm(request)

  def action: String = IncrementalConstructionStepView.path

  override def doforward: Option[Either[String, WebView]] = if (null == request.getParameter("randomize")) None
  else Some(Left(fullpath(IncrementalConstructionInputView.path) + "?example=" + RandomTryLibrary.randomExampleNumber()))

  if (null != request.getParameter("example")) {
    val trial = RandomTryLibrary.example(request.getParameter("example").toInt)
    form.loglines.value = Some(trial.loglines)
    form.multilineRegex.value = trial.multiline
    form.multilineNegate.values = List(form.multilineNegate.name)
    form.groklibs.values = List("grok-patterns", "java")
  }

  def maintext: NodeSeq = <p>
    您可以提供许多日志文件行，并逐步构建一个匹配所有这些行的<a href="http://logstash.net/docs/latest/filters/grok">grok 模式</a>。
    在每个步骤中，选择或输入与日志行的下一个逻辑段匹配的模式。
    这可以是固定字符串（例如分隔符）、grok模式库中的（可能命名的）
    模式或您明确指定的模式。也可以先应用<a href="http://logstash.net/docs/latest/filters/multiline">多行过滤器</a>。
  </p> ++
    <p>
      在下面的表格中，请输入您要为其创建 grok 模式的一些日志行，标记您要从中绘制模式的模式库，然后按
    </p> ++ submit("解析!")

  def sidebox: NodeSeq = <p>你也可以用一个</p> ++ buttonanchor(IncrementalConstructionInputView.path + "?randomize", "随机示例")

  def formparts: NodeSeq = form.loglinesEntry ++ form.grokpatternEntry ++ form.multilineEntry

  // missing: extra patterns by hand

  def result: NodeSeq = <span/>

}

object IncrementalConstructionInputView {
  val path = "/construction"
}
