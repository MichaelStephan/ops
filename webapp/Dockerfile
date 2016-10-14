FROM ubuntu:latest
RUN apt-get update && \
    apt-get install -y default-jre && \ 
    apt-get install -y net-tools && \ 
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
ADD target/webapp-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
ENV RIEMANN_HOST riemann-sevenup.eu-central.tools.yaas.io
ENV GRAPHITE_ENDPOINT riemann-sevenup.eu-central.tools.yaas.io:2003 
EXPOSE 6666
CMD java -jar app.jar 
