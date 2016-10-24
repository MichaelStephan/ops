(defn collect-health-signal [index]
  (where (and (not= service nil)
              (re-find #"-health$" service))
         (by [:service]
             (changed-state {:init "ok"}
                            (adjust [:state #(if (= % "expired")
                                               "critical"
                                               %)]
                                    (configured-tell-team)
                                    index)))))

(defn create-health-signal [index]
  (where (and
           (not= service nil)
           (or 
             ; add hystrix
             (re-find #" javax.servlet.Filter.2xx-responses count" service))
           (not (expired? event))
           (not= (:type event) "maintenance-mode"))
         (adjust #(let [{:keys [service]} %]
                    (assoc %
                           :service (str (first (clojure.string/split service #" |_" 2))
                                         "-health")
                           :metric 1
                           :ttl 30 
                           :host nil
                           :tags nil
                           :state "ok"))
                 reinject
                 index)))
