(defn notify [routing-key]
  #_(let [vo (io (victorops victorops-api-key routing-key))]
    (changed-state {:init "ok"}
                   (where (state "info") (:info vo))
                   (where (state "warning") (:warning vo))
                   (where (state "critical") (:critical vo))
                   (where (state "ok") (:recovery vo)))))

(defn tell-team [mapping]
  (changed-state {:init "ok"}
                 (where (and
                          (not= service nil)
                          (not= event nil)
                          (not (maintenance-mode? event)))
                        prn
                        #_(notify "architecture")))) ; TODO
