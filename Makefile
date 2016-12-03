all: README.html

%.html: %.md
	pandoc --ascii -s -S $< -o $@
