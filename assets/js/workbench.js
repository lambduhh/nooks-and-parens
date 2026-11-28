// Workbench page script. The evaluator, Scittle, runs in a Web Worker
// (assets/js/workbench-worker.js), never on the page's main thread.
//
// The failsafe: Clojure runs synchronously, so `(loop [] (recur))` on the main
// thread would freeze the tab. In a worker it only blocks the worker. Each run
// gets a time limit (data-time-limit on #wb, in seconds) and a Stop button;
// either one terminates the worker, says what happened, and starts a fresh
// worker. Definitions made before that are gone, and the message says so.
//
// States: starting → ready ⇄ running; any → offline if the worker can't start.
// Offline: an unedited starter recipe still shows its :expect from
// data/recipes.edn; anything else explains that the evaluator didn't load.
//
// Privacy: nothing here makes a network request, stores anything, or reads
// cookies. Definitions live only until reload (no saved sessions).
//
// Exercises: each starter button carries data-exercise (kind, task, goal,
// hint, why, links; never the solution). The goal goes to the worker, which
// compares by value. Feedback is a verdict in words, the hint while the
// result doesn't match, and the why and links once it does. Predict
// exercises have no goal: any run shows the why.
//
// Every user-visible string comes from data-strings on #wb (data/i18n/*.edn).
(function () {
  'use strict';

  function norm(s) { return s.replace(/\s+/g, ' ').trim(); }

  function fmt(template) {
    var args = Array.prototype.slice.call(arguments, 1), i = 0;
    return template.replace(/%s/g, function () { return String(args[i++]); });
  }

  function start() {
    var bench = document.getElementById('wb');
    if (!bench) return;
    var S = JSON.parse(bench.getAttribute('data-strings'));
    var workerUrl = bench.getAttribute('data-worker');
    var limit = Number(bench.getAttribute('data-time-limit')) || 10;
    var $ = function (id) { return document.getElementById(id); };
    var code = $('wb-code'), status = $('wb-status'), kind = $('wb-kind'),
        value = $('wb-value'), printedBox = $('wb-printed'), printed = $('wb-printed-text'),
        errhelp = $('wb-errhelp'), out = $('wb-out'), craftBtn = $('wb-craft'), stopBtn = $('wb-stop');
    var starters = document.querySelectorAll('[data-recipe]');
    var feedbackBox = $('wb-feedback'), verdict = $('wb-verdict'), hint = $('wb-hint'),
        whyBox = $('wb-why-box'), why = $('wb-why'), links = $('wb-links');
    var current = null; // the loaded exercise, from its button's data-exercise

    function exerciseOf(button) {
      try { return JSON.parse(button.getAttribute('data-exercise')); } catch (e) { return null; }
    }

    function showTask(ex) {
      $('wb-task').hidden = !ex;
      if (!ex) return;
      $('wb-ex-kind').textContent = ex.kindLabel;
      $('wb-ex-title').textContent = ex.title;
      $('wb-ex-task').textContent = ex.task;
      $('wb-ex-goal-box').hidden = !ex.goal;
      $('wb-ex-goal').textContent = ex.goal || '';
    }

    function hideFeedback() { feedbackBox.hidden = true; }

    function showWhy(ex) {
      why.textContent = ex.why;
      links.textContent = '';
      (ex.links || []).forEach(function (l, i) {
        if (i) links.appendChild(document.createTextNode(' · '));
        var a = document.createElement('a');
        a.href = l.url; a.textContent = l.title;
        links.appendChild(a);
      });
      whyBox.hidden = false;
    }

    // matched: true, false, or null (no goal to compare).
    function feedback(matched) {
      if (!current) { hideFeedback(); return; }
      feedbackBox.hidden = false;
      if (current.kind === 'predict') {
        verdict.hidden = true; hint.hidden = true; showWhy(current);
      } else if (matched) {
        verdict.hidden = false; verdict.textContent = S.match;
        hint.hidden = true; showWhy(current);
      } else {
        verdict.hidden = false; verdict.textContent = S.noMatch;
        hint.textContent = current.hint || ''; hint.hidden = !current.hint;
        whyBox.hidden = true;
      }
    }

    var worker = null, state = 'starting', runId = 0, timer = null, queued = null;

    function render(k, text, opts) {
      opts = opts || {};
      out.classList.toggle('err', !!opts.error);
      kind.textContent = k;
      value.textContent = text;
      printed.textContent = opts.printed || '';
      printedBox.hidden = !opts.printed;
      errhelp.hidden = !opts.help;
      if (!opts.keepFeedback) hideFeedback();
    }

    function waiting(msg) { render(S.waitingKind, msg || S.waiting); }

    function setState(s) {
      state = s;
      status.textContent = s === 'offline' ? S.statusOffline
                         : s === 'starting' ? S.statusStarting
                         : S.statusReady;
      stopBtn.hidden = s !== 'running';
      craftBtn.setAttribute('aria-disabled', String(s === 'running'));
    }

    function spawn() {
      setState('starting');
      try {
        worker = new Worker(workerUrl);
      } catch (e) {
        goOffline(e);
        return;
      }
      worker.onmessage = function (m) {
        var d = m.data;
        if (d.type === 'ready') {
          setState('ready');
          if (queued !== null) { var q = queued; queued = null; run(q); }
        } else if (d.type === 'failed') {
          goOffline(d.message);
        } else if (d.id === runId && state === 'running') {
          clearTimeout(timer);
          setState('ready');
          if (d.ok) {
            render(d.kind, d.value, { printed: d.printed });
            feedback(d.match);
          } else {
            var where = d.line ? fmt(S.errorAt, d.line, d.column) + '\n' : '';
            render(S.errorKind, where + d.message, { error: true, help: true, printed: d.printed });
            feedback(false);
          }
        }
      };
      worker.onerror = function (e) {
        if (state === 'starting') { e.preventDefault(); goOffline(e.message); }
      };
    }

    function goOffline(why) {
      if (worker) worker.terminate();
      worker = null;
      setState('offline');
      // Say why, for whoever debugs it.
      if (window.console) console.warn('Workbench: evaluator unavailable, offline preview.', why);
      if (queued !== null) { var q = queued; queued = null; offline(q); }
    }

    // The failsafe: stop the worker, explain, start a fresh one.
    function stop(reason) {
      clearTimeout(timer);
      if (worker) worker.terminate();
      worker = null;
      render(S.stoppedKind, reason, { error: true });
      spawn();
    }

    function run(src) {
      runId += 1;
      setState('running');
      render(S.busyKind, S.busy);
      timer = setTimeout(function () { stop(fmt(S.stoppedTimeout, limit)); }, limit * 1000);
      worker.postMessage({ id: runId, code: src, goal: current && current.goal });
    }

    function craft() {
      var src = code.value;
      if (state === 'running') return;
      if (!src.trim()) { render(S.emptyKind, S.empty, { error: true }); return; }
      if (current && current.kind !== 'predict' && src.indexOf('___') !== -1) { waiting(S.blanks); return; }
      if (state === 'offline') { offline(src); return; }
      if (state === 'starting') { queued = src; render(S.busyKind, S.starting); return; }
      run(src);
    }

    // Without the evaluator, a starter recipe that hasn't been edited still
    // shows its result: the :expect from data/recipes.edn.
    function offline(src) {
      for (var i = 0; i < starters.length; i++) {
        var b = starters[i];
        if (b.getAttribute('data-expect') && norm(b.getAttribute('data-code')) === norm(src)) {
          render(b.getAttribute('data-kind'), b.getAttribute('data-expect'));
          if (current && current.kind === 'predict') feedback(null);
          return;
        }
      }
      render(S.offlineKind, S.offline, { error: true });
    }

    function load(key, announce) {
      if (state === 'running') return false;
      for (var i = 0; i < starters.length; i++) {
        if (starters[i].getAttribute('data-recipe') === key) {
          current = exerciseOf(starters[i]);
          showTask(current);
          code.value = starters[i].getAttribute('data-code');
          waiting(announce ? fmt(S.loaded, starters[i].getAttribute('data-title')) : null);
          return true;
        }
      }
      return false;
    }

    $('wb-controls').hidden = false;
    // The page opens with the first exercise in the editor.
    if (starters.length) current = exerciseOf(starters[0]);
    waiting();
    if (location.hash) load(decodeURIComponent(location.hash.slice(1)), false);
    spawn();

    craftBtn.addEventListener('click', craft);
    stopBtn.addEventListener('click', function () { if (state === 'running') stop(S.stoppedByYou); });
    $('wb-clear').addEventListener('click', function () {
      if (state === 'running') return;
      code.value = ''; waiting(); code.focus();
    });
    // Ctrl/Cmd+Enter crafts. Tab is left alone, so it still moves focus.
    code.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) { e.preventDefault(); craft(); }
    });
    for (var i = 0; i < starters.length; i++) {
      starters[i].addEventListener('click', function (e) {
        var key = e.currentTarget.getAttribute('data-recipe');
        if (load(key, true)) history.replaceState(null, '', '#' + key); // shareable, no hashchange
      });
    }
    window.addEventListener('hashchange', function () {
      load(decodeURIComponent(location.hash.slice(1)), true);
    });
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
  else start();
})();
