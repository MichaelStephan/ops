FROM ubuntu:latest
# install dependencies
RUN apt-get update && \
      apt-get install -y ruby1.9.1 libssl-dev zlib1g-dev && \
      rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN gem install riemann-dash

# setup riemann user
RUN addgroup riemann && \
      adduser --disabled-password --ingroup riemann --gecos '' riemann
ADD ./config.rb /etc/riemann/riemann-dash-config.rb
USER riemann
EXPOSE 4567
ENTRYPOINT ["riemann-dash", "/etc/riemann/riemann-dash-config.rb"]
