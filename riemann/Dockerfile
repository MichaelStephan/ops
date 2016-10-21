FROM ubuntu:latest
# install dependencies, only jre and wget
RUN apt-get update && \
      apt-get install -y openjdk-8-jre-headless wget bzip2 && \
      rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
# setup riemann user
RUN addgroup riemann && \
      adduser --disabled-password --ingroup riemann --gecos '' riemann
RUN cd /opt && \
  wget https://aphyr.com/riemann/riemann-0.2.10.tar.bz2 && \
  tar -xvf riemann-0.2.10.tar.bz2 && \
  rm -rf riemann-0.2.10.tar.bz2 && \
  mv riemann-0.2.10 riemann
USER riemann
EXPOSE 2003
EXPOSE 5555
EXPOSE 5555/udp
EXPOSE 5556
ENTRYPOINT ["/opt/riemann/bin/riemann", "-a", "-Xmx1124m", "/etc/riemann/riemann.config"]
