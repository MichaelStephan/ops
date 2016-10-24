(defn collect-thread-metrics [index]
  (where (and
           (not (expired? event))
           (not= service nil)
           (re-find #"thread.count|thread.blocked.count|thread.timed_waiting.count|thread.waiting.count" service))
         (smap (configured-threshold-check)
               (configured-tell-team)
                index)))
