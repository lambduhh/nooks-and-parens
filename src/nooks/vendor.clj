(ns nooks.vendor
  "Third-party browser files the site serves itself, fetched at build time.

  Scittle isn't committed: a workstation commit hook reads constants in its
  minified code as personal data (false positives), and Jordan chose fetching
  over an exemption file in this public repo (2026-11-24). Visitors still load
  it from the site, never from a CDN (CLAUDE.md § Privacy).

  The build downloads the pinned npm tarball once into .work/vendor/ (git-
  ignored), checks the tarball against the registry's sha512 and the file
  against its SHA-256, then copies the file into public/. A mismatch fails the
  build. Upgrading: see assets/vendor/README.md."
  (:require [babashka.fs :as fs]
            [babashka.http-client :as http]
            [babashka.process :as p]
            [clojure.java.io :as io])
  (:import [java.security MessageDigest]
           [java.util Base64]))

(def scittle
  {:version   "0.8.33"
   :tarball   "https://registry.npmjs.org/scittle/-/scittle-0.8.33.tgz"
   ;; The registry's versions."0.8.33".dist.integrity
   :integrity "sha512-XeED9aRtVi0uHStoYn8hBO/dYEMfY7W4BIZkuj1J6MSxeGslfHVlar/hhCkYvA7y1X3ISRg/XqmFoz6pEYUUSQ=="
   ;; package/dist/scittle.js inside that tarball
   :sha256    "e04a33ff90947057772f42e683314e9c9f8671ebb922398208b4ad1c76991373"})

(def cache-dir (fs/path ".work" "vendor"))

(defn- digest [algo ^bytes bs]
  (.digest (MessageDigest/getInstance algo) bs))

(defn- hex [^bytes bs] (apply str (map #(format "%02x" (bit-and % 0xff)) bs)))

(defn- file-sha256 [path] (hex (digest "SHA-256" (fs/read-all-bytes path))))

(defn- cached [{:keys [version]}]
  {:js      (fs/path cache-dir (str "scittle-" version ".js"))
   :license (fs/path cache-dir (str "scittle-" version "-LICENSE.txt"))})

(defn- fetch! [{:keys [tarball integrity sha256] :as pkg}]
  (let [{:keys [js license]} (cached pkg)
        tmp (fs/create-temp-dir {:prefix "nooks-scittle-"})]
    (try
      (println "Fetching" tarball)
      (let [bs (:body (http/get tarball {:as :bytes}))
            got (str "sha512-" (.encodeToString (Base64/getEncoder) (digest "SHA-512" bs)))]
        (when (not= got integrity)
          (throw (ex-info (str "Scittle tarball integrity mismatch: got " got) {:want integrity})))
        (let [tgz (fs/path tmp "scittle.tgz")]
          (with-open [o (io/output-stream (fs/file tgz))] (.write o ^bytes bs))
          ;; tar is on macOS and the CI image; the archive is already verified.
          (p/shell {:dir (str tmp) :out :string} "tar" "-xzf" "scittle.tgz"
                   "package/dist/scittle.js" "package/LICENSE")))
      (let [extracted (fs/path tmp "package" "dist" "scittle.js")]
        (when (not= sha256 (file-sha256 extracted))
          (throw (ex-info "scittle.js SHA-256 mismatch" {:want sha256 :got (file-sha256 extracted)})))
        (fs/create-dirs cache-dir)
        (fs/copy extracted js {:replace-existing true})
        (fs/copy (fs/path tmp "package" "LICENSE") license {:replace-existing true}))
      (finally (fs/delete-tree tmp)))))

(defn install!
  "Put scittle.js and its licence into <out-dir>/assets/vendor/."
  [out-dir]
  (let [{:keys [js license]} (cached scittle)]
    (when-not (and (fs/exists? js) (fs/exists? license) (= (:sha256 scittle) (file-sha256 js)))
      (fetch! scittle))
    (let [dir (fs/path out-dir "assets" "vendor")]
      (fs/create-dirs dir)
      (fs/copy js (fs/path dir "scittle.js") {:replace-existing true})
      (fs/copy license (fs/path dir "scittle-LICENSE.txt") {:replace-existing true}))))
