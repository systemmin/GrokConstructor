package net.stoerr.grokconstructor.incremental

import com.google.apphosting.api.DeadlineExceededException

import javax.servlet.http.HttpServletRequest
import net.stoerr.grokconstructor.matcher.MatcherEntryView
import net.stoerr.grokconstructor.webframework.{WebView, WebViewWithHeaderAndSidebox}
import net.stoerr.grokconstructor.{GrokPatternLibrary, JoniRegex, JoniRegexQuoter, RandomTryLibrary, TimeoutException}
import org.joni.exception.SyntaxException

import scala.collection.immutable.NumericRange
import scala.xml.{NodeSeq, Text}

/**
  * Performs a step in the incremental construction of the grok pattern.
  *
  * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
  * @since 02.03.13
  */
class IncrementalConstructionStepView(val request: HttpServletRequest) extends WebViewWithHeaderAndSidebox {

  override val title: String = "正在进行的 Grok 模式增量构建"
  val form = IncrementalConstructionForm(request)
  val currentRegex = form.constructedRegex.value.getOrElse("\\A") + getNamedNextPartOrEmpty
  val currentJoniRegex = JoniRegex(GrokPatternLibrary.replacePatterns(currentRegex, form.grokPatternLibrary))
  val logLines: Seq[String] = form.multilineFilter(form.loglines.valueSplitToLines).toIndexedSeq
  val loglinesSplitted: Seq[(String, String)] = logLines.map({
    line =>
      val jmatch = currentJoniRegex.matchStartOf(line)
      (jmatch.get.matched, jmatch.get.rest)
  })
  val loglineRests: Seq[String] = loglinesSplitted.map(_._2)
  val constructionDone = loglineRests.forall(_.isEmpty)

  val groknameToMatches: List[(String, List[String])] = try {
    for {
      grokname <- form.grokPatternLibrary.keys.toList
      regex = JoniRegex(GrokPatternLibrary.replacePatterns("%{" + grokname + "}", form.grokPatternLibrary))
      restlinematchOptions = loglineRests.map(regex.matchStartOf)
      if !restlinematchOptions.exists(_.isEmpty)
      /* In some cases a suggestion matches the rest of the line, but not as a continuation for the full line.
        * For example: \\tb with current regex a\\t has restline b , which matches %{WORD} but that has a word boundary. */
      newregex = new JoniRegex(currentJoniRegex.regex + regex.regex)
      if logLines.map(newregex.matchStartOf).forall(_.isDefined)
      restlinematches: List[String] = restlinematchOptions.map(_.get.matched).toList
    } yield (grokname, restlinematches)
  } catch {
    case timeoutException @ (_ : DeadlineExceededException | _ : InterruptedException)  =>
      throw new TimeoutException("Timeout executing the search for the next pattern.\n" +
        "Number one recommendation is to input more and more diverse log lines, which should all be matched by the pattern, into the log lines field." +
        "That restricts the search space - the more the better (within reasonable limits, of course).", timeoutException)
  }

  // TODO missing: add extra patterns by hand later
  /** List of pairs of a list of groknames that have identical matches on the restlines to the list of matches. */
  val groknameListToMatches: List[(List[String], List[String])] = groknameToMatches.groupBy(_._2).map(p => (p._2.map
  (_._1), p._1)).toList
  form.constructedRegex.value = Some(currentRegex)
  /** groknameListToMatches that have at least one nonempty match, sorted by the sum of the lengths of the matches. */
  val groknameListToMatchesCleanedup = groknameListToMatches.filter(_._2.exists(!_.isEmpty)).sortBy(-_._2.map(_
    .length).sum)

  private val syntaxErrorInNextPartPerHand: Option[String] = try {
    JoniRegex(form.nextPartPerHand.value.getOrElse(""))
    None
  } catch {
    case patternSyntaxException: SyntaxException =>
      Some(patternSyntaxException.getMessage)
  }

  form.nameOfNextPart.value = None // reset for next form display
  form.nextPartPerHand.value = None

  override def action: String =
    if (!constructionDone) IncrementalConstructionStepView.path
    else MatcherEntryView.path

  override def doforward: Option[Either[String, WebView]] =
    if (null != request.getParameter("randomize"))
      Some(Left(IncrementalConstructionInputView.path + "?example=" + RandomTryLibrary.randomExampleNumber()))
    else if (null != request.getParameter("matchrests")) {
      val view = new MatcherEntryView(request)
      view.form.loglines.value = Some(loglineRests.mkString("\n"))
      Some(Right(view))
    } else None

