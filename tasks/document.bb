#!/usr/bin/env bb

(ns tasks.document
  (:require [babashka.fs :as fs]
            [clojure.spec.alpha :as s]
            [babashka.tasks :refer [exec]]
            [com.thirstysink.pgmq-clj.specs]))

(def specs-header "# Specs")
(def docs-header "# Documentation")

(defn- write-divider [readme-file]
  (spit readme-file "\n---\n" :append true))

(defn- format-spec-def [spec]
  (str "\n```clojure\n" (pr-str (s/describe spec)) "\n" "```" "\n"))

(defn- write-fn-spec [readme-file spec]
  (write-divider readme-file)
  (spit readme-file (str "\n### " spec) :append true)
  (spit readme-file (format-spec-def spec) :append true))

(defn- write-arg-spec [readme-file spec]
  (spit readme-file (str "\n#### " spec) :append true)
  (spit readme-file (format-spec-def spec) :append true))

(defn- write-specs [readme-file]
  (println "Writing specs...")
  (spit readme-file (str "\n\n" specs-header "\n") :append true)
  (let [specs (reverse (keys (s/registry)))]
    (doseq [spec specs]
      (if  (= (first (str spec)) \:)
        (write-arg-spec readme-file spec)
        (write-fn-spec readme-file spec)))))

(defn- write-docs [readme-file docs-buffer]
  (println "Writing documentation...")
  (spit readme-file (str "\n\n" docs-header "\n") :append true)
  (spit readme-file docs-buffer :append true))

(defn- delete-files [files]
  (println (str "Deleting files " files "..."))
  (doseq [file files]
    (fs/delete file)))

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
    (spit readme-file tmpl-content)
    (write-divider readme-file)
    (write-docs readme-file docs-content)
    (write-specs readme-file)))

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
