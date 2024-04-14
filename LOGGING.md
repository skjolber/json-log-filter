# Logging guide
Tips and tricks for logging.

The end result should be a valid JSON log statement with intact tracing headers, which is small enough to fit within the constraints of the log accumulation tool.

## Request-response logging
Technically a full JSON parser should be used to validate the JSON syntax of documents from external parties. This is however more costly, and strictly overkill for a lot of situations:

 * use of an API gateway or proxy which validates JSON syntax
 * clients known to produce valid JSON
   * trusted external clients (i.e. authenticated)
   * internal clients
 * responses known to be valid JSON
 * API walls between tracing info and request/response body and/or headers.

## Untrusted JSON post-processing
Performing two additional operations might be necessary:

 * validate document syntax (as JSON)
   * for raw inlining of JSON from untrusted sources in log statements
 * removing linebreaks (and possibly all extra whitespace)
   * for `one line per log-statement`, typically for console- and/or file logging

For a typical REST service, the above operations might be necessary for the (untrusted) incoming request payload, but not the (trusted) outgoing response payload. 

Note that
  
 * the `Jackson`-based processors in this project do both of these automatically, and 
 * most frameworks do databinding and/or schema-validation, so at some point the incoming request is known to be valid JSON. An ideal implementation takes advantage of this, logging as text if the databinding fails, otherwise logging as (filtered) JSON.
