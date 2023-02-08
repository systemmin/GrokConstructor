package net.stoerr.grokconstructor.automatic

import javax.servlet.http.HttpServletRequest

import net.stoerr.grokconstructor.automatic.AutomaticDiscoveryView.{FixedString, NamedRegex, RegexPart}
import net.stoerr.grokconstructor.webframework.{WebView, WebViewWithHeaderAndSidebox}
import net.stoerr.grokconstructor.{GrokPatternLibrary, JoniRegex, RandomTryLibrary, StartMatch}

import scala.xml.NodeSeq

/**
 * We try to find all sensible regular expressions consisting of grok patterns and fixed strings that
 * match all of a given collection of lines. The algorithm is roughly: in each step we look whether the first characters
 * of all rest-lines are equal and are not letters/digits. If they are, we take that for the regex. If they aren't we try to match all grok
 * regexes against the string. The regexes are partitioned into sets that match exactly the same prefixes of all
 * rest-lines, sort these according to the average length of the matches and try these.
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 08.03.13
 */
class AutomaticDiscoveryView(val request: HttpServletRequest) extends WebViewWithHeaderAndSidebox {

  lazy val namedRegexps: Map[String, JoniRegex] = form.grokPatternLibrary.map {
    case (name, regex) => name -> new JoniRegex(GrokPatternLibrary.replacePatterns(regex, form.grokPatternLibrary))
  }
  lazy val namedRegexpsList: List[(String, JoniRegex)] = namedRegexps.toList
  override val title: String = "自动 grok 发现"
  val form = AutomaticDiscoveryForm(request)
  /** We try at most this many calls to avoid endless loops because of
    * the combinatorical explosion */
  var callCountdown = 1000

  override def action: String = AutomaticDiscoveryView.path

  def maintext: NodeSeq = <p>这是 <a href="http://www.stoerr.net/">我</a> 第一次尝试支持创建<a href="http://logstash.net/docs/latest/filters/grok">grok 表达式</a>。它可能生成所有正则表达式，
    这些正则表达式由库中非字母数字和 grok 模式的固定字符串组成，并匹配所有给定的日志文件行集。
    如果 grok 库中有多个模式与每个日志行中的相同字符串匹配，它们将组合在一起并显示为下拉列表。
    不幸的是，可能的正则表达式的数量随着行的长度呈指数增长，因此这在实践中并不真正可用。
    因此，结果列表被截断为 200 个结果。<a href="http://en.wiktionary.org/wiki/your_mileage_may_vary">YMMV（因人而异）</a>。
  </p> ++ <p>
    请输入一些日志行，您希望为其生成可能的 grok 模式，然后按</p> ++
    submit("解析!")

  def sidebox: NodeSeq = <p>你也可以试试这个</p> ++ buttonanchor(AutomaticDiscoveryView.path + "?randomize", "随机示例")

  if (null != request.getParameter("example")) {
    val trial = RandomTryLibrary.example(request.getParameter("example").toInt)
    form.loglines.value = Some(trial.loglines)
    form.multilineRegex.value = trial.multiline
    form.multilineNegate.values = List(form.multilineNegate.name)
    form.groklibs.values = List("grok-patterns")
  }

  def formparts: NodeSeq = form.loglinesEntry ++ form.grokpatternEntry

  override def result: NodeSeq = {
    val linesOpt = form.multilineFilter(form.loglines.valueSplitToLines).toList
    resultTable(matchingRegexpStructures(linesOpt))
  }

  override def doforward: Option[Either[String, WebView]] = if (null == request.getParameter("randomize")) None
  else Some(Left(fullpath(AutomaticDiscoveryView.path) + "?example=" + RandomTryLibrary.randomExampleNumber()))

  def resultTable(results: Iterator[List[RegexPart]]): xml.Node = table(
    rowheader("最多 200 个可能匹配所有行的 grok 正则表达式组合") ++ results.take(200).toList.map {
      result =>
        row(result map {
          case FixedString(str) => <span>
            {'»' + str + '«'}
          </span>
          case NamedRegex(patterns) if (patterns.size == 1) => <span>
            {"%{" + patterns(0) + "}"}
          </span>
          case NamedRegex(patterns) => <select>
            {patterns.sorted map {
              pattern => <option>
                {"%{" + pattern + "}"}
              </option>
            }}
          </select>
        })
    })

  def matchingRegexpStructures(lines: List[String]): Iterator[List[RegexPart]] = {
    if (callCountdown <= 0) return Iterator(List(FixedString("SEARCH TRUNCATED")))
    callCountdown -= 1
    if (lines.find(!_.isEmpty).isEmpty) return Iterator(List())
    val commonPrefix = AutomaticDiscoveryView.biggestCommonPrefixExceptDigitsOrLetters(lines)
    if (0 < commonPrefix.length) {
      val restlines = lines.map(_.substring(commonPrefix.length))
      return matchingRegexpStructures(restlines).map(FixedString(commonPrefix) :: _)
    } else {
      val regexpand = for ((name, regex) <- namedRegexpsList) yield (name, lines.map(regex.matchStartOf))
      val candidatesThatMatchAllLines = regexpand.filter(_._2.find(_.isEmpty).isEmpty)
      val candidates = candidatesThatMatchAllLines.filterNot(_._2.find(_.get.length > 0).isEmpty)
      val candidateToMatches = candidates.map {
        case (name, matches) => (name, matches.map(_.get))
      }
      val candidatesGrouped: Map[List[StartMatch], List[String]] = candidateToMatches.groupBy(_._2).mapValues(_.map(_._1))
      val candidatesSorted = candidatesGrouped.toList.sortBy(-_._1.map(_.length).sum)
      val res = for ((matches, names) <- candidatesSorted) yield {
        val restlines = matches.map(_.rest)
        matchingRegexpStructures(restlines).map(NamedRegex(names) :: _)
      }
      return res.fold(Iterator())(_ ++ _)
    }
  }

}

object AutomaticDiscoveryView {

  val path = "/automatic"

  /** The longest string that is a prefix of all lines. */
  def biggestCommonPrefixExceptDigitsOrLetters(lines: List[String]): String =
    if (lines.size != 1) lines.reduce(commonPrefixExceptDigitsOrLetters)
    else wrapString(lines(0)).takeWhile(!_.isLetterOrDigit)

  def commonPrefixExceptDigitsOrLetters(str1: String, str2: String) =
    wrapString(str1).zip(wrapString(str2)).takeWhile(p => (p._1 == p._2 && !p._1.isLetterOrDigit)).map(_._1).mkString("")

  sealed trait RegexPart

  case class FixedString(str: String) extends RegexPart

  case class NamedRegex(regexps: List[String]) extends RegexPart

}
