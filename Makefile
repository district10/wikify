all: README.html

%.html: %.md
	pandoc --ascii -s -S $< -o $@

build: target/wikify.jar
	java -jar $<
	cp search.full.xml generatedXml/search.full.xml.txt
	cp search.note.xml generatedXml/search.note.xml.txt
target/wikify.jar: src/main/java/com/tangzhixiong/wikify/Main.java
	mvn package
