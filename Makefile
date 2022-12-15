JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
P2POmokClient.java\
P2POmokServer.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class