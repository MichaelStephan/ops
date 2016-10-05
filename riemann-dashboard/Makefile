all: build

build:
	docker build -t riemann-dash .

run: build
	docker run --rm -i -t -p 4567:4567 riemann-dash
