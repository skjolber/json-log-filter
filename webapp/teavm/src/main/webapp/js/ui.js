/* ── TeaVM bridge ─────────────────────────────────────────────
   Capture @JSExport globals immediately after app.js loads so no
   later page function can shadow the names.
   applyFilter is also captured for the synchronous fallback path. */
var _teaVMApplyFilter    = applyFilter;
var _teaVMGetFilterClass = getFilterClass;
window.JsonFilterApp = {
  getFilterClass: function() { return _teaVMGetFilterClass.apply(null, arguments); }
};

/* ── Web Worker — off-main-thread filtering ───────────────── */
var _filterWorker = null;
var _workerBusy   = false;
var _pendingJob   = null; /* only the latest queued job matters */

try {
  _filterWorker = new Worker('js/worker.js');
  _filterWorker.onmessage = function(e) {
    _workerBusy = false;
    _applyFilterResult(e.data.result, e.data.ms, e.data.prettyPrint, e.data.inputLen);
    if (_pendingJob) {
      var job = _pendingJob;
      _pendingJob = null;
      _dispatchJob(job);
    }
  };
  _filterWorker.onerror = function(e) {
    _workerBusy = false;
    document.getElementById('status').textContent = 'Worker error: ' + e.message;
    if (_pendingJob) { var job = _pendingJob; _pendingJob = null; _dispatchJob(job); }
  };
} catch(e) {
  /* file:// origin or other restriction — worker unavailable, use sync fallback */
}

function _dispatchJob(job) {
  if (_filterWorker) {
    _workerBusy = true;
    _filterWorker.postMessage(job);
    return;
  }
  /* Synchronous fallback (e.g. file:// protocol) */
  var t0 = performance.now();
  var result;
  try {
    result = _teaVMApplyFilter(
      job.input,
      job.anonymizeKeys, job.pruneKeys,
      job.anonymizePaths, job.prunePaths,
      job.maxStringLength, job.maxSize,
      job.removeWhitespace,
      job.anonymizeMessage, job.pruneMessage,
      job.truncateMessage, job.maxPathMatches
    );
  } catch(e) {
    result = 'Error: ' + e.message;
  }
  _applyFilterResult(result, (performance.now() - t0).toFixed(2), job.prettyPrint, job.inputLen);
  if (_pendingJob) { var next = _pendingJob; _pendingJob = null; _dispatchJob(next); }
}

function _collectArgs() {
  return {
    input:          val('inputJson'),
    anonymizeKeys:  val('anonymizeKeys'),
    pruneKeys:      val('pruneKeys'),
    anonymizePaths: val('anonymizePaths'),
    prunePaths:     val('prunePaths'),
    maxStringLength: intVal('maxStringLength'),
    maxSize:        intVal('maxSize'),
    removeWhitespace: boolVal('removeWhitespace'),
    anonymizeMessage: val('anonymizeMessage'),
    pruneMessage:   val('pruneMessage'),
    truncateMessage: val('truncateMessage'),
    maxPathMatches: intVal('maxPathMatches'),
    prettyPrint:    boolVal('prettyPrint'),
    inputLen:       val('inputJson').length
  };
}

function _applyFilterResult(result, ms, prettyPrint, inputLen) {
  var out    = document.getElementById('outputJson');
  var status = document.getElementById('status');
  var timing = document.getElementById('filterTiming');

  var filteredLen = result ? result.length : 0; /* size straight from the filter */

  if (prettyPrint && result && !result.startsWith('Error:')) {
    try { result = JSON.stringify(JSON.parse(result), null, 2); } catch(e) {}
  }

  status.textContent = '';
  out.className      = 'code-input';
  out.value = result;
  document.getElementById('outputSizer').textContent = (result || '') + '\n';
  updateOutputHighlight(result, false);

  if (result && result.startsWith('Error:')) {
    out.classList.add('error');
    updateOutputHighlight(result, true);
    status.textContent = result;
    timing.textContent = '';
    updateOutputCount(inputLen, -1, -1);
  } else {
    timing.textContent = ' (' + ms + ' ms)';
    updateOutputCount(inputLen, filteredLen, prettyPrint ? result.length : -1);
  }
}

