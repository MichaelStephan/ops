(defn collect-appender-metrics [index]
  (streams
    (where (and
             (not= service nil)
             (re-find #" ch.qos.logback.core.Appender" service))
      (splitp re-find service
              #"debug count|error count|info count|trace count|warn count" (by [:host :service]
                                                                               (delta-over-time 60
                                                                                                (smap (configured-threshold-check)
                                                                                                      (configured-tell-team)
                                                                                                      index)))
              #"all count"
              ; Sum up counters for all hosts
              (by [:service]
                  (coalesce 4
                            (smap folds/sum
                                  (with {:host nil}
                                    (adjust [:service clojure.string/replace #"$" "_sum"]
                                            ; Calcuate delta every N seconds
                                            (delta-over-time 60
                                                             index))))))
              devnull))))

