all: build

clean:
	mvn clean

build: clean
	mvn compile assembly:single

package: build
	docker build -t michaelstephan/somewebapp .

publish: package
	docker push michaelstephan/somewebapp

run:
	docker run -i -t --rm --name somewebapp -p 6666:6666 -e RIEMANN_HOST=localhost -e GRAPHITE_HOST=localhost:2003 michaelstephan/somewebapp

cfdeploy: publish
	cf push somewebapp --docker-image michaelstephan/somewebapp