/* ── Helpers ──────────────────────────────────────────────── */
function val(id)        { return document.getElementById(id).value; }
function intVal(id)     { var v = parseInt(val(id), 10); return isNaN(v) ? -1 : v; }
function boolVal(id)    { return document.getElementById(id).checked; }
function setVal(id, v)  { document.getElementById(id).value   = v; }
function setChk(id, v)  { document.getElementById(id).checked = v; }

/* ── Char counts ──────────────────────────────────────────── */
function updateInputCount() {
  var n = document.getElementById('inputJson').value.length;
  document.getElementById('inputCharCount').textContent = n.toLocaleString();
}

function validateInputJson() {
  var text = document.getElementById('inputJson').value.trim();
  var el   = document.getElementById('inputJsonError');
  if (!text) { el.textContent = ''; return; }
  try {
    JSON.parse(text);
    el.textContent = '';
  } catch(e) {
    el.textContent = '⚠ ' + e.message;
  }
}

function updateOutputCount(inputLen, filteredLen, prettyLen) {
  var el = document.getElementById('outputCharCount');
  el.className = 'char-count';
  if (filteredLen < 0) { el.textContent = '—'; return; }

  /* Base: filtered size vs input size */
  var text = filteredLen.toLocaleString();
  if (inputLen > 0 && filteredLen !== inputLen) {
    var diff = inputLen - filteredLen;
    var pct  = 100 - Math.round(Math.abs(diff) / inputLen * 100);
    if (diff > 0) {
      text += ' (' + pct + '%)';
      el.classList.add('reduced');
    } else {
      text += '  (+' + Math.abs(diff).toLocaleString() + ')';
      el.classList.add('grown');
    }
  }

  /* Append pretty-printed size when it differs */
  if (prettyLen >= 0) {
    text += '  →  ' + prettyLen.toLocaleString() + ' pretty';
  }

  el.textContent = text;
}

/* ── Persistence (localStorage) ──────────────────────────── */
var STORAGE_KEY  = 'jlf_v1';
var PERSIST_TEXT = [
  'inputJson',
  'anonymizeKeys', 'anonymizePaths', 'anonymizeMessage',
  'pruneKeys', 'prunePaths', 'pruneMessage',
  'maxStringLength', 'maxSize', 'maxPathMatches', 'truncateMessage'
];
var PERSIST_BOOL = ['removeWhitespace', 'prettyPrint', 'syntaxHighlight', 'liveFilter', 'darkMode'];

function saveSettings() {
  try {
    var s = {};
    PERSIST_TEXT.forEach(function(id) { s[id] = document.getElementById(id).value; });
    PERSIST_BOOL.forEach(function(id) { s[id] = document.getElementById(id).checked; });
    localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
  } catch(e) {}
}

function loadSettings() {
  try {
    var raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return false;
    var s = JSON.parse(raw);
    PERSIST_TEXT.forEach(function(id) { if (s[id] !== undefined) document.getElementById(id).value   = s[id]; });
    PERSIST_BOOL.forEach(function(id) { if (s[id] !== undefined) document.getElementById(id).checked = s[id]; });
    return true;
  } catch(e) { return false; }
}

/* ── Syntax highlighting ──────────────────────────────────── */
function highlightJSON(text) {
  var s = text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  return s.replace(
    /("(?:\\.|[^"\\])*")(\s*:)?|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|\b(true|false|null)\b|([{}\[\],:])/g,
    function(m, str, colon, num, kw, punct) {
      if (str !== undefined)
        return colon
          ? '<span class="j-key">' + str + '</span>' + colon
          : '<span class="j-str">' + str + '</span>';
      if (num   !== undefined) return '<span class="j-num">'   + num   + '</span>';
      if (kw    !== undefined) return '<span class="j-kw">'    + kw    + '</span>';
      if (punct !== undefined) return '<span class="j-punct">' + punct + '</span>';
      return m;
    }
  );
}

