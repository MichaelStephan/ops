(def devnull (fn [& _]))

(defn delta-over-time [n & children]
  (moving-time-window n (fn [[head & rest]]
                          (when rest
                            (let [fval (:metric head)
                                  l (folds/maximum rest)
                                  lval (:metric l)]
                              (when (and (number? fval) (number? lval))
                                (call-rescue (assoc l
                                                    :metric (- lval fval)
                                                    :service (str (:service l) "_dt"))
                                             children)))))))

