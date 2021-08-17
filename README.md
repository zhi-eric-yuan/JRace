# jrace
Java implementation of racing based automatic algorithm configurators.

## Build and run
The best way is to build the project by maven: 
`mvn compile`.
Then the binary files will be generated to the directory `target/classes`. One can also build the binary jar file by running `mvn clean compile assembly:single`. The jar file will be found in the `target` directory. Run the jar file from command line by `java -jar target/jrace-1.0.1.jar -s random/scenario.txt`. Alternatively, one can run by maven as follows: `mvn exec:java -Dexec.args="-s random/scenario.txt"`.

Alternatively, one can also build and run by JDK. 
To compile from source: `javac -d bin -cp lib/slf4j-api-1.7.5.jar:lib/logback-classic-1.0.13.jar:lib/logback-core-1.0.13.jar $(find . -name *.java)`.
Example run: 
`java -cp bin:lib/slf4j-api-1.7.5.jar:lib/logback-classic-1.0.13.jar:lib/logback-core-1.0.13.jar tune.Tuner -s random/scenario.txt`


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

## Configurator setting

Configurator can be specified in the scenario file under the option `tuner`. Currently the following configurators are available: 
* Full configurator that tunes numeric, categorical, and conditional parameters:
  * `irace` as default configurator, iterated racing following the proposal in [1, 2]. 
  * `urace` racing with uniform random sampled configurations [1, 2]. 
* Specific configurator for numeric parameters:
  * `cmaes`, uses CMA-ES to generate candidate configurations [3], and racing based post-selection [4, 2] to select the best.
  * `bobyqa`, BOBYQA with racing based post-selection [3, 4, 2].
  * `bc`, a hybrid of BOBYQA in the first iteration to determine a starting configuration for CMA-ES [2].
  * `simplex`, Nelder-Mead simplex with post-selection [2].

JRace supports two types of racing: 
* Friedman's two way analysis of variance by ranks [1],
* and student's t-test.

## Reference
[1] [M. Birattari, Z. Yuan, P. Balaprakash, and T. Stützle. F-Race and iterated F-Race: An overview. In T. Bartz-Beielstein et al., editors, Experimental Methods for the Analysis of Optimization Algorithms, Natural Computation Series, pages 311-336, Springer, 2010.](http://iridia.ulb.ac.be/IridiaTrSeries/link/IridiaTr2009-018.pdf)

[2] [Z. Yuan, Automated algorithm configuration for hard optimization problems. Ph.D. thesis, IRIDIA, Université Libre de Bruxelles, Belgium](http://iridia.ulb.ac.be/~zyuan/downloads/yuan2019thesis.pdf)

[3] [Z. Yuan, M. Montes de Oca, M. Birattari, and T. Stützle. Continuous optimization algorithms for tuning real and integer  parameters of swarm intelligence algorithms. Swarm Intelligence, 6(1):49-75, 2012.](http://iridia.ulb.ac.be/IridiaTrSeries/IridiaTr2011-017.pdf)

[4] [Z. Yuan, T. Stützle, M. Montes de Oca, H.C. Lau, and M. Birattari. An analysis of post-selection in automatic configuration. In Proc. of the Genetic and Evolutionary Computation Conference (GECCO), pages 1557-1564, Amsterdam, Netherlands, 2013.](http://iridia.ulb.ac.be/~zyuan/downloads/YuanEtal2013ps.pdf)