function updateInputHighlight() {
  var ta  = document.getElementById('inputJson');
  var pre = document.getElementById('inputHighlight');
  var st  = ta.scrollTop, sl = ta.scrollLeft;
  pre.innerHTML = ta.value ? highlightJSON(ta.value) : '';
  pre.scrollTop  = st;
  pre.scrollLeft = sl;
}

function updateOutputHighlight(text, isError) {
  var ta  = document.getElementById('outputJson');
  var pre = document.getElementById('outputHighlight');
  var st  = ta.scrollTop, sl = ta.scrollLeft;
  pre.classList.toggle('error', isError);
  if (isError || !text) {
    pre.textContent = text || '';
  } else {
    pre.innerHTML = highlightJSON(text);
  }
  pre.scrollTop  = st;
  pre.scrollLeft = sl;
}

/* ── Filter implementation name ──────────────────────────── */
function updateFilterImpl() {
  var el = document.getElementById('filterImpl');
  if (!el) return;
  try {
    var name = JsonFilterApp.getFilterClass(
      val('anonymizeKeys'), val('pruneKeys'),
      val('anonymizePaths'), val('prunePaths'),
      intVal('maxStringLength'), intVal('maxSize'),
      boolVal('removeWhitespace'),
      val('anonymizeMessage'), val('pruneMessage'),
      val('truncateMessage'), intVal('maxPathMatches')
    );
    el.textContent = name || '—';
  } catch(e) { el.textContent = '—'; }
}

/* ── Apply filter (dispatches to worker) ─────────────────── */
function runFilter() {
  var out    = document.getElementById('outputJson');
  var status = document.getElementById('status');

  if (!val('inputJson').trim()) {
    out.value          = '';
    out.className      = 'code-input';
    status.textContent = '';
    document.getElementById('filterTiming').textContent = '';
    document.getElementById('outputSizer').textContent  = '\n';
    updateOutputHighlight('', false);
    updateOutputCount(-1, -1, -1);
    return;
  }

  status.textContent = '';
  out.className      = 'code-input';
  updateInputCount();

  var job = _collectArgs();
  if (_workerBusy) {
    _pendingJob = job; /* replace any previously queued job */
  } else {
    _dispatchJob(job);
  }
}

/* ── Dark mode ────────────────────────────────────────────── */
function applyTheme(dark) {
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
}

/* ── Syntax highlight toggle ──────────────────────────────── */
function setupHighlightToggle() {
  var chk   = document.getElementById('syntaxHighlight');
  var panel = document.getElementById('ioPanel');
  function apply() {
    panel.classList.toggle('no-highlight', !chk.checked);
    if (chk.checked) {
      updateInputHighlight();
      var outTa = document.getElementById('outputJson');
      updateOutputHighlight(outTa.value, outTa.classList.contains('error'));
    }
  }
  chk.addEventListener('change', apply);
  apply();
}

