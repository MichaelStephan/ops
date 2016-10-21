(defn collect-gc-metrics [index]
  (streams
    (where (and
             (not= service nil)
             (re-find #" gc" service))
           index)))
