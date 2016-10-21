(defn collect-hystrix-metrics [index]
  (streams
    (where (and
             (not= service nil)
             (re-find #"_remote hystrix" service))
           (splitp re-find service
                   #"isCircuitBreakerOpen" (adjust (fn [{:keys [service metric] :as e}]
                                                     (assoc e :metric (if (= "true" metric)
                                                                        1.0
                                                                        0.0)))
                                                   (smap (configured-threshold-check)
                                                         (configured-tell-team)
                                                         index))
                   #"countSuccess" (by [:host :service]
                                       (delta-over-time 60
                                                        index))
                   #"countTimeout|countFailure|countExceptionsThrown|countThreadPoolRejected|countShortCircuited" (by [:host :service]
                                                                                                                      (delta-over-time 60
                                                                                                                                       (smap (configured-threshold-check)tell-team
                                                                                                                                             (configured-tell-team)
                                                                                                                                             index)))
                   #"latencyTotal_percentile" (by [:service] (coalesce
                                                               (smap folds/maximum
                                                                     (with :host nil
                                                                       index))))
                   devnull))))
