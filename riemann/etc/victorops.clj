; heavily based on https://github.com/aphyr/riemann/blob/master/src/riemann/pagerduty.clj
;"Send alerts to VictorOps over HTTPS through their REST endpoint"
(require '[cheshire.core :as json])
(require '[clj-http.client :as client])

(def ^:private event-url "https://alert.victorops.com/integrations/generic/20131114/alert")

(defn- post
  "POST to the VictorOps REST API."
  [request event api-key routing-key]
  (try
    (
      client/post (str event-url "/" api-key "/" routing-key)
      {
        :body (json/generate-string request)
        :socket-timeout 5000
        :conn-timeout 5000
        :content-type :json
        :accept :json
        :throw-entire-message? true
      }
    )
    (catch java.net.ConnectException ex (info event))
  )
)

(defn- format-event
  "Formats an event for VictorOps"
  [event-type event]
  {
    :message_type event-type
    :entity_id (str (:host event) "/" (:service event))
    :state_message (str (:host event) " " (:service event) " is " (:state event) " (" (:metric event) ")")
    :monitoring_tool "riemann"
  }
)

(defn victorops
  "Creates a VictorOps adapter. Takes your api and routing key, and returns a map of functions which trigger events.
  (let [vo (victorops \"my-api-key\", \"my-routing-key\")]
  (changed-state
    (where (state \"ok\") (:recover vo))
    (where (state \"warning\") (:trigger vo))
    (where (state \"critical\") (:trigger vo))))"
  [api-key routing-key]
  {
    :info         (fn [e] (post (format-event :INFO e) e api-key routing-key))
    :trigger      (fn [e] (post (format-event :CRITICAL e) e api-key routing-key))
    :warning      (fn [e] (post (format-event :WARNING e) e api-key routing-key))
    :recover      (fn [e] (post (format-event :RECOVERY e) e api-key routing-key))
  }
)

; redefine alert method

(def alert
  (let [vo (victorops "eebe09e8-2023-4087-8f5f-2bd570a850b", "7up")]
    (where (not (state "expired"))
      (changed-state {:init "ok"}
        (stable 0 :state
          (where (state "ok") (:recover vo))
          (where (state "warning") (:warning vo))
          (where (or (state "critical") (state "blocker")) (:trigger vo))
        )
      )
    )
  )
)

