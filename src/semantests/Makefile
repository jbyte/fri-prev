#
# START GOOGLE CHROME WITH --allow-file-access-from-files
#

%.frames.xml : %.prev
	java -classpath ../../prg/bin compiler.Main --xsldir=../../prg/xsl --phase=frames --loggedphases=frames $<
%.seman.xml : %.prev
	java -classpath ../prg/bin compiler.Main --xsldir=../prg/xsl --phase=seman --loggedphases=seman $<

%.abstr.xml	: %.prev
	java -classpath ../prg/bin compiler.Main --xsldir=../prg/xsl --phase=abstr --loggedphases=abstr $<

%.synan.xml	: %.prev
	java -classpath ../prg/bin compiler.Main --xsldir=../prg/xsl --phase=synan --loggedphases=synan $<

%.lexan.xml	: %.prev
	java -classpath ../prg/bin compiler.Main --xsldir=../prg/xsl --phase=lexan --loggedphases=lexan $<

clean		:
	mv test.prev .test.prev
	'rm' -fr *.prev
	mv .test.prev test.prev
	'rm' -fr *.xml
