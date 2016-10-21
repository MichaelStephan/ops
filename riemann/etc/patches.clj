; overriding the graphite-server decode-graphite-line function. 
; It is responsible for parsing a line. Per definition graphite
; can only carry numeric metrics. This implementation is more
; flexible and also allows other types
(binding [*ns* (find-ns 'riemann.transport.graphite)] 
  (eval '(def decode-graphite-line
           (fn [line]
             (let [[service metric timestamp & garbage] (split line #"\s+")]
               ; Validate format
               (cond garbage
                     (throw+ "too many fields")
                     
                     (= "" service)
                     (throw+ "blank line")
                     
                     (not metric)
                     (throw+ "no metric")
                     
                     (not timestamp)
                     (throw+ "no timestamp")
                     
                     (re-find #"(?i)nan" metric)
                     (throw+ "NaN metric"))
    ; Parse numbers
    (let [metric (try (Double. metric)
                      (catch NumberFormatException e
                        ; still proceed although the metric could not be transformed into a numeric value
                        metric ))
          timestamp (try (Long. timestamp)
                         (catch NumberFormatException e
                           (throw+ "invalid timestamp")))
          [host service-name metric-name] (clojure.string/split service #"\." 3)]
      ; Construct event
      (->Event host 
               (str service-name " " metric-name)
               nil
               nil
               metric
               nil
               timestamp
               nil)))))))
