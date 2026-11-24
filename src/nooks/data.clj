(ns nooks.data
  "Loads data/*.edn. Every page is built from what this returns."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]))

(def data-dir "data")

(defn- read-edn [path]
  (edn/read-string (slurp (str (fs/path data-dir path)))))

(defn load-all []
  {:site    (read-edn "site.edn")
   :days    (vec (sort-by :day/n (read-edn "days.edn")))
   :videos  (read-edn "videos.edn")
   :recipes (read-edn "recipes.edn")
   :ring    (read-edn "ring.edn")
   :formats   (read-edn "formats.edn")
   :resources (read-edn "resources.edn")
   :i18n    {:en    (read-edn "i18n/en.edn")
             :pt-BR (read-edn "i18n/pt-BR.edn")}})
