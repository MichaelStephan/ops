(defn collect-gc-metrics [index]
  (where (and
           (not (expired? event))
           (not= service nil)
           (re-find #" gc" service))
         index))