/* ── Live filtering & field listeners ────────────────────── */
function setupLiveFilter() {
  var liveCheckbox = document.getElementById('liveFilter');
  var applyBtn     = document.getElementById('applyBtn');

  /* Keep truncateMessage single-line: suppress Enter, strip newlines on paste */
  var truncTa = document.getElementById('truncateMessage');
  truncTa.addEventListener('keydown', function(e) {
    if (e.key === 'Enter') e.preventDefault();
  });
  truncTa.addEventListener('input', function() {
    var v = this.value;
    if (v.indexOf('\n') !== -1) {
      var pos = this.selectionStart;
      this.value = v.replace(/\n/g, '');
      this.selectionStart = this.selectionEnd = pos - 1;
    }
  });

  function syncButton() {
    applyBtn.style.display = liveCheckbox.checked ? 'none' : 'block';
  }
  liveCheckbox.addEventListener('change', function() {
    syncButton();
    if (liveCheckbox.checked) runFilter();
  });

  /* Settings fields — always update impl name; run filter only when live */
  var filterFields = [
    'anonymizeKeys', 'pruneKeys', 'anonymizePaths', 'prunePaths',
    'maxStringLength', 'maxSize', 'removeWhitespace', 'prettyPrint',
    'anonymizeMessage', 'pruneMessage', 'truncateMessage', 'maxPathMatches'
  ];
  filterFields.forEach(function(id) {
    var el = document.getElementById(id);
    if (!el) return;
    el.addEventListener('input',  function() { updateFilterImpl(); if (liveCheckbox.checked) runFilter(); });
    el.addEventListener('change', function() { updateFilterImpl(); if (liveCheckbox.checked) runFilter(); });
  });

  /* Input JSON — refresh highlight/count always; filter when live */
  var inputTa  = document.getElementById('inputJson');
  var inputPre = document.getElementById('inputHighlight');
  var cursorEl = document.getElementById('inputCursorPos');

  function updateCursorPos() {
    var pos   = inputTa.selectionStart;
    var text  = inputTa.value.substring(0, pos);
    var line  = text.split('\n').length;
    var col   = pos - text.lastIndexOf('\n');
    cursorEl.textContent = line + ':' + col;
  }

  function updateCursorBadgePos() {
    var wrapperRect = inputTa.parentElement.getBoundingClientRect();
    var pad = parseFloat(getComputedStyle(document.documentElement).fontSize) * 0.35;
    var top = Math.max(pad, -wrapperRect.top + pad);
    top = Math.min(top, wrapperRect.height - cursorEl.offsetHeight - pad);
    cursorEl.style.top = top + 'px';
    // Show badge instantly while scrolling (skip CSS fade)
    var inView = wrapperRect.bottom > 0 && wrapperRect.top < window.innerHeight;
    cursorEl.classList.toggle('visible', inView);
  }
  window.addEventListener('scroll', updateCursorBadgePos, { passive: true });

  inputTa.addEventListener('input', function() {
    document.getElementById('filterTiming').textContent = '';
    updateInputCount();
    updateInputHighlight();
    validateInputJson();
    updateCursorPos();
    document.getElementById('inputSizer').textContent = inputTa.value + '\n';
    if (liveCheckbox.checked) runFilter();
  });
  inputTa.addEventListener('keyup',    updateCursorPos);
  inputTa.addEventListener('click',    updateCursorPos);
  inputTa.addEventListener('focus',    updateCursorPos);
  inputTa.addEventListener('select',   updateCursorPos);
  inputTa.addEventListener('scroll', function() {
    inputPre.scrollTop  = inputTa.scrollTop;
    inputPre.scrollLeft = inputTa.scrollLeft;
  });

  var outputTa  = document.getElementById('outputJson');
  var outputPre = document.getElementById('outputHighlight');
  outputTa.addEventListener('scroll', function() {
    outputPre.scrollTop  = outputTa.scrollTop;
    outputPre.scrollLeft = outputTa.scrollLeft;
  });

  syncButton();
  updateInputHighlight();
  document.getElementById('inputSizer').textContent = inputTa.value + '\n';
  validateInputJson();
  runFilter();
  updateFilterImpl();
}

