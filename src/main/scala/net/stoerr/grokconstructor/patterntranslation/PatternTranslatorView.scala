package net.stoerr.grokconstructor.patterntranslation

import java.util.logging.Logger
import javax.servlet.http.HttpServletRequest

import net.stoerr.grokconstructor.RandomTryLibrary
import net.stoerr.grokconstructor.automatic.AutomaticDiscoveryView
import net.stoerr.grokconstructor.matcher.MatcherEntryView
import net.stoerr.grokconstructor.webframework.{WebView, WebViewWithHeaderAndSidebox}

import scala.util.{Failure, Random, Success, Try}
import scala.xml.NodeSeq

/**
 * Created by hps on 13.04.2016.
 */
class PatternTranslatorView(val request: HttpServletRequest) extends WebViewWithHeaderAndSidebox {

  private val logger = Logger.getLogger("PatternTranslatorView")

  override val title: String = "模式转换"
  val form = PatternTranslatorForm(request)

  override def action = PatternTranslatorView.path

  override def maintext: NodeSeq = <p>
    这会尝试从 log4j
    <a href="https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html">PatternLayout</a>
    格式生成
    <a href="http://logstash.net/docs/latest/filters/grok">grok 正则表达式</a>
    ，该格式解析由该格式生成的日志文件输出。您将需要使用
    <a href={fullpath(MatcherEntryView.path)}>matcher</a>
    检查和优化模式。
  </p> ++ <p>
    这是目前非常实验性的——不要指望它能工作或任何东西。:-) 请报告问题，如果可能的话，请提出如何将麻烦的占位符转换为适当的 grok 表达式的好建议。</p> ++
    <p>
      如果有人提出如何翻译不同占位符的好建议，那么将其扩展到其他日志记录库（如 logback 等）会相对容易。或多或少，log4j 模式转换的规范在
      <a href="https://github.com/stoerr/GrokConstructor/blob/master/Log4jsupport.txt">这里</a>
      。
    </p> ++
    <p>请输入log4j格式然后按</p> ++ submit("Go!")

  val translationResult: Option[Try[String]] = form.format.value.map(pat => Try(Log4jTranslator.translate(pat)))

  override def sidebox: NodeSeq = <p>你也可以用一个</p> ++ buttonanchor(PatternTranslatorView.path + "?randomize", "随机示例") ++
    (if (form.format.value.isEmpty || translationResult.isEmpty || translationResult.get.isFailure) <span/>
    else <p>您还可以通过调用匹配器来尝试构建的正则表达式。</p> ++ submit("去匹配", "matcher"))

  val examples = List("%d{dd.MM.yyyy HH:mm:ss},%m%n",
    "%-4r [%t] %-5p %c %x - %m%n",
    "%d{yyyyMMddHHmmss};%X{host};COMMONS;(%13F:%L);%X{gsid};%X{lsid};%-5p;%m%n",
    "[cc]%d{MMM-dd HH:mm:ss} %-14.14c{1}- %m%n",
    "%d{ABSOLUTE} | %-5p | %-10t | %-24.24c{1} | %-30.30C %4L | %m%n",
    "%d{dd.MM.yyyy HH:mm:ss,SSS} - %r [%-5p] %c %m%n",
    "%d{yyyy-MM-dd HH:mm:ss} %-5.5p [%-30c{1}] %-32X{sessionId} %X{requestId} - %m\n"
  )

  override def doforward: Option[Either[String, WebView]] =
    if (null != request.getParameter("randomize")) Some(Left(fullpath(PatternTranslatorView.path) + "?example=" + Random.nextInt(examples.size)))
    else if (null != request.getParameter("matcher")) {
      val view = new MatcherEntryView(request)
      view.form.pattern.value = Some(translationResult.get.get)
      Some(Right(view))
    } else None

  if (null != request.getParameter("example")) {
    val trial = examples(request.getParameter("example").toInt)
    form.format.value = Some(trial)
  }

  def formparts: NodeSeq = form.patternEntry ++ resultPart

  override def result: NodeSeq = <span/>

  def resultPart = translationResult.map {
    case Success(translated) =>
      form.result.value = Some(translated)
      table(row(form.result.inputTextArea("构建 grok 模式:", 180, 6, enabled = false)))
    case Failure(TranslationException(message)) =>
      table(warn(s"无法转换模式，因为 : $message"))
    case Failure(otherException) => throw otherException
  }.getOrElse(<span/>)

}


object PatternTranslatorView {

  val path = "/translator"

}
