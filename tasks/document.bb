(ns tasks.document
  (:require [babashka.fs :as fs]
            [clojure.spec.alpha :as s]
            [babashka.tasks :refer [exec]]
            [com.thirstysink.pgmq-clj.specs]))

(defn- write-specs [readme-file]
  (println "Writing specs...")
  (let [specs (keys (s/registry))]
    (spit readme-file "\n\n# Specs" :append true)
    (doseq [spec specs]
      (spit readme-file (str "\n### " spec "\n") :append true)
      (spit readme-file (str "```clojure\n" (pr-str (s/describe spec)) "\n```\n") :append true))))

(defn- write-docs [readme-file docs-buffer]
  (println "Writing documentation...")
  (spit readme-file "\n\n# Documentation\n" :append true)
  (spit readme-file docs-buffer :append true))

(defn- delete-files [files]
  (println (str "Deleting files " files "..."))
  (doseq [file files]
    (fs/delete file)))  ; Ensure proper deletion of the files

(defn- build-readme [template-path readme-path docs-path]
  (println (str "Building " readme-path "..."))
  (let [bkp-path (str readme-path ".bkp")
        readme-file (fs/file readme-path)
        readme-bkp-file (fs/file bkp-path)
        tmpl-content (slurp template-path)
        docs-content (slurp docs-path)]
    (fs/delete-if-exists readme-bkp-file)
    (fs/copy readme-file readme-bkp-file)
    (fs/delete readme-file)
    (spit readme-file tmpl-content)      ; Write template content to readme
    (write-docs readme-file docs-content) ; Write documentation content to readme
    (write-specs readme-file)))          ; Write specs to readme

(defn- build-documentation []
  (println "Generating documentation...")
  (let [documents-path "API.md"
        readme-tmpl-path "README.md.tmpl"
        readme-path "README.md"
        readme-bkp-path (str readme-path ".bkp")]
    (exec 'quickdoc.api/quickdoc)
    (build-readme readme-tmpl-path readme-path documents-path)
    (delete-files [documents-path readme-bkp-path])))

(defn -main [& _]
  (build-documentation)
  (println "Documentation complete!"))
