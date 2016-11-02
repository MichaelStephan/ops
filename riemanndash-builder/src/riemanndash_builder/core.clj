(ns riemanndash-builder.core
  (:require [clojure.data.json :as json]
            [taoensso.timbre :as timbre :refer [infof error errorf]])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:gen-class))

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

(defn profiler-template [app app-short]
  {:name (str app-short " profiler")
   :view (baloon-template
          :child (vstack-template
                  :children [(hstack-template
                              :children [(gauge-template
                                          :title "hosts"
                                          :query (str "service =~ \"" app " profiler hosts\""))
                                         (gauge-template
                                          :title "rate"
                                          :query (str "service =~ \"" app " profiler rate\""))])
                             (grid-template
                              :weight 10
                              :title "functions"
                              :query (str "service ~= \"" app " profiler fn\"")
                              :rows "service"
                              :cols "value"
                              :row_sort "metric")]))})

(defn hystrix-metric-name [app cmd metric-name]
  (str app " hystrix.HystrixCommand." cmd "." metric-name))

(defn hystrix-cmd-template [app cmd]
  (let [metric-name (partial hystrix-metric-name app cmd)]
    {:name cmd
     :view (baloon-template
            :child (vstack-template
                    :children [(hstack-template
                                :weight 5
                                :children [(flot-template
                                            :weight 8
                                            :title "percentiles"
                                            :query (str "(service =~ \"" (metric-name "latencyTotal_percentile%") "\") and (host = nil)"))
                                           (grid-template
                                            :weight 2
                                            :title "circuit breaker open?"
                                            :query (str "(service = \"" (metric-name "isCircuitBreakerOpen") "\")"))])
                               (grid-template
                                :weight 5
                                :title (str cmd " stats")
                                :query (str "(service =~ \"" (metric-name "%_dt") "\")"))]))}))

(defn logs-template [app app-short]
  {:name (str app-short " log")
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

(defn jvm-template [app app-short]
  {:name (str app-short " jvm")
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

(defn http-template [app app-short]
  {:name (str app-short " http")
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
                              :query "service =~ \"%health\""
                              :rows "service"
                              :cols "1")]))})

(defn hystrix-cmds-template [app cmds]
  (flatten
   (map (fn [[service cmds]]
          (flatten
           (map (fn [cmd]
                  (hystrix-cmd-template (str app "_remote") (str (name service) "-" cmd)))
                cmds)))
        cmds)))

(defn dash-template [& attrs]
  (merge-attrs
   {:server "127.0.0.1:5556"
    :server_type "ws"}
   attrs))

(defn main-template [app-name app-short-name]
  (dash-template
   :workspaces (remove nil?
                       (concat [(profiler-template app-name app-short-name)
                                (jvm-template app-name app-short-name)
                                (logs-template app-name app-short-name)
                                (http-template app-name app-short-name)
                                (maintenance-template)
                                (health-template)]))))

(defn main-error-template [apps]
  (dash-template
   :workspaces [{:name "errors"
                 :view (baloon-template
                        :child (vstack-template
                                :children [(hstack-template
                                            :weight 2
                                            :children (for [[long-name short-name _] apps]
                                                        (flot-template
                                                         :title (str short-name " http 5xx")
                                                         :timeRange 900
                                                         :weight 4
                                                         :graphType "bar"
                                                         :query (str "(service = \"" long-name " javax.servlet.Filter.5xx-responses count_dt\") and (host = nil)"))))
                                           (hstack-template
                                            :weight 2
                                            :children (for [[long-name short-name _] apps]
                                                        (flot-template
                                                         :title (str short-name " appender errors")
                                                         :timeRange 900
                                                         :weight 4
                                                         :graphType "bar"
                                                         :query (str "(service = \"" long-name " ch.qos.logback.core.Appender.error count_dt\") and (host = nil)"))))
                                           (hstack-template
                                            :weight 2
                                            :children (for [[long-name short-name _] apps]
                                                        (flot-template
                                                         :title (str short-name " hystrix errors")
                                                         :timeRange 900
                                                         :weight 4
                                                         :graphType "bar"
                                                         :query (str "(service =~ \"" long-name " hystrix.HystrixCommand.countFailure_dt\") and (host = nil)"))))]))}]))

(defn main-hystrix-cmds-template [app-name app-short-name cmds]
  (dash-template
   :workspaces (remove nil?
                       (concat (hystrix-cmds-template app-name cmds)))))

(defn file-path [root-path file-name]
  (str root-path (when-not (.endsWith root-path "/") "/") file-name))

(defn config-rb [config-name]
  (str "set  :port, 4567\n"
       "set  :bind, '0.0.0.0'\n"
       "config[:ws_config] = '/etc/riemann/" config-name "'"))

