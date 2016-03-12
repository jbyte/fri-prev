# compile the Main class (basicly the whole package)
compile:
	javac -d ../bin/  compiler/Main.java

# compile targer .java file
%.class: %.java
	javac -d ../bin $<

# clean class files
clean:
	'rm' -rf ../bin/*
