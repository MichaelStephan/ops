# Monitoring & notification stragegy - team 7 up - v1.0

## Introduction

### Goal
The main goal is to detect issues in the 7 up applications proactively prior any customers are negatively impacted.

### Problem
The only monitoring system available for normal application development teams is `uptime`. It's functionality is very constrained as it can only emit http based health pings against a test subject. It uses this information to decide whether the subject is in good or bad health conditions. A ping can only be used to check whether a single instance out of multiple responds with http status code 200 (= ok) or some other value (= nok); testing a cluster of services is impossible in the YaaS setup.

TODO add visualization of problem

### Solution
In order to improve the mean-time-to-detect and mean-time-to-repair KPIs the team 7 up follows a new monotoring strategy which relies on `riemann` as its nerve system. The new strategy requires the test subject to periodically emit health relevant information to `riemann`. In riemann the data is collected and reasoning logic decides whether the test subject is in good or bad health condition.

TODO add visualization of solution
