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
    (is (= "Shop hours" (:label/shop-hours en)))
    (is (= "DIY recipes" (:label/diy-recipes en)))
    (is (= "Workbench" (:label/workbench en)))))

(deftest drafts-keep-their-marker
  ;; Fixture strings: the real files may have no drafts at any given time.
  (binding [i18n/*strings* {:en {:x/draft {:draft "One line inviting people in."} :x/label {:draft "Slot 7"}
                                 :label/workbench "Workbench"}
                            :pt-BR {}}]
    (testing "a draft renders as a span.draft"
      (is (= [:span.draft "One line inviting people in."] (i18n/t :en :x/draft))))
    (testing "a final string renders as itself"
      (is (= "Workbench" (i18n/t :en :label/workbench))))
    (testing "plain text keeps a visible marker"
      (is (= "[draft] Slot 7" (i18n/t-str :en :x/label))))
    (testing "pt-BR falls back to English"
      (is (= "Workbench" (i18n/t :pt-BR :label/workbench))))
    (testing "a missing key fails the build"
      (is (thrown? clojure.lang.ExceptionInfo (i18n/t :en :no/such-key))))))
