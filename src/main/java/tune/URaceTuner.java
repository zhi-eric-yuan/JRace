/*
 * Created by Zhi Yuan
 */
package tune;

import org.apache.commons.math.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import race.Race;
import util.MathHelp;
import util.Randomizer;
import datahandler.OutputHandler;
import eval.AlgorithmEvaluator;
import algo.CategoricalParameter;
import algo.Configuration;
import algo.Instance;
import algo.NumericalParameter;
import algo.Parameter;

/**
 * @author yuan
 * Created on Sep 23, 2013
 *
 */
public class URaceTuner {

	private static Logger log = LoggerFactory.getLogger(URaceTuner.class);
	/**
	 * 
	 */
	public URaceTuner() {
	}

	public Configuration tune(Parameter[] params, int budget,
			Instance[] instances) {
		// default setting in Birattari et al. 2010
		double nr = 6;
		
		int numConfig = (int) (budget / nr);
		
		log.info("{} uniformly randomly sampled configurations: ", numConfig);
		
		Configuration[] configs = uniformRandomSampling(params, numConfig);
		
		AlgorithmEvaluator eval = new AlgorithmEvaluator(configs, instances);
		configs = eval.getConfigurations();
		numConfig = configs.length;
		
		log.info("{} unique configurations", numConfig);
		for (int i = 0; i < configs.length; i++) {
			OutputHandler.writeln(configs[i].toString());
		}
		// TODO should read number of instances from file
		int numInstances = instances.length;
		Race racer = new Race(numConfig, numInstances, eval, budget, Tuner.useFRace);
		int bestIndex = racer.race();
		log.info("Best parameter configuration found: {}", configs[bestIndex].toString());
		return configs[bestIndex];
	}
	
	/**
	 * @param params
	 * @param numConfig
	 * @return
	 */
	public static Configuration[] uniformRandomSampling(Parameter[] params,
			int numConfig) {
		Configuration[] configs = new Configuration[numConfig];
		Parameter param;
		double[] range;
		int dim = params.length;
		boolean isInt;
		Configuration config;
		int index = 0;
		
		if (Tuner.defaultConfig != null) {
			log.info("Adding default configuration: {}", Tuner.defaultConfig);
			configs[0] = Tuner.defaultConfig;
			index++;
		}
		
		for (int i = index; i < numConfig; i++) {
			config = new Configuration(dim);
			for (int j = 0; j < dim; j++) {
				param = params[j];
				if (param.isNumerical()) {
					double value = uniformNumerical(param);
					config.addParam(param, value);
					
				} else if (param.isCategorical()) {
					// TODO also to handle the categorical and conditional parameters.
					CategoricalParameter cat = (CategoricalParameter)param;
					String value = cat.getValue(Randomizer.nextInt(cat.getNumValues()));
					config.addParam(param, value);
				} else {
					log.error("Unrecognized parameter {}", param.toString());
				}
				configs[i] = config;
			}
		}
		return configs;
	}

	/**
	 * @param param
	 * @return
	 */
	public static double uniformNumerical(Parameter param) {
		double[] range;
		boolean isInt;
		double value;
		range = ((NumericalParameter) param).getRange().clone();
		isInt = param.isInteger();
		if (isInt && range[0] == Math.round(range[0]) && range[1] == Math.round(range[1])) {
			// To make the border value equally likely to be sampled 
			// as other values.
			range[0] -= 0.499999;
			range[1] += 0.499999;
		}
		value = Randomizer.nextDouble(range[0], range[1]);
		if (isInt) {
			value = FastMath.round(value);
		}
		value = MathHelp.roundToSignifDigit(value, Tuner.signifDigit);
		//value = MathHelp.roundToDecimalPlace(value, Tuner.signifDigit);
		return value;
	}

}
