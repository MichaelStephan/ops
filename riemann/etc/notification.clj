(defn- format-event [event-type event]
  { :message_type event-type
    :entity_id (:service event) 
    :state_message (str (:host event) " " (:service event) " is " (:state event) " (" (:metric event) ")")})

(defn victorops [api-key routing-key]
  {:info (fn [e] (post (format-event :INFO e) e api-key routing-key))
   :critical (fn [e] (post (format-event :CRITICAL e) e api-key routing-key))
   :warning (fn [e] (post (format-event :WARNING e) e api-key routing-key))
   :recover (fn [e] (post (format-event :RECOVERY e) e api-key routing-key))})

(defn notify [routing-key]
  (let [vo (victorops victorops-api-key routing-key)]
    (splitp re-find (:state event)
            #"^info$" (:info vo)
            #"^warning$" (:warning vo)
            #"^critical$" (:critical vo)
            #"^ok$" (:recover vo))))

(defn tell-team [mapping]
  (changed :state {:init "ok"}
           (where (not (maintenance-mode? event))
             (notify (get mapping (keyword (service-name event)) default-notification-mapping)))))
