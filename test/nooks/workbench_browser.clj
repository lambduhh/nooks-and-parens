(ns nooks.workbench-browser
  "bb test:browser: the Workbench in a real browser.

  Builds the site, serves it on 127.0.0.1, and drives headless Chrome over the
  DevTools protocol (a WebSocket; no npm, no driver). Runs every starter recipe
  in data/recipes.edn through the real page, under the real CSP, and checks
  the printed result against :expect. That's the only place :expect can be
  checked: bb is JVM Clojure, and some results differ in ClojureScript
  (frequencies of a string gives [\\i 4] in bb, [\"i\" 4] in Scittle).

  Chrome: $CHROME, else the macOS app, else google-chrome / chromium on PATH.
  Not part of `bb test` (it needs a browser); run it after touching the
  Workbench or upgrading assets/vendor/scittle.js."
  (:require [babashka.fs :as fs]
            [babashka.http-client :as http]
            [babashka.http-client.websocket :as ws]
            [babashka.http-server :as server]
            [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.string :as str]
            [nooks.build :as build]
            [nooks.data :as data]
            [nooks.i18n :as i18n]
            [nooks.pages.workbench :as wb])
  (:import [java.time Instant]))

;; ---- Chrome over the DevTools protocol ---------------------------------------

(defn- find-chrome []
  (or (System/getenv "CHROME")
      (first (filter fs/exists? ["/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"]))
      (some #(some-> (fs/which %) str) ["google-chrome" "google-chrome-stable" "chromium" "chromium-browser"])
      (throw (ex-info "No Chrome found. Set CHROME to a Chrome or Chromium binary." {}))))

(defn- wait-for [f what]
  (loop [n 0]
    (or (try (f) (catch Exception _ nil))
        (if (< n 200)
          (do (Thread/sleep 100) (recur (inc n)))
          (throw (ex-info (str "Timed out waiting for " what) {}))))))

(defn- connect
  "A CDP session on a new tab: {:call (fn [method params]) :events atom}."
  [debug-port]
  (let [tab     (wait-for #(-> (http/put (str "http://127.0.0.1:" debug-port "/json/new?about:blank"))
                               :body (json/parse-string true))
                          "Chrome's debugging port")
        ids     (atom 0)
        pending (atom {})
        events  (atom [])
        ;; Chrome splits large CDP messages into WebSocket fragments; on-message
        ;; gets each with last? false until the final one. Parse only whole
        ;; messages: parsing a fragment threw, dropped the reply, and the call
        ;; timed out ("CDP timeout: Runtime.evaluate", intermittent).
        buf     (StringBuilder.)
        conn    (ws/websocket
                 {:uri (:webSocketDebuggerUrl tab)
                  :on-message (fn [_ data last?]
                                (.append buf (str data))
                                (when last?
                                 (let [text (str buf)
                                       _    (.setLength buf 0)
                                       m    (json/parse-string text true)]
                                  (if-let [pr (and (:id m) (get @pending (:id m)))]
                                    (deliver pr m)
                                    (swap! events conj m)))))})]
    {:events events
     :call (fn [method & [params]]
             (let [id (swap! ids inc), pr (promise)]
               (swap! pending assoc id pr)
               (ws/send! conn (json/generate-string {:id id :method method :params (or params {})}))
               (let [r (deref pr 20000 ::timeout)]
                 (when (= r ::timeout) (throw (ex-info (str "CDP timeout: " method) {})))
                 (when-let [e (:error r)] (throw (ex-info (str method ": " (:message e)) e)))
                 (:result r))))}))

(defn- js
  "Evaluate a JS expression in the page and return its value."
  [{:keys [call]} expr]
  (let [r (call "Runtime.evaluate" {:expression expr :returnByValue true :awaitPromise true})]
    (when-let [ex (:exceptionDetails r)]
      (throw (ex-info (str "JS exception: " (get-in ex [:exception :description] (:text ex))) {:expr expr})))
    (get-in r [:result :value])))

(defn- goto! [s url]
  ((:call s) "Page.navigate" {:url url})
  (Thread/sleep 200)
  (wait-for #(= "complete" (js s "document.readyState")) url))

;; ---- Checks -------------------------------------------------------------------

(def ^:private failures (atom []))

(defn- check [label ok? & [detail]]
  (println (if ok? "  ok  " "  FAIL") label (if (and (not ok?) detail) (str "\n        " detail) ""))
  (when-not ok? (swap! failures conj label)))

(defn- result [s]
  (js s "({kind: document.getElementById('wb-kind').textContent,
           value: document.getElementById('wb-value').textContent,
           printed: document.getElementById('wb-printed').hidden ? null : document.getElementById('wb-printed-text').textContent,
           errhelp: !document.getElementById('wb-errhelp').hidden})"))

(defn- one-line
  "A pretty-printed result joined back onto one line: the Workbench's printer
  only adds line breaks (and indentation after them) to pr-str's output."
  [s]
  (str/replace s #"\n *" " "))

(defn- feedback [s]
  (js s "({shown: !document.getElementById('wb-feedback').hidden,
           verdict: document.getElementById('wb-verdict').hidden ? null : document.getElementById('wb-verdict').textContent,
           hint: document.getElementById('wb-hint').hidden ? null : document.getElementById('wb-hint').textContent,
           whyHidden: document.getElementById('wb-why-box').hidden,
           why: document.getElementById('wb-why').textContent})"))

(def ^:private ^:dynamic *strs* nil)

(defn- settled
  "Wait until the evaluator isn't starting or running, then return the result."
  [s]
  (wait-for #(let [st (js s "document.getElementById('wb-status').textContent")
                   r  (result s)]
               (when (and (not= st (*strs* :workbench/status-starting))
                          (not= (:kind r) (*strs* :workbench/busy-kind))
                          (js s "document.getElementById('wb-stop').hidden"))
                 r))
            "the Workbench to settle"))

(defn- start-craft! [s code]
  (js s (str "(() => { const c = document.getElementById('wb-code'); c.value = " (json/generate-string code)
             "; document.getElementById('wb-craft').click(); })()")))

(defn- craft! [s code]
  (start-craft! s code)
  (settled s))

(defn- csp-problems
  "Console/log events that mean the CSP blocked something, or a page error."
  [events]
  (for [e events
        :let [text (or (get-in e [:params :entry :text])
                       (get-in e [:params :exceptionDetails :exception :description]))]
        :when (and text
                   (or (= "Runtime.exceptionThrown" (:method e))
                       (re-find #"(?i)content security policy|refused to" text)))]
    text))

(defn run [s url recipes strs]
  (let [{:keys [call events]} s]
    (call "Runtime.enable") (call "Log.enable") (call "Page.enable") (call "Network.enable")

    (println "Workbench with Scittle:" url)
    (goto! s url)
    (settled s)
    (check "status says the evaluator is ready"
           (= (strs :workbench/status-ready) (js s "document.getElementById('wb-status').textContent")))
    (check "controls are shown" (false? (js s "document.getElementById('wb-controls').hidden")))
    (doseq [{k :id :keys [kind title code expect solution why hint]} recipes]
      (js s (str "document.getElementById('" (name k) "').click()"))
      (check (str "exercise " k ": button loads its code")
             (= code (js s "document.getElementById('wb-code').value")))
      (check (str "exercise " k ": the task panel shows it")
             (= title (js s "document.getElementById('wb-ex-title').textContent")))
      (if expect
        (let [res (craft! s code) fb (feedback s)]
          (check (str "exercise " k ": result matches :expect") (= expect (one-line (:value res))) (str "got " (:value res)))
          (check (str "exercise " k ": kind label matches the offline label")
                 (= (wb/value-kind expect) (:kind res)) (str "got " (:kind res)))
          (if (= :predict kind)
            (check (str "exercise " k ": running shows the why")
                   (and (:shown fb) (not (:whyHidden fb)) (= why (:why fb))) (pr-str fb))
            (check (str "exercise " k ": the starter isn't the goal yet; the hint shows")
                   (and (= (strs :workbench/no-match) (:verdict fb)) (= hint (:hint fb)) (:whyHidden fb)) (pr-str fb))))
        (let [res (craft! s code)]
          (check (str "exercise " k ": blanks are caught before running")
                 (= (strs :workbench/blanks) (:value res)) (pr-str res))))
      (when solution
        (let [res (craft! s (apply str/replace-first code solution)) fb (feedback s)]
          (check (str "exercise " k ": the solution matches the goal and shows the why")
                 (and (= (strs :workbench/match) (:verdict fb)) (not (:whyHidden fb)) (= why (:why fb)))
                 (str (pr-str fb) " result " (:value res))))))
    (let [r (craft! s "(println \"hello\" 1)\n(prn {:a 1})\n:done")]
      (check "println/prn output is shown, value is the last form"
             (and (= ":done" (:value r)) (= "hello 1\n{:a 1}" (:printed r))) (pr-str r)))
    (let [r (craft! s "(def x 41)")
          r2 (craft! s "(inc x)")]
      (check "definitions last until reload" (= "42" (:value r2)) (pr-str r2)))
    (let [r (craft! s "(range)")]
      (check "an infinite seq prints truncated instead of hanging"
             (and (= "seq" (:kind r)) (str/ends-with? (:value r) "...)")) (subs (:value r) 0 (min 40 (count (:value r))))))
    (let [r (craft! s "[{:name \"alice\" :count 3} {:name \"bob\" :count 5} {:name \"cara\" :count 2}]")]
      (check "a result too long for one line prints one item per line"
             (= "[{:name \"alice\", :count 3}\n {:name \"bob\", :count 5}\n {:name \"cara\", :count 2}]" (:value r))
             (pr-str (:value r))))
    (let [r (craft! s "{:elves [{:name \"Ada\" :snacks [1000 2000 3000]} {:name \"Grace\" :snacks [4000]}] :total 10000}")]
      (check "nested results indent under their parent, one map entry per line"
             (= "{:elves [{:name \"Ada\", :snacks [1000 2000 3000]}\n         {:name \"Grace\", :snacks [4000]}],\n :total 10000}" (:value r))
             (pr-str (:value r))))
    (let [r (craft! s "(range 30)")]
      (check "plain values pack onto lines"
             (= "(0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19\n 20 21 22 23 24 25 26 27 28 29)" (:value r))
             (pr-str (:value r))))
    (let [code "(into (sorted-map) (for [y (range 4) x (range 4)] [[x y] (if (= x y) \"#\" \".\")]))"
          r    (craft! s code)
          flat (:value (craft! s (str "(pr-str " code ")")))]
      (check "pretty output joined onto one line is exactly pr-str"
             (= (pr-str (one-line (:value r))) flat) (str (pr-str (:value r)) " vs " flat)))
    (let [r (craft! s "(+ 1\n   (nope 2))")]
      (check "an error shows its kind, position and message, and the how-to link"
             (and (= (strs :workbench/error-kind) (:kind r))
                  (str/includes? (:value r) (format (strs :workbench/error-at) 2 4))
                  (str/includes? (:value r) "nope")
                  (:errhelp r))
             (pr-str r)))
    (let [r (craft! s "(+ 1 2")]
      (check "a reader error shows too" (and (= (strs :workbench/error-kind) (:kind r)) (:errhelp r)) (pr-str r)))
    (let [r (craft! s "   ")]
      (check "empty input says so" (= (strs :workbench/empty) (:value r)) (pr-str r)))
    (let [r (craft! s "(+ 2 3)")]
      (check "the error help hides again after a good result" (not (:errhelp r)) (pr-str r)))
    (js s "(() => { const c = document.getElementById('wb-code'); c.value = '(* 6 7)';
             c.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter', ctrlKey: true, bubbles: true})); })()")
    (check "Ctrl+Enter crafts" (= "42" (:value (settled s))))
    (js s "(() => { const c = document.getElementById('wb-code'); c.value = '(* 2 5)';
             c.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter', metaKey: true, bubbles: true})); })()")
    (check "Cmd+Enter crafts" (= "10" (:value (settled s))))
    (check "the result slot is a polite live region"
           (= "polite" (js s "document.getElementById('wb-kind').parentElement.getAttribute('aria-live')")))

    (println "Failsafe (infinite loops):")
    (craft! s "(def kept 1)")
    (start-craft! s "(loop [] (recur))")
    (Thread/sleep 500)
    (let [t0 (System/currentTimeMillis)
          busy (result s)
          _ (js s "1 + 1")
          ms (- (System/currentTimeMillis) t0)]
      (check "while a loop runs, the page says so and shows Stop"
             (and (= (*strs* :workbench/busy-kind) (:kind busy))
                  (false? (js s "document.getElementById('wb-stop').hidden"))) (pr-str busy))
      (check "while a loop runs, the page still responds" (< ms 1000) (str ms " ms")))
    (let [t0 (System/currentTimeMillis)
          r  (settled s)
          secs (/ (- (System/currentTimeMillis) t0) 1000.0)]
      (check "the time limit stops it and says why"
             (and (= (*strs* :workbench/stopped-kind) (:kind r))
                  (= (format (*strs* :workbench/stopped-timeout) wb/time-limit) (:value r)))
             (str (pr-str r) " after " secs " s")))
    (check "the evaluator restarts" (= "3" (:value (craft! s "(+ 1 2)"))))
    (let [r (craft! s "kept")]
      (check "a restart clears earlier definitions, as the message says"
             (= (*strs* :workbench/error-kind) (:kind r)) (pr-str r)))
    (start-craft! s "(loop [] (recur))")
    (Thread/sleep 700)
    (js s "document.getElementById('wb-stop').click()")
    (let [r (settled s)]
      (check "Stop stops it at once" (= (*strs* :workbench/stopped-by-you) (:value r)) (pr-str r)))
    (check "and the evaluator works again" (= "6" (:value (craft! s "(* 2 3)"))))

    (let [r (last recipes) k (:id r)]
      (goto! s "about:blank")
      (goto! s (str url "#" (name k)))
      (check (str "/workbench/#" (name k) " opens that recipe")
             (= (:code r) (js s "document.getElementById('wb-code').value"))))

    (println "Toolbelt shortcut keys (0-based):")
    (goto! s url)
    (js s "(() => { const c = document.getElementById('wb-code'); c.focus();
             c.dispatchEvent(new KeyboardEvent('keydown', {key: '3', bubbles: true})); })()")
    (Thread/sleep 300)
    (check "a number typed in the code box doesn't switch tools"
           (str/ends-with? (js s "location.pathname") "/workbench/"))
    (js s "document.body.dispatchEvent(new KeyboardEvent('keydown', {key: '3', bubbles: true}))")
    (wait-for #(and (str/ends-with? (js s "location.pathname") "/learn/")
                    (= "complete" (js s "document.readyState"))) "key 3 → Learn")
    (check "key 3 opens Learn" true)
    ;; /today/ redirects at once (to the day's page, or home before 1 Dec),
    ;; so check slot 0's link, then that key 0 follows it.
    (check "slot 0 is Today"
           (str/ends-with? (js s "document.querySelector('.hotbar a[data-k=\"0\"]').pathname") "/today/"))
    (js s "document.body.dispatchEvent(new KeyboardEvent('keydown', {key: '0', bubbles: true}))")
    (wait-for #(not (str/ends-with? (js s "location.pathname") "/learn/")) "key 0 to leave Learn")
    (check "key 0 follows it" true)

    (check "no CSP violations or page errors" (empty? (csp-problems @events)) (str/join "\n        " (csp-problems @events)))

    ;; CDP's URL blocking doesn't reach a worker's script fetches (checked in
    ;; Chrome 158), so stand in for Worker with one that reports what the real
    ;; worker reports when importScripts('scittle.js') fails.
    (println "Workbench without an evaluator (offline preview):")
    (call "Page.addScriptToEvaluateOnNewDocument"
          {:source "window.Worker = function () { var w = this;
                      setTimeout(function () { w.onmessage({data: {type: 'failed', message: 'test: no scittle.js'}}); }, 0); };
                    window.Worker.prototype.postMessage = function () {};
                    window.Worker.prototype.terminate = function () {};"})
    (goto! s "about:blank")
    (goto! s url)
    (settled s)
    (check "status says offline preview"
           (= (strs :workbench/status-offline) (js s "document.getElementById('wb-status').textContent")))
    (let [r (first recipes) k (:id r)]
      (js s (str "document.getElementById('" (name k) "').click()"))
      (let [res (craft! s (js s "document.getElementById('wb-code').value"))]
        (check (str "an unedited starter (" k ") shows its :expect") (= (:expect r) (:value res)) (pr-str res))))
    (let [res (craft! s "(+ 1 2)")]
      (check "anything else explains the offline preview"
             (= (strs :workbench/offline) (:value res)) (pr-str res)))))

;; ---- Runner ---------------------------------------------------------------------

(defn- free-port []
  (with-open [s (java.net.ServerSocket. 0)] (.getLocalPort s)))

(defn -main [& _]
  (let [d       (data/load-all)
        base    (get-in d [:site :site/base-path])
        root    (fs/path ".work" "browser" "root")
        out     (str (fs/path root (str/replace base #"^/" "")))
        port    (free-port)
        dport   (free-port)
        profile (str (fs/create-temp-dir {:prefix "nooks-chrome-"}))
        strs    (fn [k] (binding [i18n/*strings* (:i18n d)] (i18n/t-str :en k)))]
    (fs/delete-tree root)
    (build/build! {:as-of (Instant/now) :out-dir out :work-dir ".work/browser/work" :quiet? true})
    (let [stop   (server/serve {:port port :dir (str root) :ip "127.0.0.1"})
          chrome (p/process [(find-chrome) "--headless=new" "--disable-gpu" "--no-first-run"
                             "--no-default-browser-check" (str "--user-data-dir=" profile)
                             (str "--remote-debugging-port=" dport) "about:blank"]
                            {:out :discard :err :discard})]
      (try
        (binding [*strs* strs]
          (run (connect dport) (str "http://127.0.0.1:" port base "/workbench/")
               (seq (:recipes d)) strs))
        (catch Exception e
          (check "browser run finished" false (ex-message e)))
        (finally
          (p/destroy-tree chrome)
          (when (fn? stop) (stop))
          ;; Chrome can still be writing its profile as it exits.
          (try (fs/delete-tree profile) (catch Exception _ nil)))))
    (println (if (empty? @failures)
               "bb test:browser: all checks pass"
               (str "bb test:browser: " (count @failures) " failure(s)")))
    (System/exit (if (empty? @failures) 0 1))))
