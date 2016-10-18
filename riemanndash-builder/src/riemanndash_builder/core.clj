(ns riemanndash-builder.core
  (:require [clojure.data.json :as json]))

(def id-counter_ (atom 0))

(defn get-id []
  (gensym (swap! id-counter_ inc)))

(defn merge-attrs [root attrs]
  (merge root (apply hash-map attrs)))

(defn template [& attrs]
  (merge-attrs
   {:id (get-id)
    :weight 1}
   attrs))

(defn home [])

(defn baloon-template [& attrs]
  (merge-attrs
   (template
    :type "Balloon"
    :version 0)
   attrs))

(defn hstack-template [& attrs]
  (merge-attrs
   (template
    :type "HStack"
    :version 0)
   attrs))

(defn vstack-template [& attrs]
  (merge-attrs
   (template
    :type "VStack"
    :version 0)
   attrs))

(defn flot-template [& attrs]
  (merge-attrs
   (template
    :type "Flot"
    :version 9
    :min 0
    :max nil
    :timeRange 300
    :graphType "line"
    :stackMode "false"
    :tooltips "metric")
   attrs))

(defn grid-template [& attrs]
  (merge-attrs
   (template
    :type "Grid"
    :version 9
    :max ""
    :rows ""
    :cols ""
    :row_sort "lexical"
    :col_sort "lexical")
   attrs))

(defn gauge-template [& attrs]
  (merge-attrs
   (template
    :type "Gauge"
    :version 1)
   attrs))

(defn log-template [& attrs]
  (merge-attrs
   (template
    :type "Log"
    :lines 1000)
   attrs))

(defn profiler-template [app]
  {:name (str app " profiler")
   :view (baloon-template
          :child (vstack-template
                  :children [(hstack-template
                              :children [(gauge-template
                                          :title "hosts"
                                          :query (str "service ~= \"" app " profiler hosts\""))
                                         (gauge-template
                                          :title "rate"
                                          :query (str "service ~= \"" app " profiler rate\""))])
                             (grid-template
                              :weight 10
                              :title "functions"
                              :query (str "service ~= \"" app " profiler fn\"")
                              :rows "service"
                              :cols "value"
                              :row_sort "metric")]))})

(defn hystrix-cmd-template [app cmd]
  (let [metric (fn [metric-name]
                 (str app " hystrix.HystrixCommand." cmd "." metric-name))]
    {:name cmd
     :view (baloon-template
            :child (vstack-template
                    :children [(hstack-template
                                :weight 5
                                :children [(flot-template
                                            :weight 8
                                            :title "percentiles"
                                            :query (str "(service =~ \"" (metric "latencyTotal_percentile%") "\") and (host = nil)"))
                                           (grid-template
                                            :weight 2
                                            :title "circuit breaker open?"
                                            :query (str "(service = \"" (metric "isCircuitBreakerOpen") "\")"))])
                               (grid-template
                                :weight 5
                                :title (str cmd " stats")
                                :query "(service =~ \"%hystrix%_dt\")")]))}))

(defn logs-template [app]
  {:name (str app " log")
   :view (baloon-template
          :child (vstack-template
                  :children [(flot-template
                              :title "logs"
                              :weight 2
                              :query (str "(service = \"" app " ch.qos.logback.core.Appender.all count_sum_dt\")"))
                             (grid-template
                              :title "counter"
                              :weight 8
                              :query (str "(service =~ \"" app " ch.qos.logback.core.Appender.%count_dt\")"))]))})

(defn jvm-template [app]
  {:name (str app " jvm")
   :view (baloon-template
          :child (vstack-template
                  :children [(grid-template
                              :weight 5
                              :title "gc"
                              :query (str "(service =~ \"" app " gc%\")"))
                             (grid-template
                              :weight 6
                              :title "thread"
                              :query (str "(service =~ \"" app " thread%\")"))
                             (grid-template
                              :weight 7
                              :title "memory"
                              :query (str "(service =~ \"" app " memory%used\")"))]))})

(defn http-template [app]
  {:name (str app " http")
   :view (baloon-template
          :child (vstack-template
                  :children [(gauge-template
                              :title "requests"
                              :weight 1
                              :query (str "(service = \"" app " javax.servlet.Filter.requests count_sum\") and (host = nil)"))
                             (grid-template
                              :weight 4
                              :title "status"
                              :query (str "(service =~ \"" app " javax.servlet.Filter.%xx-responses count_dt\")"))
                             (grid-template
                              :weight 4
                              :title "times"
                              :query (str "(service =~ \"" app " javax.servlet.Filter.responsetimes p%\")"))]))})

(defn maintenance-template []
  {:name "maintenance"
   :view (baloon-template
          :child (vstack-template
                  :children [(log-template
                              :title "status"
                              :query "(type = \"maintenance-mode\")")]))})

(defn health-template []
  {:name "health"
   :view (baloon-template
          :child (vstack-template
                  :children [(grid-template
                              :title "health"
                              :query "service =~ \"%health\"")]))})

(defn hystrix-cmds-template [app cmds]
  (flatten (map (fn [[service cmds]]
                  (flatten (map (fn [cmd]
                                  (hystrix-cmd-template (str app "_remote") (str (name service) "-" cmd)))
                                cmds)))
                hystrix-cmds)))

(def hystrix-cmds {:package-v1-subscription ["subscription-getBySubscriberAndId" "subscription-getByTenant" "subscription-terminateSubscriptions"]
                   :package-v1-repository ["data-getByQuery" "data-Update" "data-Create" "data-Delete" "data-AttributeDelete" "data-getById"]
                   :package-v1-organization ["data-getById"]
                   :package-v1-pubsubService ["data-removeProductByPackage" "data-notifySubscribersByPackage"]
                   :package-v1-emailService ["sendEmail"]
                   :package-v1-catalogService ["data-createProduct" "data-deleteProduct"]
                   :package-v1-apiManagementService ["data-getServicesByProject" "data-getBuilderModulesByProject" "data-getApplicationById" "data-getClientCredentialsById"]
                   :package-v1-accountService ["data-getProjectById" "data-getProjectUserRoles"]})

(defn main-template []
  (let [app "someWebApp"]
    {:server "127.0.0.1:5556"
     :server_type "ws"
     :workspaces (remove nil?
                         (concat [(profiler-template app)
                                  (jvm-template app)
                                  (logs-template app)
                                  (http-template app)
                                  (maintenance-template)
                                  (health-template)]
                                 (hystrix-cmds-template app hystrix-cmds)))}))

(spit "/Users/i303874/Desktop/ops/riemann-dashboard/config.json" (json/write-str (main-template)))
