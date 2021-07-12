# jrace
Java implementation of racing based automatic algorithm configurators.

Example run: 
`java -cp bin:lib/slf4j-api-1.7.5.jar:lib/logback-classic-1.0.13.jar:lib/logback-core-1.0.13.jar tune.Tuner -s random/scenario.txt`

To compile from source: `javac -d bin -cp lib/slf4j-api-1.7.5.jar:lib/logback-classic-1.0.13.jar:lib/logback-core-1.0.13.jar $(find . -name *.java)`
