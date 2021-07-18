# jrace
Java implementation of racing based automatic algorithm configurators.

Example run: 
`java -cp bin:lib/slf4j-api-1.7.5.jar:lib/logback-classic-1.0.13.jar:lib/logback-core-1.0.13.jar tune.Tuner -s random/scenario.txt`

To compile from source: `javac -d bin -cp lib/slf4j-api-1.7.5.jar:lib/logback-classic-1.0.13.jar:lib/logback-core-1.0.13.jar $(find . -name *.java)`

## Usage
Follow the the steps below to configure your algorithm with jrace:
1. Create a directory to store configuration scenarios.
```bash
mkdir tuning
cd tuning
```
2. Copy scenario template and parameter template file.
```bash
cp ../params.tmpl params.txt
cp ../scenario.tmpl scenario.txt
```
3. Specify the execution command line of running the target algorithm in `scenario.txt`, and the parameters to be configured in the `params.txt`. Make sure executing the target algorithm outputs the evaluation in the last line on screen. 
4. Put the directory of training instances in `scenario.txt`
5. Create a `seed-settings` file, recommendably with a random order of the training instances, and a seed associated with each training instances. It is a two-column text file with seed at the first column and instance name at the second column. Specify the `seed-settings` file in `scenario.txt`
6. Run jrace with a given scenario file, for example, 
```bash
java -cp bin:lib/slf4j-api-1.7.5.jar:lib/logback-classic-1.0.13.jar:lib/logback-core-1.0.13.jar tune.Tuner -s tuning/scenario.txt
```
