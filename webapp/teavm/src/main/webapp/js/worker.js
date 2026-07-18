/* Web Worker — runs TeaVM-compiled filter off the main thread */
importScripts('./app.js');

self.onmessage = function(e) {
  var d = e.data;
  var t0 = performance.now();
  var result;
  try {
    result = applyFilter(
      d.input,
      d.anonymizeKeys, d.pruneKeys,
      d.anonymizePaths, d.prunePaths,
      d.maxStringLength, d.maxSize,
      d.removeWhitespace,
      d.anonymizeMessage, d.pruneMessage,
      d.truncateMessage, d.maxPathMatches
    );
  } catch(e) {
    result = 'Error: ' + e.message;
  }
  var ms = (performance.now() - t0).toFixed(2);
  self.postMessage({ result: result, ms: ms, prettyPrint: d.prettyPrint, inputLen: d.inputLen });
};
