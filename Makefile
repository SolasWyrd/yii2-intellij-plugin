.PHONY: help build test clean run lint package verify init rebuild install

help:
	@echo "Yii2 Model Magic Plugin"
	@echo ""
	@echo "Available commands:"
	@echo "  make build      - Build the plugin"
	@echo "  make test       - Run unit and platform tests"
	@echo "  make clean      - Clean build artifacts"
	@echo "  make run        - Run PhpStorm sandbox with the plugin"
	@echo "  make package    - Build plugin distribution"
	@echo "  make verify     - Run IntelliJ Plugin Verifier"
	@echo "  make lint       - Run repository checks"

init: clean lint test package verify

build:
	./gradlew build

test:
	./gradlew test

clean:
	./gradlew clean

package:
	./gradlew buildPlugin
	@echo "Plugin ZIP created in build/distributions/"

verify:
	./gradlew verifyPlugin

run:
	./gradlew runIde

lint:
	./gradlew check

rebuild: clean build

install: package
	@echo "Install via: Settings → Plugins → ⚙ → Install Plugin from Disk"
