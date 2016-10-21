(defn service-name [{:keys [service]}]
  (let [[service metric] (clojure.string/split service #" " 2)]
    (cond
      (and
       (.endsWith service "_remote")
       (.startsWith metric "hystrix.HystrixCommand")) (let [tmp (clojure.string/split metric #"-" 4)
                                                            [_ _ remote-service] tmp]
                                                        (condp >= 4
                                                          (count tmp) remote-service
                                                          service))
      :else service)))

; borrowed from https://kartar.net/2016/04/managing-maintenance-with-riemann/
(defn maintenance-mode? [event]
  (let [service (service-name event)
        active? (->> (list 'and
                           '(= :type "maintenance-mode")
                           (list '= :service service))
                     (riemann.index/search (:index @core))
                     first
                     :state
                     (= "active"))]
    (when active?
    (info (str "Not notifying about `" service "` as maintenance mode is on"))
     active?)))

(defn collect-maintenance-signal [index]
  (streams
    (where (and
             (not (expired? event))
             (= "maintenance-mode" (:type event)))
           index)))
