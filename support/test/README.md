# json-log-filter-test
Test support for JSON filters. 

This module contains utilities for support of a directory-structure where to-be filtered JSON documents are stored in their original form in a parent folder (input).

The expected filtering results (output) are in subfolders along with a file `filter.properties` which describes the filter operation.

If an output folder does not contain all the JSON documents in the input folder, a noop operation is assumed (output equal to input).

## Example

Take input file `/object/object1.json` containing

```json
{"grandparent":{"parent":{"child1":"text","child2":true,"child3":{"key":"value"}}}}
```

which we want to test for anon filtering. 

Then make the a subfolder `anon` with the filter properties file `/object/anon/filter.json` containing an `anonymizeFilters` property:

```properties
anonymizeFilters=/grandparent/parent/child1
```

This filter will affect the input, so also create an output file `/object/anon/object1.json` with contents

```json
{"grandparent":{"parent":{"child1":"*","child2":true,"child3":{"key":"value"}}}}
```

## Filter properties

```properties
pruneFilters=/key
anonymizeFilters=/key
maxStringLength=4
maxPathMatches=1
```

Max size is handled separately.

## Test cases

 * array
 * boolean
 * maxSize
 * number
 * object
 * text
 
## Test dimensions
Combinations of the following:

 * prune
 * anonymize
 * max string length
 * max path matches
 * max size

