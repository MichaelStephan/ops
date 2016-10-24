(defn service-name [{:keys [service]}]
  (if service
    (let [[service metric] (clojure.string/split service #" " 2)]
      (cond
        (and
          (.endsWith service "_remote")
          (.startsWith metric "hystrix.HystrixCommand")) (let [tmp (clojure.string/split metric #"-" 4)
                                                               [_ _ remote-service] tmp]
                                                           (condp >= 4
                                                             (count tmp) remote-service
                                                             service))
        :else service))
    "unknown"))

