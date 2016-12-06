.PHONY: build clean

MKDNS = $(wildcard publish/*.md)
HTMLS = $(MKDNS:%.md=%.html)

all: generatedXml/search.full.xml $(HTMLS) build

%.html: %.md
	pandoc --ascii -s -S -f markdown+abbreviations+east_asian_line_breaks+emoji $< -o $@
publish/%.html: %.md
	pandoc --ascii -s -S -f markdown+abbreviations+east_asian_line_breaks+emoji $< -o $@
publish/search.full.xml: build

generatedXml/search.full.xml: publish/README.html publish/note.html
build: target/wikify.jar publish/README.html publish/note.html
	java -jar $< -i publish -o generatedXml

target/wikify.jar: src/main/java/com/tangzhixiong/wikify/Main.java src/main/java/com/tangzhixiong/wikify/Utils.java
	mvn package

clean:
	rm publish/*.html -f
	rm generatedXml/* -f
	rm target -rf
