# CMFTool Project

This distribution contains command-line tools and supporting libraries for working with NIEM and CMF artifacts.

## Contents

- `bin/` — launch scripts
- `lib/` — application and dependency jars
- `docs/` — HTML documentation and examples
- `docs/examples/` — sample input files and scripts

## Applications

This distribution may include one or more of these tools:

- `cmftool`
- `niemtran`
- `scheval`

Use the launcher scripts in `bin/` to run the tools.

## Documentation

* [Building message formats with CMFTool](./msgFormats.html) \
  This article describes how to use CMFTool to construct a *message format*.

## Examples

* [Request message specification](./examples/request) \
  A simple message type asking for a quantity of one or more items.

<style>
h1 { font-size: 14pt; }
h2,h3,h4 { font-size: 12pt;  }
h3,h4 { font-style: italic; font-weight: normal }
code { font-family: "Source Code Pro", "Liberation Mono", monospace; font-size: 11pt; }
pre { background-color:#f0f0f0; padding: 6px; page-break-after: avoid; }
pre > code { font-size: 9pt; margin-left:auto; margin-right:auto; page-break-after: avoid; }
pre.blk code { display: block; transform: translateY(-1.5em); }
pre.blk { padding-bottom: 0; }
figcaption { text-align:center; font-style:italic; margin-top: 10pt; margin-bottom:10pt;  page-break-before: avoid;  }
figcaption > a { color: #000 }
body { font-family: LiberationSans, Arial, Helvetica, sans-serif; font-size: 12pt; line-height: 1.2; }
</style>