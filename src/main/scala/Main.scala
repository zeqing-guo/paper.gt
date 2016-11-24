import java.io.File

/**
  * Created by zqguo on 16-11-23.
  */
object Main extends App {
  val parser = new scopt.OptionParser[Config]("ref") {
    head("ref", "0.1.0")

    opt[String]('t', "title").valueName("<title>").action((title, c) => c.copy(title = title)).
      text("search the paper with its <title>")

    opt[String]('i', "input").valueName("<file>").
      action((file, c) => c.copy(titlesFile = file)).
      text("read titles from <file> line by line")

    opt[String]('b', "bib").valueName("<bib target file>").
      action((file, c) => c.copy(bibTargetFile = file)).
      text("write the bibTex in <bib target file>")

    opt[String]('p', "pdf").valueName("<directory for pdfs>").
      action((dir, c) => c.copy(pdfDownloadDir = dir)).
      text("download papers into a <directory for pdfs>")

    opt[Unit]('d', "debug").action((_, c) => c.copy(debug = true))
      .text("print debug information")

    help("help").abbr("h").text("print this usage text")

    version("version").abbr("v").text("print ref version")
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      Ref.isDebug = config.debug
      if (config.title != "") {
        if (config.bibTargetFile != "") {
          // -t "paper name" -b "bibfile"
          // which will save paper's bib file in target file
          val bib = Ref.dblpKey2Bib(Ref.title2DblpKey(config.title))
          IO.printToFile(new File(config.bibTargetFile)) {
            p => if (bib == "") {
              p.println(s"Cannot find ${config.title} in http://dblp.uni-trier.de")
            } else {
              p.print(bib)
            }
          }
        } else if (config.pdfDownloadDir != "") {
          // -t "paper name" -d "download directory"
          // which will download paper into target directory
          val dblpKey = Ref.title2DblpKey(config.title)
          val dblpName = dblpKey.substring(dblpKey.lastIndexOf('/') + 1)
          val doi = Ref.dblpKey2Doi(dblpKey)
          if (!IO.downloadPdf(doi, config.pdfDownloadDir, dblpName)) {
            println(s"Cannot download ${config.title}")
          }
        } else {
          // -t "paper name"
          // which will print bib in console
          val bib = Ref.dblpKey2Bib(Ref.title2DblpKey(config.title))
          if (bib == "") {
            println(s"Cannot find ${config.title} in http://dblp.uni-trier.de")
          } else {
            println(bib)
          }
        }
      } else if (config.titlesFile != "") {
        val titles = IO.readFromFile(config.titlesFile)
        if (config.bibTargetFile != "") {
          // -i "file with paper list" -b "bibfile"
          // which will download papers' bib and save them in "bibfile"
          val bibs = titles.filter(_ != "").map {
            title => Ref.dblpKey2Bib(Ref.title2DblpKey(title))
          } mkString "\n"
          IO.printToFile(new File(config.bibTargetFile)) {
            p => if (bibs == "") {
              p.print(s"Cannot find ${config.title} in http://dblp.uni-trier.de")
            } else {
              p.print(bibs)
            }
          }
        } else if (config.pdfDownloadDir != "") {
          // -i "file with paper list" -p "download directory"
          // which will download papers and save them in target directory
          titles.foreach {
            title =>
              val dblpKey = Ref.title2DblpKey(title)
              val dblpName = dblpKey.substring(dblpKey.lastIndexOf('/') + 1)
              val doi = Ref.dblpKey2Doi(dblpKey)
              if (!IO.downloadPdf(doi, config.pdfDownloadDir, dblpName)) {
                println(s"Cannot download ${config.title}")
              }
          }
        } else {
          // -i "file with paper list" -d "download directory"
          // which will print bibs in console
          val bibs = titles.filter(_ != "").map {
            title => Ref.dblpKey2Bib(Ref.title2DblpKey(title))
          } mkString "\n"
          if (bibs == "") {
            print(s"Cannot find ${config.title} in http://dblp.uni-trier.de")
          } else {
            print(bibs)
          }
        }
      } else {
        // other input arguments is invalid
        println("Try --help for more information.")
      }
    case None => //pass
  }
}
