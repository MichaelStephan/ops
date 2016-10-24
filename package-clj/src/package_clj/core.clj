(ns package-clj.core
  (:require [taoensso.timbre :as timbre :refer [infof error errorf]])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:gen-class))

(defn -main [include-folder riemann-file output-file & skip-includes]
  (let [skip-includes (set skip-includes)]
    (try+
     (infof "Not including %s" skip-includes)

     (->>
      (with-open [rdr (clojure.java.io/reader riemann-file)]
        (doall
         (map (fn [line]
                (if-let [[_ include-file] (re-find #"^\s*\(include\s+\"([^\"]+)\"\s*\)" line)]
                  (if (contains? skip-includes include-file)
                    line
                    (str "; ----- " include-file " START -----\n"
                         (slurp (str include-folder "/" include-file))
                         "; ----- " include-file " END -----\n"))
                  line))
              (line-seq rdr))))
      (clojure.string/join "\n")
      (spit output-file))
     (catch Object _
       (error "An unknown error occured" &throw-context)))))

(-main "/Users/i303874/Desktop/ops/riemann/etc"
       "/Users/i303874/Desktop/ops/riemann/etc/riemann.config"
       "/tmp/yyy.clj"
       "victorops.clj")
