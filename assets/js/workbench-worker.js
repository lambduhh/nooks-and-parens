// Workbench evaluator: Scittle in a Web Worker, so the page stays responsive.
//
// Why a worker: Clojure code runs synchronously. On the page's main thread,
// `(loop [] (recur))` would freeze the tab, with no way to stop it. Here it
// only blocks this worker; workbench.js gives each run a time limit, then
// terminates the worker and starts a fresh one.
//
// Scittle 0.8.33 loads in a worker unchanged: it only touches `document`
// behind a check that it exists.
//
// SCI_DISABLE_JIT: SCI's optional JIT probes `new Function`, which the site's
// CSP (script-src 'self', no 'unsafe-eval') blocks. The probe fails safely,
// but the browser logs a violation; the flag skips the probe.
//
// Messages
//   out:  {type: 'ready'} | {type: 'failed', message}
//   in:   {id, code}
//   out:  {id, ok: true, kind, value, printed}
//         {id, ok: false, message, line, column, printed}
// Nothing here makes a network request beyond loading scittle.js, stores
// anything, or reads cookies.
self.SCI_DISABLE_JIT = true;

(function () {
  'use strict';

  // show: a value as Clojure data with a kind label, pretty-printed: a
  // result that fits in 50 columns stays on one line; a longer map puts one
  // entry per line, a longer vector, list or set one item per line (plain
  // values packed onto lines), nested and indented. Joining the lines with
  // single spaces gives exactly pr-str's output, so only line breaks change
  // (bb test:browser checks this). *print-length* and *print-level* stop
  // (range) or deep nesting from printing forever, and the printer itself
  // only takes the first 101 items of a collection. Not clojure.pprint:
  // Scittle's pprint plugin ignores *print-length*, so (range) would hang.
  // err: message and position from an SCI error. capture: println/prn to JS.
  var PRETTY =
    "(fn pretty [v]\n" +
    "      (letfn [(fill [strs width]\n" +
    "                ;; Pack short items onto lines up to width; one space between items.\n" +
    "                (reduce (fn [acc s]\n" +
    "                          (let [line (peek acc)]\n" +
    "                            (if (and line (<= (+ (count line) 1 (count s)) width))\n" +
    "                              (conj (pop acc) (str line \" \" s))\n" +
    "                              (conj acc s))))\n" +
    "                        [] strs))\n" +
    "              (lines [x indent depth]\n" +
    "                (let [s (pr-str x)]\n" +
    "                  (if (or (not (coll? x)) (record? x) (> depth 12) (<= (+ indent (count s)) 50))\n" +
    "                    s\n" +
    "                    (let [[open close sep] (cond (map? x) [\"{\" \"}\" \",\"] (vector? x) [\"[\" \"]\" \"\"]\n" +
    "                                                 (set? x) [\"#{\" \"}\" \"\"] :else [\"(\" \")\" \"\"])\n" +
    "                          in    (+ indent (count open))\n" +
    "                          shown (take 100 x)\n" +
    "                          more  (when (seq (drop 100 x)) [\"...\"])\n" +
    "                          body  (cond\n" +
    "                                  (map? x)\n" +
    "                                  (for [item shown :let [k (pr-str (key item))]]\n" +
    "                                    (str k \" \" (lines (val item) (+ in (count k) 1) (inc depth))))\n" +
    "                                  (not-any? coll? shown)\n" +
    "                                  (fill (concat (map pr-str shown) more) (max 20 (- 50 in)))\n" +
    "                                  :else\n" +
    "                                  (concat (for [item shown] (lines item in (inc depth))) more))\n" +
    "                          body  (if (map? x) (concat body more) body)]\n" +
    "                      (str open (clojure.string/join (str sep \"\\n\" (apply str (repeat in \" \"))) body) close)))))]\n" +
    "        (lines v 0 0)))\n";
  var HELPERS =
    '(let [pretty ' + PRETTY + ']\n' +
    '(array\n' +
    ' (fn [v]\n' +
    '   (binding [*print-length* 100 *print-level* 12]\n' +
    '     (array (cond (nil? v) "nil" (map? v) "map" (vector? v) "vector" (set? v) "set"\n' +
    '                  (seq? v) "seq" (string? v) "string" (keyword? v) "keyword"\n' +
    '                  (symbol? v) "symbol" (number? v) "number" (boolean? v) "boolean"\n' +
    '                  (fn? v) "function" (instance? js/RegExp v) "regex" :else "value")\n' +
    '            (pretty v))))\n' +
    ' (fn [e] (let [d (ex-data e)] (array (or (ex-message e) (str e)) (:line d) (:column d))))\n' +
    ' (fn [f] (set! *print-fn* (fn [& xs] (apply f xs)))\n' +
    '         (set! *print-err-fn* (fn [& xs] (apply f xs))))))';

  var evalString, show, err, lines = [];
  try {
    // Relative to this file: /assets/js/ → /assets/vendor/.
    importScripts('../vendor/scittle.js');
    evalString = self.scittle.core.eval_string;
    var h = evalString(HELPERS);
    show = h[0]; err = h[1];
    h[2](function () { lines.push(Array.prototype.join.call(arguments, ' ')); });
    self.postMessage({ type: 'ready' });
  } catch (e) {
    self.postMessage({ type: 'failed', message: String(e && e.message || e) });
    return;
  }

  self.onmessage = function (m) {
    var id = m.data.id;
    lines = [];
    try {
      var r = show(evalString(m.data.code));
      self.postMessage({ id: id, ok: true, kind: r[0], value: r[1], printed: lines.join('\n') });
    } catch (e) {
      var info;
      try { info = err(e); } catch (_) { info = [String(e && e.message || e), null, null]; }
      self.postMessage({ id: id, ok: false, message: info[0], line: info[1], column: info[2],
                         printed: lines.join('\n') });
    }
  };
})();
