(ns maintainer-clj.core
  (:require [riemann.client :as r]
            [taoensso.timbre :as timbre :refer [infof error errorf]])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:gen-class))

(defn read-options [options]
  (if options
    (->> options
         (partition 2)
         (map (fn [[k v]]
                {(if (string? k)
                   (keyword (clojure.string/replace k #"^--" ""))
                   k)
                 v}))
         (apply merge))
    {}))

(def separator (str (clojure.string/join "" (take 20 (repeat "-"))) "\n"))

(defn println-help []
  (infof (str "\njava -jar maintainer-clj <CMD> <OPTIONS>\n"
              separator
              "CMDs:\n"
              " --help prints help\n"
              " --set activate maintenance mode\n"
              " --unset deactivate maintenance mode\n"
              separator
              "OPTIONs:\n"
              " --service SERVICE-NAME name of the service to set/unset maintenance mode for. This is MANDATORY for CMDs --set/--unset/--stats\n"
              " --host RIEMANN-TCP-HOST hostname of the remote riemann tcp server. If unset it defaults to localhost\n"
              " --port RIEMANN-TCP-PORT port the remote riemann tcp server is reachable at. If unset it defaults to 5555\n"
              " --ttl MINUTES ttl in minutes until the maintenance mode is deactivated. If unsed it defaults to 15 minutes"
              " --description TEXT description of maintenance activities")))


(defn riemann-client [{:keys [host port] :or {:host "localhost"
                                                      :port 5555} :as options}]
  (r/tcp-client (select-keys options [:host :port])))

(defn do-query [riemann-client]
  (let [ret (-> (r/query riemann-client "type = \"maintenance-mode\"")
                (deref 5000 :timeout))]
    (condp = ret
      :timeout (throw+ {:type :riemann-timeout})
      (infof "Stats\n %s" (apply str ret)))))

(defn do-set-or-unset [riemann-client {:keys [service ttl description] :or {ttl 15}} set?]
  (when-not service
    (throw+ {:type :option-missing :hint "service"}))
  (condp = (-> riemann-client
               (r/send-event {:service service
                              :ttl (* 60 15)
                              :type "maintenance-mode"
                              :description description
                              :state (if set?
                                       "active"
                                       "inactive")})
               (deref 5000 :timeout))
    :timeout (throw+ {:type :riemann-timeout})
    (infof "%s maintenance mode for service %s" (if set? "Enabled" "Disabled") service)))

(defn -main [& [cmd & options]]
  (try+ 
    (let [options (read-options options)]
      (condp = cmd
        "--help" (println-help) 
        "--set" (do-set-or-unset (riemann-client options) options true)
        "--unset" (do-set-or-unset (riemann-client options) options false)
        "--stats" (do-query (riemann-client options))
        (do
          (errorf "CMD %s is unknown" cmd)
          (println-help))))
    (catch [:type :riemann-timeout] {}
      (errorf "The communication with riemann timed out"))
    (catch [:type :option-missing] {:keys [hint]}
      (errorf "Option `%s` is missing" hint))
    (catch Object _
      (error "An unknown error occured" &throw-context)))
  ; hack to make the clj program quit
  (java.lang.System/exit 0))
