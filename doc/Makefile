default:html

clean:
	rm -f *~
	rm -Rf FreeCol
	rm -f *.aux
	rm -f *.dvi
	rm -f *.log

html:clean
	latex FreeCol.tex
	latex FreeCol.tex
	latex2html -show_section_numbers FreeCol.tex
