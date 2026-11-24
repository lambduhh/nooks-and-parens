// Toolbelt shortcuts: number keys follow the toolbelt links (1 = first slot).
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
    var i = '123456789'.slice(0, links.length).indexOf(e.key);
    if (i >= 0 && e.key.length === 1 && links[i]) { location.href = links[i].href; }
  });
})();
