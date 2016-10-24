(defn collect-memory-metrics [index]
  (where (and (not (expired? event))
              (not= service nil)
              (re-find #" memory" service)
              (re-find #".*heap\.used$" service))
         (scale (/ 1 1024 1024)
                (smap (configured-threshold-check)
                      (configured-tell-team)
                      index))))
