(ns nooks.i18n-test
  (:require [clojure.test :refer [deftest is testing]]
            [nooks.data :as data]
            [nooks.i18n :as i18n]))

(def strings (:i18n (data/load-all)))

(deftest value-forms
  (doseq [[lang m] strings
          [k v] m]
    (is (or (string? v) (and (map? v) (= #{:draft} (set (keys v))) (string? (:draft v))))
        (str lang " " k " must be a string or {:draft \"…\"}"))))

(deftest pt-BR-keys-exist-in-English
  (is (empty? (remove (set (keys (:en strings))) (keys (:pt-BR strings))))))

(deftest decided-labels-are-exact
  (let [en (:en strings)]
    (is (= "What're ya REPLin'?" (:label/replin en)))
    (is (= "DIY recipes" (:label/diy-recipes en)))
    (is (= "Workbench" (:label/workbench en)))))

(deftest drafts-keep-their-marker
  (binding [i18n/*strings* strings]
    (testing "a draft renders as a span.draft"
      (is (= [:span.draft "One line inviting people in for December."] (i18n/t :en :home/invite))))
    (testing "a final string renders as itself"
      (is (= "Workbench" (i18n/t :en :label/workbench))))
    (testing "plain text keeps a visible marker"
      (is (= "[draft] Slot 7" (i18n/t-str :en :route/slot-7))))
    (testing "pt-BR falls back to English"
      (is (= "Workbench" (i18n/t :pt-BR :label/workbench))))
    (testing "a missing key fails the build"
      (is (thrown? clojure.lang.ExceptionInfo (i18n/t :en :no/such-key))))))
