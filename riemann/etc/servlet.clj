(defn collect-servlet-metrics [index]
  (let [index (smap (configured-threshold-check)
                    index)]
    (where (and
             (not (expired? event))
             (not= service nil)
             (re-find #" javax.servlet.Filter" service))
      (splitp re-find service
              #"\.requests count" (coalesce
                                    (smap folds/sum
                                          (adjust [:service clojure.string/replace #"$" "_sum"]
                                                  (with :host nil
                                                    index))))
              #"xx-responses count" (by [:host :service]
                                        (delta-over-time 60
                                                         (smap (configured-threshold-check)
                                                               (configured-tell-team)
                                                               index)))
              #".*p.*" index
            devnull))))

