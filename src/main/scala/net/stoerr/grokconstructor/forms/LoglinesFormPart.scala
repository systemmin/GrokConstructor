package net.stoerr.grokconstructor.forms

import net.stoerr.grokconstructor.webframework.WebForm

/**
 * 一些日志线的输入表单。
 * @author <a href="http://www.stoerr.net/">Hans-Peter Stoerr</a>
 * @since 01.03.13
 */
trait LoglinesFormPart extends WebForm {

  val loglines = InputText("loglines")

  def loglinesEntry = loglines.inputTextArea("您要匹配的一些日志行。请注意：对于构造算法，您应该使用与模式匹配的几行，并选择尽可能不同的行。这减少了搜索空间。越多越好（当然在合理范围内）。", 180, 20)

}
