(defn collect-profiler-metrics [index]
  (streams
    (where (not= service nil)
      (splitp re-find service
              ; Aggregate rate of samples taken
              #"profiler rate" (coalesce
                                   ; Total sample rate
                                   (smap folds/sum
                                         (with :host nil
                                               index))

                                   ; Distinct number of hosts
                                   (smap folds/count
                                         (adjust [:service clojure.string/replace "rate" "hosts"]
                                                 (with :host nil
                                                   index))))

              ; Flatten function times across hosts, updating every 60s.
              #"profiler fn"
              (pipe - (by :service
                          (coalesce 60
                                    (smap folds/sum
                                          (with {:host nil :ttl 120} -))))
                    ; And index the top 10.
                    (top 10 :metric
                         index
                         (with :state "expired" index)))
              devnull))))

