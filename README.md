# paper.gt
A useful paper tool for computer science

## Usage

```
> java -jar ref.jar -h
ref 0.1.0
Usage: ref [options]

  -i, --input <file>       read titles from <file> line by line
  -t, --title <title>      search the paper with its <title>
  -b, --bib <bib target file>
                           write the bibTex in <bib target file>
  -p, --pdf <directory for pdfs>
                           download papers into a <directory for pdfs>
  -d, --debug              print debug information
  -h, --help               print this usage text
  -v, --version            print ref version

```

## Features

- :white_check_mark: Get bibtex from paper's name
- :white_check_mark: Get pdf from paper's name
- :white_check_mark: Deal with many papers at one time

## TODO

- [ ] Proxy
- [ ] Concurrency
- [ ] Robustness
- [ ] Support for conference and journals

