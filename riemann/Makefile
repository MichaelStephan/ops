all: build

build:
	docker build -t riemann .

reload:
	docker kill -s SIGHUP riemann

run: build
	docker run --rm --name riemann -i -t -p 2003:2003 -p 5555:5555 -p 5556:5556 -v ${PWD}/etc:/etc/riemann riemann

test: build
	docker run --rm -v ${PWD}/etc:/etc/riemann riemann test
