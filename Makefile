.PHONY: all
all: build

.PHONY: build
build:
	mvn package --global-toolchains toolchains.xml

.PHONY: clean
clean:
	mvn clean

.PHONY: rebuild
rebuild: 
	mvn clean package --global-toolchains toolchains.xml

.PHONY: install
install:
	mvn install --global-toolchains toolchains.xml

.PHONY: reinstall
reinstall:
	mvn clean install --global-toolchains toolchains.xml