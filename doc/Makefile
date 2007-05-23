default:html

clean:
	rm -f *~
	rm -Rf FreeCol
	rm -f *.aux
	rm -f *.dvi
	rm -f *.log
	rm -f *.out
	rm -f *.pdf

pdf:clean
	pdflatex FreeCol.tex
	pdflatex FreeCol.tex

html:pdf
	latex2html -show_section_numbers FreeCol.tex
