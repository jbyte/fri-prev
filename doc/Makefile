lang-spec.pdf: lang-spec.tex ../prg/src/compiler/phase/synan/prev-lr.grammar
	./grammar.sh
	pdflatex lang-spec.tex

lang-spec-ll.pdf: lang-spec-ll.tex ../prg/src/compiler/phase/synan/prev-ll.grammar
	#./grammar-ll.sh
	pdflatex lang-spec-ll.tex

clean:
	'rm' -fr grammar.tex
	'rm' -fr *~ *.aux *.log *.pdf *.gz
