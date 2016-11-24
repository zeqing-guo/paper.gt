/**
  * Created by zqguo on 16-11-22.
  */
object Ref {
  var isDebug = true

  def title2DblpKey(title: String): String = {
    import scala.xml.Elem
    val titleEncode = java.net.URLEncoder.encode(title, "utf-8")
    val queryApi = s"http://dblp.org/search/publ/api?q=$titleEncode&h=1"
    if (isDebug) {
      println(s"[title2DblpKey] query for $title:\n$queryApi\n")
    }
    try {
      val content: Elem = xml.XML.loadString(IO.fetchHTML(queryApi))
      val dblpKey = (content \\ "url").headOption.map {
        (paperNode) =>
          val paperUrl = paperNode.text
          paperUrl.substring(lastNIndexOf(paperUrl, '/', 3) + 1)
      }.getOrElse("")
      if (isDebug) {
        println(s"[title2DblpKey] dblp key for $title:\n$dblpKey\n")
      }
      dblpKey
    } catch {
      case ex: Exception =>
        if (isDebug) {
          ex.printStackTrace()
        }
        ""
    }
  }

  def dblpKey2Bib(dblpKey: String): String = {
    val queryApi = s"http://dblp.dagstuhl.de/rec/bib0/$dblpKey.bib"
    try {
      val bib = IO.fetchHTML(queryApi)
      if (isDebug) {
        println(s"[dblpkey2Bib] bib for $dblpKey:\n$bib\n")
      }
      bib
    } catch {
      case ioe: java.io.IOException =>
        if (isDebug) {
          ioe.printStackTrace()
        }
        ""
      case ste: java.net.SocketException =>
        if (isDebug) {
          ste.printStackTrace()
        }
        ""
      case fnfe: java.io.FileNotFoundException =>
        if (isDebug) {
          fnfe.printStackTrace()
        }
        ""
    }
  }

  def dblpKey2Doi(dblpKey: String): Option[String] = {
    val queryApi = s"http://dblp.uni-trier.de/rec/xml/$dblpKey.xml"
    try {
      val content = xml.XML.loadString(IO.fetchHTML(queryApi))
      val doi = (content \\ "ee")
        .headOption
        .map(_.text)
        .map(doi => doi.substring(lastNIndexOf(doi, '/', 2) + 1))
      if (isDebug) {
        println(s"[dblpKey2Doi] $doi")
      }
      doi
    } catch {
      case ioe: java.io.IOException =>
        if (isDebug) {
          ioe.printStackTrace()
        }
        None
      case ste: java.net.SocketException =>
        if (isDebug) {
          ste.printStackTrace()
        }
        None
      case fnfe: java.io.FileNotFoundException =>
        if (isDebug) {
          fnfe.printStackTrace()
        }
        None
    }
  }

  def lastNIndexOf(s: String, c: Char, n: Int): Int = {
    var count = 1
    s.lastIndexWhere {
      char =>
        if (count < n) {
          if (char == c) {
            count += 1
          }
          false
        } else {
          c == char
        }
    }
  }
}
