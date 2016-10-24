; borrowed from https://kartar.net/2016/04/managing-maintenance-with-riemann/
(defn maintenance-mode? [event]
  (when-let [service (service-name event)]
    (let [active? (->> (list 'and
                             '(= :type "maintenance-mode")
                             (list '= :service service))
                       (riemann.index/search (:index @core))
                       first
                       :state
                       (= "active"))]
      (when active?
        (info (str "Not notifying about `" service "` as maintenance mode is on"))
        active?))))

(defn collect-maintenance-signal [index]
  (where (and
           (not (expired? event))
           (= "maintenance-mode" (:type event)))
         index))