  def maintext: NodeSeq = if (!constructionDone) <p>请为 grok 模式选择下一个组件。您可以选择固定字符串（例如分隔符）、
    grok 模式库中的（可能命名的）模式或您明确指定的模式。您可以使用浏览器的后退按钮重试（使用表单重新提交）。做出选择并按下</p> ++ submit("继续!")
  else <p>已成功匹配所有日志行。可以从下面的表单字段复制正则表达式。
    您还可以通过调用匹配器来尝试构建的正则表达式.</p> ++ submit("去匹配")

  def sidebox: NodeSeq = <p>要尝试正则表达式如何对不匹配的剩余行</p> ++ submit("匹配 " +
    "剩余行!", "matchrests", "_blank")

  override def result: NodeSeq = <span/>

  def formparts: NodeSeq = form.constructedRegex.inputText("到目前为止构造的正则表达式：", 180, enabled =
    false) ++
    form.loglines.hiddenField ++
    form.constructedRegex.hiddenField ++
    form.grokhiddenfields ++
    form.multilinehiddenfields ++
    (if (syntaxErrorInNextPartPerHand.isDefined)
      <p class="box error">Syntax error in the handmade regex:
        <br/>{syntaxErrorInNextPartPerHand.get}
      </p>
    else <span/>) ++
    selectionPart

  def selectionPart: NodeSeq = {
    val alreadymatchedtable = table(
      rowheader2("已匹配", "未匹配的其余日志行匹配") ++
        loglinesSplitted.map {
          case (start, rest) => row2(<code>
            {start}
          </code>, <code>
            {visibleWhitespaces(rest)}
          </code>)
        }
    )
    if (!constructionDone) {
      alreadymatchedtable ++
        formsection("要选择正则表达式的延续，您可以选择一个固定的字符串，该字符串对于所有日志文件行休息都是通用的作为分隔符：") ++
        <div class="ym-fbox-check">
          {commonprefixesOfLoglineRests.map(p => form.nextPart.radiobutton(JoniRegexQuoter.quote(p), <code>
          {'»' + visibleWhitespaces(p) + '«'}
        </code>)).reduceOption(_ ++ _).getOrElse(<span/>)}
        </div> ++
        formsection("o或者从 grok 库中选择以下表达式之一来匹配日志行的一部分：") ++
        form.nameOfNextPart.inputText("可选：为 grok 表达式命名以检索它的匹配值", 20,
          1) ++
        table(
          rowheader2("Grok 表达式", "在其余日志行的开头匹配") ++
            groknameListToMatchesCleanedup.map(grokoption)) ++
        formsection("或者您可以输入将匹配所有日志文件行的下一部分的正则表达式：") ++
        <div class="ym-fbox-check">
          {form.nextPart.radiobutton(form.nextPartPerHandMarker, "继续手工制作的正则表达式")}
        </div> ++
        form.nextPartPerHand.inputText("下一个组件的正则表达式：", 170)
    } else alreadymatchedtable
  }

  private def commonprefixesOfLoglineRests: Iterator[String] = {
    val biggestprefix = biggestCommonPrefix(loglineRests)
    NumericRange.inclusive(1, biggestprefix.length, 1).toIterator
      .map(biggestprefix.substring(0, _))
  }

  /** The longest string that is a prefix of all lines. */
  private def biggestCommonPrefix(lines: Seq[String]): String =
  if (lines.size > 1) lines.reduce(commonPrefix) else lines.head

  // unfortunately wrapString collides with TableMaker.stringToNode , so we use it explicitly
  private def commonPrefix(str1: String, str2: String) = wrapString(str1).zip(wrapString(str2)).takeWhile(p => (p._1
    == p._2)).map(_._1).mkString("")

  def grokoption(grokopt: (List[String], List[String])) = grokopt match {
    case (groknames, restlinematches) =>
      row2(
        <div class="ym-fbox-check">
          {groknames.sorted.map(grokname =>
          form.nextPart.radiobutton("%{" + grokname + "}", <code/>.copy(child = new Text("%{" + grokname + "}")),
            form.grokPatternLibrary(grokname)
          ))
          .reduce(_ ++ _)}
        </div>, <pre/>.copy(child = new Text(visibleWhitespaces(restlinematches.mkString("\n"))))
      )
  }

  private def getNamedNextPartOrEmpty = {
    val nextPart = form.nextPart.value.getOrElse("")
    if (nextPart == form.nextPartPerHandMarker) {
      try {
        JoniRegex(form.nextPartPerHand.value.getOrElse("")).regex
      } catch {
        case _: SyntaxException =>
          ""
      }
    }
    else form.nameOfNextPart.value match {
      case None => nextPart
      case Some(name) => nextPart.replaceFirst( """^%\{(\w+)}$""", """%{$1:""" + name + "}")
    }
  }

}

object IncrementalConstructionStepView {
  val path = "/constructionstep"
}
