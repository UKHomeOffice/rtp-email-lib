package uk.gov.homeoffice.domain.core.email

import WordWrap._
import scala.io.Source
import scala.xml.parsing.XhtmlParser

object HtmlStripper {

  def bodyText(htmlString: String) = {

    val html = XhtmlParser(Source.fromString(removeDoctype(htmlString)))

    val bodyText = (html \\ "body").text

    val trimmed = bodyText.split("""\n""").map(_.replaceAll("""^\s+""", "").trim).mkString(
"""
|
""".stripMargin)


    val multiLinesStripped =  trimmed.replaceAll("""^\s+""", "").replaceAll(" +", " ").replaceAll("""\s*\n\s+""",
"""
|
""".stripMargin)

    val bodyTexTrimmedWithHeadings = multiLinesStripped.replaceAll(
"""Contact us
|
""".stripMargin,
"""
|--------------------------------------------------------------
|Contact us
|--------------------------------------------------------------
""".stripMargin).replaceAll("""This email and any attachments transmitted with it are intended for the named recipient only. If you receive this email in error please notify us immediately and then delete the email and any attachments. It is the responsibility of the recipient to check for the presence of viruses before opening any attachments.""",
"""
|--------------------------------------------------------------
|--------------------------------------------------------------
|This email and any attachments transmitted with it are intended for the named recipient only. If you receive this email in error please notify us immediately and then delete the email and any attachments. It is the responsibility of the recipient to check for the presence of viruses before opening any attachments.
""".stripMargin)

    bodyTexTrimmedWithHeadings.wrap
  }

  def removeDoctype(htmlString: String) = htmlString.split("""<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">""")(1).replaceAll("(?m)^[ \t]*\r?\n", "")
}

object WordWrap {
  val wrapRegEx = """(.{1,80})\s""".r

  implicit class StringImprovements(s: String) {
    def wrap: String = wrapRegEx.replaceAllIn(s, m => m.group(1) + "\n")
  }
}
