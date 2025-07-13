(ns pcrit.pdb.yaml-test
  "AI Assistant thought there was :sort-keys option, but there isn't.
  This test keeps this belief from creeping back in."
  (:require [clojure.test :refer :all]
            [clj-yaml.core :as yaml]))

(deftest there-is-no-yaml-sort-keys-option
  (let [data {:z 1 :a 2 :m 3}
        yaml-sorted   (yaml/generate-string data {:sort-keys true :dumper-options {:flow-style :block}})
        yaml-unsorted (yaml/generate-string data {:sort-keys false :dumper-options {:flow-style :block}})]
    ;; When :sort-keys is true, keys should be sorted alphabetically
    (is (not (re-find #"a: 2\nm: 3\nz: 1" yaml-sorted)))
    ;; When :sort-keys is false, keys should appear in insertion order
    ;; (which in Clojure 1.9+ is the order of the map literal)
    (is (re-find #"z: 1\na: 2\nm: 3" yaml-unsorted))))

(deftest there-is-no-yaml-sort-keys-option-2
  (let [data {:z 1 :a 2 :m 3}
        yaml-sorted   (yaml/generate-string data :sort-keys true {:dumper-options {:flow-style :block}})
        yaml-unsorted (yaml/generate-string data :sort-keys false {:dumper-options {:flow-style :block}})]
    ;; When :sort-keys is true, keys should be sorted alphabetically
    (is (not (re-find #"a: 2\nm: 3\nz: 1" yaml-sorted)))
    ;; When :sort-keys is false, keys should appear in insertion order
    ;; (which in Clojure 1.9+ is the order of the map literal)
    (is (re-find #"z: 1\na: 2\nm: 3" yaml-unsorted))))

(deftest there-is-no-yaml-sort-keys-option-3
  (let [data {:z 1 :a 2 :m 3}
        yaml-sorted   (yaml/generate-string data :sort-keys true {:dumper-options {:flow-style :flow}})
        yaml-unsorted (yaml/generate-string data :sort-keys false {:dumper-options {:flow-style :flow}})]
    ;; When :sort-keys is true, keys should be sorted alphabetically
    (is (not (re-find #"a: 2, m: 3, z: 1" yaml-sorted)))
    ;; When :sort-keys is false, keys should appear in insertion order
    ;; (which in Clojure 1.9+ is the order of the map literal)
    (is (re-find #"z: 1, a: 2, m: 3" yaml-unsorted))))
