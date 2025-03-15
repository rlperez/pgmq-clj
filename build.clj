(ns build
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.java.shell :as shell]            ;; [deps-deploy.deps-deploy :as dd]
            [clojure.tools.build.api :as b]))

(defn- get-version []
  (-> "deps.edn"
      slurp
      edn/read-string
      :info
      :version))

(def lib 'com.thirstysink/pgmq-clj)
(def main-cls (string/join "." (filter some? [(namespace lib) (name lib) "core"])))
(def version (format (get-version)))
(def target-dir "target")
(def class-dir (str target-dir "/" "classes"))
(def uber-file (format "%s/%s.jar" target-dir (name lib)))
(def basis (b/create-basis {:project "deps.edn"}))

(defn clean
  "Delete the build target directory"
  [_]
  (println (str "Cleaning " target-dir))
  (b/delete {:path target-dir}))

(defn- pom-template [version]
  [[:description "A library to simplify working with PGMQ (https://tembo.io/pgmq/)."]
   [:url "https://github.com/rlperez/pgmq-clj"]
   [:licenses
    [:license
     [:name "MIT License"]
     [:url "https://opensource.org/license/mit"]]]
   [:developers
    [:developer
     [:name "Rigoberto L. Perez"]]]
   [:scm
    [:url "https://github.com/rlperez/pgmq-clj"]
    [:connection "scm:git:https://github.com/rlperez/pgmq-clj.git"]
    [:developerConnection "scm:git:ssh://git@github.com/rlperez/pgmq-clj.git"]
    [:tag (str "v" version)]]])

(defn- jar-opts [opts]
  (let [version (if (:snapshot opts) (str version "-SNAPSHOT") version)]
    (assoc opts
           :lib       lib
           :version   version
           :jar-file  (format "target/%s-%s.jar" lib version)
           :basis     (b/create-basis {})
           :class-dir class-dir
           :target    "target"
           :src-dirs  ["src"]
           :pom-data  (pom-template version))))

(defn build-project [_]
  (println "Compiling Clojure...")
  (b/compile-clj {:basis basis
                  :src-dirs ["src/clj"]
                  :class-dir class-dir}))

(defn write-docs [_]
  (shell/sh "bb" "document"))

(defn- package-jar [_]
  (println "Making jar...")
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :main main-cls
           :basis basis}))

(defn write-pom [opts]
  (println "Writing Pom...")
  (let [jar-opts (jar-opts opts)]
    (b/write-pom jar-opts)
    (b/copy-file {:src "target/classes/META-INF/maven/com.thirstysink/pgmq-clj/pom.xml"
                  :target "./pom.xml"})))

(defn jar [_]
  (build-project [])
  (write-docs [])
  (package-jar []))

;; (defn deploy "Deploy the JAR to Clojars." [opts]
;;   (let [{:keys [jar-file] :as opts} (jar-opts opts)]
;;     (dd/deploy {:installer :remote :artifact (b/resolve-path jar-file)
;;                 :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))}))
;;   opts)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn all [_]
  (clean nil) (write-pom nil) (jar nil))
