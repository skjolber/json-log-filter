# json-log-filter-test
Test support for JSON filters. 

Contains utilities for support of a directory-structure where to-be filtered JSON documents are stored in their original form in a parent folder (input).

The expected filtering results (output) are in subfolders along with a file `filter.properties` which describes the filter operation.

If an output folder does not contain all the JSON documents in the input folder, a noop operation is assumed (output equal to input).

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

