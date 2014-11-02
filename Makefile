JAVAC=javac
JFLAGS=-g
SRC=src
.SUFFIXES: .java .class
.java.class:
	$(JAVAC) $(JFLAGS) $*.java

CLASSES= \
	./src/com/sildfs/server/*.java\
	./src/com/sildfs/message/*.java\
	./src/com/sildfs/tool/*.java\
	./src/com/sildfs/exception/*.java\
	./src/com/sildfs/transaction/*.java

default: classes

classes = $(CLASSES:.java=.class)

clean :
	rm -f *.class
	
