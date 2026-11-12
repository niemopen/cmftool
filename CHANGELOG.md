# Changelog

This changelog records releases for the CMFTool repository and its published components.

Version groups:

- `lib-cmf`, `app-cmftool`, and `app-niemtran` normally release together
- `lib-util` may release independently
- `app-scheval` may release independently
- the `allApps.zip` bundle may have its own version

## [bundle 1.2] - 2026-11-12

- **cmftool m2x** now generates XSD 1.1 when @substitutionGroup needs more than one QName [NDR issue #52](https://github.com/niemopen/niem-naming-design-rules/issues/52)
- Removed more technical debt from lib-util

## [bundle 1.1] - 2026-10-06

- Simple message formats in XML and JSON
- Message conversion from XML to JSON, and JSON to XML
- JSON Schema generation
- Documentation and worked examples in docs folder
