(defn collect-health-signal [index]
  (streams
    (where (and (not= service nil)
                (re-find #"-health$" service))
           (by [:service]
               (changed-state {:init "ok"}
                              (adjust [:state #(if (= "expired")
                                                 "critical"
                                                 %)]
                                      (configured-tell-team)
                                      index))))))

(defn create-health-signal [index]
  (streams
    (where (and
             (not= service nil)
             (not (expired? event))
             (not= (:type event) "maintenance-mode"))
           (throttle 1 15
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
                             index)))))

