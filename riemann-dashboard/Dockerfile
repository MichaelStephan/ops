FROM ubuntu:latest
# install dependencies
RUN apt-get update && \
      apt-get install -y ruby1.9.1 libssl-dev zlib1g-dev && \
      rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN gem install riemann-dash

# setup riemann user
RUN addgroup riemann && \
      adduser --disabled-password --ingroup riemann --gecos '' riemann
ADD ./dash.sh /usr/bin/dash.sh
RUN chmod u+x /usr/bin/dash.sh
USER riemann
EXPOSE 4567
ENTRYPOINT ["/usr/bin/dash.sh"]
