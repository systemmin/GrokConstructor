package net.stoerr.grokconstructor.matcher

import javax.servlet.http.HttpServletRequest

import net.stoerr.grokconstructor.forms.{GrokPatternFormPart, LoglinesFormPart, MultilineFormPart}
import net.stoerr.grokconstructor.webframework.WebForm

/**
  * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
  * @since 17.02.13
  */
case class MatcherForm(request: HttpServletRequest) extends WebForm with GrokPatternFormPart with MultilineFormPart with LoglinesFormPart {

  val pattern = InputText("pattern")

  def patternEntry = pattern.inputText("应该匹配所有日志文件行的（未加引号！）模式。\" " +
    "+\n“（请记住，将搜索整个日志行/消息以查找此模式；如果您希望此模式与”" +
    "+\n“整行，用^$或\\A \\Z括起来。这会加快搜索速度，尤其是在找不到图案的情况下。", 180)

}
