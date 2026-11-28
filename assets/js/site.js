// Toolbelt shortcuts: number keys follow the toolbelt links (0 = first slot).
// Never fires in a text field or with a modifier key; the switch turns it off.
// The on/off choice is kept in localStorage on this device only (no cookie,
// sent nowhere). If storage is blocked, the switch still works for the page.
(function () {
  var KEY = 'np-shortcuts';
  var btn = document.getElementById('np-keys');
  var links = document.querySelectorAll('.hotbar a[data-k]');
  if (!btn || !links.length) return;

  function load() { try { return localStorage.getItem(KEY) !== 'off'; } catch (e) { return true; } }
  function save(v) { try { localStorage.setItem(KEY, v ? 'on' : 'off'); } catch (e) {} }

  var on = load();
  function paint() {
    btn.setAttribute('aria-pressed', String(on));
    btn.textContent = on ? btn.dataset.on : btn.dataset.off;
  }
  btn.hidden = false;
  paint();
  btn.addEventListener('click', function () { on = !on; save(on); paint(); });

  document.addEventListener('keydown', function (e) {
    if (!on || e.ctrlKey || e.metaKey || e.altKey || e.shiftKey || e.isComposing) return;
    var t = e.target, tag = t && t.tagName;
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || (t && t.isContentEditable)) return;
    // Each slot's key is its data-k (0-based since 2026-11-24).
    for (var i = 0; i < links.length; i++) {
      if (links[i].getAttribute('data-k') === e.key) { location.href = links[i].href; return; }
    }
  });
})();

// Visit and click counts, sent to GoatCounter (cookieless; docs/analytics.md).
// Off unless the page carries <meta name="np-count">, which the build adds
// only when :analytics/goatcounter is set in data/site.edn.
//
// Sends: the page path and title, the referrer, and for a click the name in
// its data-count (with the page it was on as the title). Never sends what's
// typed in the Workbench, screen size, or anything stored on the device.
// Nothing is sent from a local build, a prerender, or when the visitor's
// browser asks not to be tracked (Do Not Track or Global Privacy Control).
//
// Other scripts count with window.npCount('name'); it's a no-op when off.
(function () {
  window.npCount = function () {};
  var meta = document.querySelector('meta[name="np-count"]');
  var endpoint = meta && meta.content;
  if (!endpoint) return;
  if (navigator.doNotTrack === '1' || navigator.globalPrivacyControl === true) return;
  if (/^(localhost|127\.|\[::1\])/.test(location.hostname)) return;

  function send(params) {
    var q = [];
    for (var k in params) q.push(k + '=' + encodeURIComponent(params[k]));
    // b=153: GoatCounter's flag for a WebDriver-driven browser, so it's
    // filed as a bot instead of a visit.
    if (navigator.webdriver) q.push('b=153');
    q.push('rnd=' + Math.random().toString(36).slice(2));
    var url = endpoint + '?' + q.join('&');
    // sendBeacon survives the page unloading, which a click on an outbound
    // link does. The image is the fallback GoatCounter documents.
    if (navigator.sendBeacon && navigator.sendBeacon(url)) return;
    new Image().src = url;
  }

  function pageview() {
    send({ p: location.pathname, t: document.title, r: document.referrer });
  }

  window.npCount = function (name) {
    send({ p: name, t: location.pathname, e: 'true' });
  };

  if (document.visibilityState === 'prerender') {
    document.addEventListener('visibilitychange', function once() {
      if (document.visibilityState !== 'visible') return;
      document.removeEventListener('visibilitychange', once);
      pageview();
    });
  } else {
    pageview();
  }

  // One listener for every element with data-count. 'click' also fires for
  // keyboard activation of links and buttons.
  document.addEventListener('click', function (e) {
    var el = e.target && e.target.closest && e.target.closest('[data-count]');
    if (el) window.npCount(el.getAttribute('data-count'));
  });
})();
