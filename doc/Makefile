default:html

clean:
	rm -f *~
	rm -Rf FreeCol
	rm -f *.aux
	rm -f *.dvi
	rm -f *.log
	rm -f *.out
	rm -f *.pdf

html:clean
	pdflatex FreeCol.tex
	pdflatex FreeCol.tex
	latex2html -show_section_numbers FreeCol.tex
