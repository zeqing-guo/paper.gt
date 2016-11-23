import java.io._

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
    doiOption.exists {
      doi =>
        val queryUrl = s"$getSciHubUrl$doi"
        if (Ref.isDebug) {
          println(s"[downloadPdf] $queryUrl")
        }
        try {
          val dir = new File(targetDir)
          dir.mkdir()
          val html = sendRequest(queryUrl)
          var pdfUrl = html.split("frame")(2)
            .split("\"", 3)(1)
          if (pdfUrl.startsWith("/")) {
            pdfUrl = "http:" + pdfUrl
          }

          if (Ref.isDebug) {
            println(s"[downloadPdf] $pdfUrl")
          }
          val pdfName = s"$targetDir/$dblpName.pdf"
          sendRequestForFile(pdfUrl, filename = pdfName)
          new File(pdfName).exists()
        } catch {
          case ex => ex.printStackTrace()
            false
        }
    }
  }


  def sendRequest(url: String,
                  connectTimeout: Int = 5000,
                  readTimeout: Int = 5000,
                  requestMethod: String = "GET") = {
    import java.net.{InetSocketAddress, Proxy, URL, HttpURLConnection}
    val proxy: Proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.131.1.38", 22556))
    val conn = if (isProxy) {
      new URL(url).openConnection(proxy).asInstanceOf[HttpURLConnection]
    } else {
      new URL(url).openConnection().asInstanceOf[HttpURLConnection]
    }
    conn.setConnectTimeout(connectTimeout)
    conn.setReadTimeout(readTimeout)
    conn.setRequestMethod(requestMethod)
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.59 Safari/537.36")
    val inputStream = conn.getInputStream
    val content = io.Source.fromInputStream(inputStream).mkString
    if (inputStream != null) {
      inputStream.close()
    }
    content
  }

  def sendRequestForFile(url: String,
                         connectTimeout: Int = 5000,
                         readTimeout: Int = 5000,
                         requestMethod: String = "GET",
                         filename: String) = {
    import java.net.{Proxy, URL, HttpURLConnection, InetSocketAddress}
    var outStream: OutputStream = null
    var inputStream: InputStream = null
    try {
      val proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.131.1.38", 22556))
      val conn = if (isProxy) {
        new URL(url).openConnection(proxy).asInstanceOf[HttpURLConnection]
      } else {
        new URL(url).openConnection().asInstanceOf[HttpURLConnection]
      }
      conn.setConnectTimeout(connectTimeout)
      conn.setReadTimeout(readTimeout)
      conn.setRequestMethod(requestMethod)
      conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.59 Safari/537.36")
      inputStream = conn.getInputStream()
      outStream = new BufferedOutputStream(new FileOutputStream(filename))
      val byteArray = Stream.continually(inputStream.read).takeWhile(-1 !=).map(_.toByte).toArray
      outStream.write(byteArray)
    } catch {
      case e: Exception => println(e.printStackTrace())
    } finally {
      outStream.close
      inputStream.close
    }
  }

}
