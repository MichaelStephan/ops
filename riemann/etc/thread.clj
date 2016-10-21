(defn collect-thread-metrics [index]
  (streams
    (where (and
             (not= service nil)
             (re-find #"thread.count|thread.blocked.count|thread.timed_waiting.count|thread.waiting.count" service))
           (smap (configured-threshold-check)
                 (configured-tell-team)
                  index))))

