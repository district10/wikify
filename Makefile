.PHONY: build clean

all: publish/README.html publish/note.html generatedXml/search.full.xml build

%.html: %.md
	pandoc --ascii -s -S $< -o $@
publish/%.html: %.md
	pandoc --ascii -s -S $< -o $@
publish/search.full.xml: build

generatedXml/search.full.xml: publish/README.html publish/note.html
build: target/wikify.jar publish/README.html publish/note.html
	java -jar $< -i publish -o generatedXml

target/wikify.jar: src/main/java/com/tangzhixiong/wikify/Main.java src/main/java/com/tangzhixiong/wikify/Utils.java
	mvn package

clean:
	rm publish/*.html
	rm generatedXml/*
	rm target -rf