(def package-hystrix-cmds {:package-v1-subscription ["subscription-getBySubscriberAndId" "subscription-getByTenant" "subscription-terminateSubscriptions"]
                           :package-v1-repository ["data-getByQuery" "data-Update" "data-Create" "data-Delete" "data-AttributeDelete" "data-getById"]
                           :package-v1-organization ["data-getById"]
                           :package-v1-pubsubService ["data-removeProductByPackage" "data-notifySubscribersByPackage"]
                           :package-v1-emailService ["sendEmail"]
                           :package-v1-catalogService ["data-createProduct" "data-deleteProduct"]
                           :package-v1-apiManagementService ["data-getServicesByProject" "data-getBuilderModulesByProject" "data-getApplicationById" "data-getClientCredentialsById"]
                           :package-v1-accountService ["data-getProjectById" "data-getProjectUserRoles"]})

(def subscription-management-hystrix-cmds {:subscription-management-v1-accountservice ["data-GetProjectRoles" "data-GetProjectRolesInfo" "data-GetProjectById" "data-GetAvailableUserProjects"]
                                           :subscription-management-v1-bulkemailservice ["data-CreateSubscribersMailingList" "data-CreateSubscriptionsEmailJob"]
                                           :subscription-management-v1-xtraservice ["data-GetMarketData"]
                                           :subscription-management-v1-orderservice ["data-getById" "data-Create"]
                                           :subscription-management-v1-organizationservice ["data-GetById"]
                                           :subscription-management-v1-packageservice ["data-GetPackageByVersionId" "data-GetPackageByPackageId"]
                                           :subscription-management-v1-subscriptionservice ["data-GetTenantSubscriptions" "data-GetSubscriptionById" "data-GetPackageSubscriptions"]})

(def market-catalog-hystrix-cmds {:catalog-management-v1-packageservice ["data-GetPackageByVersionId"]
                                  :catalog-management-v1-productservice ["data-getById" "data-create" "data-getAll" "data-delete"]})

(def apps [["package" "pkg" package-hystrix-cmds]
           ["subscription-management" "sub" subscription-management-hystrix-cmds]
           ["market-catalog" "cat" market-catalog-hystrix-cmds]])

(defn hystrix-thresholds [app cmds metric-name]
  (flatten
   (map (fn [[service cmds]]
          (flatten
           (map (fn [cmd]
                  (hystrix-metric-name app cmd metric-name))
                cmds)))
        cmds)))

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
  (infof (str "\njava -jar dashbuilder-clj <CMD> <OPTIONS>\n"
              separator
              "CMDs:\n"
              " --dashboards export dashboards\n"
              " --thresholds export thresholds\n"
              separator
              "OPTIONs:\n"
              " --destination FILE destination, mandatory for --dashboards/ --thresholds\n")))

(defn do-export-dashboards [{:keys [destination]}]
  (when-not destination
    (throw+ {:type :option-missing :hint "destination"}))

  (let [file-path (partial file-path destination)]
    (doall
    ; main templates
     (map (fn [[long short :as app]]
            (let [config-name (str short "-config.json")]
              (->> app
                   (take 2)
                   (apply main-template)
                   (json/write-str)
                   (spit (file-path config-name)))
              (->> (config-rb config-name)
                   (spit (file-path (str short "-config.rb"))))))
          apps))
    ; main hystrix templates
    (doall
     (map (fn [[long short cmds :as app]]
            (let [config-name (str short "-hys-config.json")]
              (->>
               app
               (apply main-hystrix-cmds-template)
               (json/write-str)
               (spit (file-path config-name)))
              (->>
               (config-rb config-name)
               (spit (file-path (str short "-hys-config.rb"))))))
          apps))
    (let [config-name "err-config.json"]
      (spit (file-path "err-config.rb") (config-rb config-name))
      (spit (file-path config-name) (-> (main-error-template apps) (json/write-str))))))

(defn do-export-thresholds [{:keys [destination]}]
  (when-not destination
    (throw+ {:type :option-missing :hint "destination"}))
  (->> apps
       (map (fn [[long short cmds]]
              (hystrix-thresholds long cmds (str long "_remote"))))
       (flatten)
       (map (fn [s] (str "\"" s "\" {:critical 100}")))
       (clojure.string/join "\n")
       (spit destination)))

(defn -main [& [cmd & options]]
  (try+
   (let [options (read-options options)]
     (condp = cmd
       "--dashboards" (do-export-dashboards options)
       "--thresholds" (do-export-thresholds options)
       (do
         (errorf "CMD %s is unknown" cmd)
         (println-help))))
   (catch [:type :option-missing] {:keys [hint]}
     (errorf "Option `%s` is missing" hint))
   (catch Object _
     (error "An unknown error occured" &throw-context)))
  ; hack to make the clj program quit
  #_(java.lang.System/exit 0))

#_(-main "--thresholds" "--destination" "/Users/i303874/Desktop/ops/riemann-dashboard/bin/etc/thresholds.clj")
(-main "--dashboards" "--destination" "/Users/i303874/Desktop/ops/riemann-dashboard/bin/etc/")
