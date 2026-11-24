(ns nooks.template-test
  (:require [babashka.fs :as fs]
            [clojure.test :refer [deftest is testing]]
            [nooks.template :as tpl]))

(deftest github-slugs
  (testing "match the anchors the template's own links use"
    (is (= "2-install-or-not" (tpl/github-slug "2. Install, or not?")))
    (is (= "3-read-the-example-day" (tpl/github-slug "3. Read the example day")))
    (is (= "using-an-ai-agent" (tpl/github-slug "Using an AI agent?")))
    (is (= "3-install-java-the-clojure-cli-and-babashka" (tpl/github-slug "3. Install Java, the Clojure CLI and Babashka")))))

(deftest links
  (let [dir (fs/create-temp-dir)
        _   (fs/create-dirs (fs/path dir "src" "howto"))
        _   (fs/create-dirs (fs/path dir "ai"))
        _   (spit (str (fs/path dir "ai" "AGENTS.md")) "x")
        _   (fs/create-sym-link (fs/path dir "AGENTS.md") (fs/path "ai" "AGENTS.md"))
        rw  (tpl/link-rewriter {:site {:template/url "https://github.com/o/t"} :sha "abc" :dir dir
                                :hub-for {"README.md" "/learn/template/" "ai/AGENTS.md" "/learn/ai/agents/"}
                                :hub-href #(str "/base" %)}
                               "docs/tutorial.md")]
    (is (= "/base/learn/template/#aliases" (rw "../README.md#aliases")) "rendered file → hub page, anchor kept")
    (is (= "/base/learn/ai/agents/" (rw "../AGENTS.md")) "symlink followed")
    (is (= "https://github.com/o/t/tree/abc/src/howto" (rw "../src/howto/")) "directory → GitHub tree")
    (is (= "https://github.com/o/t/blob/abc/deps.edn" (rw "../deps.edn")) "other file → GitHub blob at the pin")
    (is (= "#3-step" (rw "#3-step")))
    (is (= "https://calva.io" (rw "https://calva.io")))
    (fs/delete-tree dir)))

(deftest raw-html
  (let [render #(-> (tpl/render-markdown :en % identity) :hiccup)]
    (binding [nooks.i18n/*strings* {:en {:template/diagram "Diagram"}}]
      (testing "allowlisted tags pass, others become text, comments vanish"
        (is (re-find #"<kbd>Ctrl</kbd>" (str (hiccup2.core/html (render "Press <kbd>Ctrl</kbd>.")))))
        (is (not (re-find #"<script" (str (hiccup2.core/html (render "<script>alert(1)</script>"))))))
        (is (not (re-find #"DRAFT" (str (hiccup2.core/html (render "<!-- DRAFT: x -->\n\n# T")))))))
      (testing "the leading DRAFT comment is reported"
        (is (:draft? (tpl/render-markdown :en "<!-- DRAFT: x -->\n\n# T" identity)))
        (is (not (:draft? (tpl/render-markdown :en "# T" identity)))))
      (testing "mermaid becomes a collapsed source block"
        (is (re-find #"<details class=\"diagram\">" (str (hiccup2.core/html (render "```mermaid\ngraph TD\n```")))))))))

(deftest blog-heading-ids
  (require 'nooks.blog)
  (let [f (resolve 'nooks.blog/github-heading-ids)]
    (is (= "<h2 id=\"1-introduction\">1. Introduction</h2>" (f "<h2 id=\"1.-introduction\">1. Introduction</h2>")))
    (is (= "<h2 id=\"7-authors-address\">7. Author&apos;s Address</h2>" (f "<h2 id=\"x\">7. Author&apos;s Address</h2>")))
    (is (re-find #"id=\"notes-1\"" (f "<h3 id=\"a\">Notes</h3><h3 id=\"b\">Notes</h3>")) "duplicates numbered")))
