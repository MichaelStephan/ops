; borrowed from https://github.com/pyr/riemann-extra/blob/master/src/org/spootnik/riemann/thresholds.clj,
; until team makes available library in classpath
(defn find-specific-threshold
  [{:keys [host tags]}
   {:keys [match-host match-tag match-default] :as threshold}]
  (cond
   match-tag     (and ((set tags) match-tag) threshold)
   match-host    (and (= match-host host) threshold)
   match-default threshold))

(defn match-threshold
  [{:keys [service]} [pattern payload]]
  (when (re-matches pattern service)
    payload))

(defn find-threshold
  [thresholds re-patterns event]
  (if-let [thresholds (or (get thresholds (:service event))
                          (some (partial match-threshold event) re-patterns))]
    (if (sequential? thresholds)
      (some (partial find-specific-threshold event) thresholds)
      thresholds)))

(defn threshold-check
  "Given a list of standard or inverted thresholds, yield
   a function that will adapt an inputs state.
   The output function does not process events with no metrics"
  [thresholds]
  (let [re-patterns (filter (complement (comp string? key)) thresholds)]
    (fn [{:keys [metric tags state] :as event}]
      (try
        (if-let [{:keys [warning critical invert exact add-tags]}
                 (if metric (find-threshold thresholds re-patterns event))]
          (assoc event
            :tags (clojure.set/union (set tags) (set add-tags))
            :state
            (cond
             (nil? metric) "unknown"
             (and exact (not= (double metric) (double exact))) "critical"
             (and exact (= (double metric) (double exact)))    "ok"
             (and critical ((if invert <= >) metric critical)) "critical"
             (and warning ((if invert <= >) metric warning)) "warning"
             :else "ok"))
          (if (nil? state)
            (assoc event :state "ok")
            event))
        (catch Exception e
          (error e "threshold-check failed for " event))))))

