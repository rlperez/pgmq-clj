(ns tasks.document
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [babashka.tasks :refer [exec]]
            [com.thirstysink.pgmq-clj.specs]))

(defn- write-specs [readme-buffer]
  (let [specs (keys (s/registry))]
    (.write readme-buffer "\n\n# Specs")
    (doseq [spec specs]
      (.write readme-buffer (str "\n### " spec "\n"))
      (.write readme-buffer (str "```clojure-n" (pr-str (s/describe spec)) "\n```\n")))))

(defn- write-docs [readme-buffer docs-buffer]
  (.write readme-buffer "\n\n# Documentation\n")
  (.write readme-buffer (slurp docs-buffer)))

(defn- delete-files [files]
  (doseq [file files]
    (io/delete-file file)))

(defn- build-readme [template-path readme-path docs-path]
  (let [bkp-path (str readme-path ".bkp")
        readme-file (io/file readme-path)
        readme-bkp-file (io/file bkp-path)]
    (io/copy readme-file readme-bkp-file)
    (io/delete-file readme-file)
    (with-open [tmpl-buffer (io/reader template-path)
                docs-buffer (io/reader docs-path)
                readme-buffer (io/writer readme-file)]
      (.write readme-buffer (slurp tmpl-buffer))
      (write-docs readme-buffer docs-buffer)
      (write-specs readme-buffer))))

(defn- build-documentation []
  (let [documents-path "API.md"
        readme-tmpl-path "README.md.tmpl"
        readme-path "README.md"
        readme-bkp-path (str readme-path ".bkp")]
    (exec 'quickdoc.api/quickdoc)
    (build-readme readme-tmpl-path readme-path documents-path)
    (delete-files [documents-path readme-bkp-path])))

(defn -main [& _]
  (println "Generating documentation...")
  (build-documentation)
  (println "Documentation complete!"))
