all: build

build:
	docker build -t riemann .

reload:
	docker kill -s SIGHUP riemann

run: build
	docker run --rm --name riemann -i -t -p 5555:5555 -p 5556:5556 -v ${PWD}/etc:/opt/riemann/etc riemann
