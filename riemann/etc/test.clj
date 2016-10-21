(tests
  (deftest health-test
    (is (= (inject! [{:service "someWebApp"
                      :metric 1
                      :time 1}])
           {:index [{:service "someWebApp-health"
                     :time 1
                     :metric 1
                     :ttl 30
                     :state "ok"}]})))
 (deftest delta-over-time-with-appender-test
   (let [service "someWebApp ch.qos.logback.core.Appender.debug count"
         e {:service service 
            :host "a"
            :metric 100
            :time 1}]
     (is (= (inject! [e (assoc e
                               :metric 500
                               :time 5)])
            {:index [{:service (str service "_dt")
                      :host "a"
                      :metric 400
                      :state "ok"
                      :time 5
                      :ttl 60}]})))))

