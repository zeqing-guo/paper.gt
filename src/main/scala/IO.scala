import java.io._
import java.net.Proxy

/**
  * Created by zqguo on 16-11-23.
  */
object IO {
  var isProxy = false
  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } catch {
      case ex: Exception =>
        if (Ref.isDebug) {
          ex.printStackTrace()
        }
    } finally { p.close() }
  }

  def readFromFile(f: String): List[String] = {
    val source = scala.io.Source.fromFile(f)
    try {
      source.getLines().toList
    } catch {
      case ex: Exception =>
        if (Ref.isDebug) {
          ex.printStackTrace()
        }
        List()
    } finally {
      source.close()
    }
  }

  def getSciHubUrl: String = {
    "http://sci-hub.cc/"
  }

  def downloadPdf(doiOption: Option[String], targetDir: String, dblpName: String): Boolean = {
    if (doiOption.isDefined) {
      val doi = doiOption.get
      val queryUrl = s"$getSciHubUrl$doi"
      if (Ref.isDebug) {
        println(s"[downloadPdf] queryUrl = $queryUrl")
      }
      val pdfUrl = fetchHTML(queryUrl)
      try {
        if (Ref.isDebug) {
          println(s"[downloadPdf] pdfUrl = $pdfUrl")
        }
        val dir = new File(targetDir)
        dir.mkdir()
        val pdfName = s"$targetDir/$dblpName.pdf"
        cleanly[ProxyFetcher, Unit](ProxyFetcher.getInstance(null, 0))(_.close()) {
          proxyFetcher =>
            var connectCount = 1
            while (connectCount <= 5) {
              val helloProxy = proxyFetcher.getProxy()
              val proxy = new Proxy(helloProxy.getType, helloProxy.getAddress)
              if (!fetchBinaryFile(pdfUrl, proxy = Some(proxy), filename = pdfName)) {
                connectCount += 1
              }
            }
        }
        new File(pdfName).exists()
      } catch {
        case ex: Exception =>
          if (Ref.isDebug) {
            ex.printStackTrace()
          }
      }
    }
    false
  }

  def fetchWebContent[T](url: String, connectTimeout: Int = 5000, readTimeout: Int = 5000, requestMethod: String = "GET"
                         , proxy: Option[Proxy] = None)(op: InputStream => T): Option[T] = {
    import java.net.{URL, HttpURLConnection}
    var inputStream: InputStream = null
    try {
      val conn = if (proxy.isDefined) {
        new URL(url).openConnection(proxy.get).asInstanceOf[HttpURLConnection]
      } else {
        new URL(url).openConnection().asInstanceOf[HttpURLConnection]
      }
      conn.setConnectTimeout(connectTimeout)
      conn.setReadTimeout(readTimeout)
      conn.setRequestMethod(requestMethod)
      conn.setRequestProperty("User-Agent"
        , "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.59 Safari/537.36")
      if (conn.getResponseCode < 400) {
        inputStream = conn.getInputStream()
        Some(op(inputStream))
      } else {
        None
      }
    } catch {
      case ex: Exception =>
        if (Ref.isDebug) {
          ex.printStackTrace()
        }
    } finally {
      inputStream.close()
    }
    None
  }

  def fetchHTML(url: String, connectTimeout: Int = 5000, readTimeout: Int = 5000
                , requestMethod: String = "GET", proxy: Option[Proxy] = None): String =
    fetchWebContent(url, connectTimeout, readTimeout, requestMethod, proxy)(io.Source.fromInputStream(_).mkString)
      .getOrElse("")

  def fetchBinaryFile(url: String, connectTimeout: Int = 5000, readTimeout: Int = 5000,
                      requestMethod: String = "GET", proxy: Option[Proxy] = None, filename: String): Boolean = {
    fetchWebContent(url, connectTimeout, readTimeout, requestMethod, proxy) {
      inputStream =>
        cleanly(new BufferedOutputStream(new FileOutputStream(filename)))(_.close()) {
          outStream =>
            val byteArray = Stream.continually(inputStream.read).takeWhile(-1 !=).map(_.toByte).toArray
            outStream.write(byteArray)
        }.isRight
    }.getOrElse(false)
  }

  // Code from stackoverflow:
  // http://stackoverflow.com/questions/8865754/scala-finally-block-closing-flushing-resource#answer-8865994
  def cleanly[A, B](resource: => A)(cleanup: A => Unit)(code: A => B): Either[Exception, B] = {
    try {
      val r = resource
      try {
        Right(code(r))
      } finally {
        cleanup(r)
      }
    }
    catch {
      case e: Exception => Left(e)
    }
  }

}