/* ── Per-section examples ─────────────────────────────────── */
var sectionExamples = {
  anonymize: {
    input: JSON.stringify({
      user: { name: "Alice Smith", email: "alice@example.com",
              password: "s3cr3t!", ssn: "123-45-6789" },
      session: { token: "Bearer x.y.z" }
    }, null, 2),
    anonymizeKeys: 'password\nssn',
    anonymizePaths: '$.user.email\n$.session.token',
    anonymizeMessage: '***',
    pruneKeys: '', prunePaths: '', pruneMessage: '',
    maxStringLength: '', maxSize: '', maxPathMatches: '',
    removeWhitespace: false, truncateMessage: ''
  },
  prune: {
    input: JSON.stringify({
      user: { name: "Bob", role: "admin" },
      debug: { stackTrace: "NullPointerException at line 42",
               heapDump: "ff d8 ff e0 00 10 ...", internalFlags: 0x1f },
      meta: { requestId: "req-999", source: "internal-api" }
    }, null, 2),
    pruneKeys: 'debug',
    prunePaths: '$.meta.source',
    pruneMessage: '[redacted]',
    anonymizeKeys: '', anonymizePaths: '', anonymizeMessage: '',
    maxStringLength: '', maxSize: '', maxPathMatches: '',
    removeWhitespace: false, truncateMessage: ''
  },
  limits: {
    input: JSON.stringify({
      title: "Short title",
      body: "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.",
      notes: "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.",
      tags: ["alpha", "beta", "gamma", "delta", "epsilon"]
    }, null, 2),
    maxStringLength: '60',
    truncateMessage: '… + ',
    maxSize: '', maxPathMatches: '',
    anonymizeKeys: '', anonymizePaths: '', anonymizeMessage: '',
    pruneKeys: '', prunePaths: '', pruneMessage: '',
    removeWhitespace: false
  },
  options: {
    input: JSON.stringify({
      type: "event", timestamp: "2025-01-01T12:00:00Z",
      payload: { action: "login", userId: 42, success: true, roles: ["user", "admin"] }
    }, null, 2),
    removeWhitespace: true,
    anonymizeKeys: '', anonymizePaths: '', anonymizeMessage: '',
    pruneKeys: '', prunePaths: '', pruneMessage: '',
    maxStringLength: '', maxSize: '', maxPathMatches: '', truncateMessage: ''
  }
};

function loadSectionExample(section) {
  var ex = sectionExamples[section];
  if (!ex) return;
  setVal('inputJson',        ex.input);
  setVal('anonymizeKeys',    ex.anonymizeKeys);
  setVal('anonymizePaths',   ex.anonymizePaths);
  setVal('anonymizeMessage', ex.anonymizeMessage);
  setVal('pruneKeys',        ex.pruneKeys);
  setVal('prunePaths',       ex.prunePaths);
  setVal('pruneMessage',     ex.pruneMessage);
  setVal('maxStringLength',  ex.maxStringLength);
  setVal('maxSize',          ex.maxSize);
  setVal('maxPathMatches',   ex.maxPathMatches);
  setVal('truncateMessage',  ex.truncateMessage);
  setChk('removeWhitespace', ex.removeWhitespace);
  document.getElementById('inputSizer').textContent = ex.input + '\n';
  updateInputCount();
  updateInputHighlight();
  validateInputJson();
  updateFilterImpl();
  if (document.getElementById('liveFilter').checked) {
    runFilter();
  } else {
    document.getElementById('outputJson').value         = '';
    document.getElementById('status').textContent       = '';
    document.getElementById('filterTiming').textContent = '';
    document.getElementById('outputJson').className     = 'code-input';
    updateOutputCount(-1, -1, -1);
  }
  saveSettings();
}

/* ── Ctrl+Enter shortcut ──────────────────────────────────── */
document.addEventListener('keydown', function(e) {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) runFilter();
});

/* ── Init ─────────────────────────────────────────────────── */
(function() {
  if (typeof _teaVMGetFilterClass === 'function' && typeof _teaVMApplyFilter === 'function') {
    document.getElementById('loading').classList.add('hidden');
    loadSettings();
    applyTheme(document.getElementById('darkMode').checked);
    document.getElementById('darkMode').addEventListener('change', function() {
      applyTheme(this.checked);
    });
    setupHighlightToggle();
    setupLiveFilter();
    document.addEventListener('input',  saveSettings);
    document.addEventListener('change', saveSettings);
  } else {
    document.getElementById('loading').innerHTML =
      '<p style="color:#ef4444">⚠ Filter engine failed to load.<br>Check the browser console for details.</p>';
  }
})();
