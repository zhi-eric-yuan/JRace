/*
 * Created by Zhi Yuan
 */
package tune;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.math.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import race.Race;
import util.MathHelp;
import util.Randomizer;
import util.SystemProperty;
import algo.CategoricalParameter;
import algo.Configuration;
import algo.Instance;
import algo.NumericalParameter;
import algo.Parameter;
import datahandler.OutputHandler;
import eval.AlgorithmEvaluator;

/**
 * @author yuan
 * Created on Sep 23, 2013
 *
 */
public class IRaceTuner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private static final int MAX_ITERATION = 10;
	AlgorithmEvaluator eval;
	private double smoothFactor = 0.9;
	protected static final int muInc = (int) Math.round(SystemProperty.getDouble(
			SystemProperty.MU_INC, 1));

	/**
	 * 
	 */
	public IRaceTuner() {
	}

	public Configuration tune(Parameter[] params, int budget,
			Instance[] instances) {
		int numParams = params.length;
		int numIterations = computeNumIterations(numParams);
		int iteration = 1;
		int mu = computeMu(iteration);
		int iterationBudget = computeIterationBudget(budget, numIterations, iteration);
		int numCandidates = computeNumCandidates(iterationBudget, mu);

		log.info("number of parameters: {}", numParams);
		log.info("number of iterations: {}", numIterations);
		log.info("Tuning budget: {}", budget);
		log.info("number of candidates of iteration {}: {}", iteration, numCandidates);

		Configuration[] configs = URaceTuner.uniformRandomSampling(params, numCandidates);
		if (Tuner.haveCategorical) {
			initCatProb(configs);
		}
		configs = uniqueConfigurations(configs);

		int survivalQuota = computeSurvivalQuota(numParams);
		int eliteQuota = survivalQuota;
		int numInstances = instances.length;
		int usedExp = 0;
		int iterationUsedExp = 0;
		int budgetLeft = budget;
		Race racer;
		Configuration[] elites;
		int bestIndex = -1;

		while (true) {
			eval = new AlgorithmEvaluator(configs, instances);
			numCandidates = configs.length;
			log.info("Iteration {} with budget: {}", iteration, iterationBudget);
			for (int i = 0; i < configs.length; i++) {
				OutputHandler.writeln(configs[i].toString());
			}
			
			racer = new Race(numCandidates, numInstances, eval, iterationBudget, 
					Tuner.useFRace, 5, survivalQuota);
			bestIndex = racer.race();
			log.info("Best parameter configuration found: {}", configs[bestIndex].toString());
			iterationUsedExp = racer.getNumExp();
			usedExp += iterationUsedExp;
			budgetLeft -= iterationUsedExp;
			log.info("Experiments used in iteration {}: {}; in total {} / {}, rest {}", 
					iteration, iterationUsedExp, usedExp, budget, budgetLeft);
			
			if (iteration == numIterations) {
				break;
			} else if (iteration == numIterations - 1) {
				survivalQuota = 1;
			}
			
			int numAlive = racer.getNumAlive();
			int numElites = Math.min(numAlive, eliteQuota);
			elites = getEliteConfigurations(racer, numElites, configs);
			log.info("{} elites:", numElites);
			for (int i = 0; i < elites.length; i++) {
				OutputHandler.writeln(elites[i].toString());
			}

			double [] eliteWeights = computeEliteWeights(numElites);
			
			iteration++;
			mu = computeMu(iteration);
			iterationBudget = computeIterationBudget(budgetLeft, numIterations, iteration);
			numCandidates = computeNumCandidates(iterationBudget, mu);
		    log.info("iteration = {}, mu = {}, iteration budget = {}, num. candidates = {}", 
		    		iteration,  mu, iterationBudget, numCandidates);
			if (iteration == numIterations && Tuner.defaultConfig != null) {
				configs = sampleConfigurationsNormal(elites, eliteWeights, numCandidates - 1, 
						numElites, iteration - 1, params, numIterations);
				configs = addDefaultToConfigs(configs);
			} else {
				configs = sampleConfigurationsNormal(elites, eliteWeights, numCandidates, 
						numElites, iteration - 1, params, numIterations);
			}

		}
		log.info("IRace ends. Best configuration found: {}", 
				configs[bestIndex].toString());

		return configs[bestIndex];
	}

	private Configuration[] nullifyConditionals(Configuration[] configs) {
		for (int i = 0; i < configs.length; i++) {
			configs[i].nullifyConditional();
		}
		return configs;
	}

	private void initCatProb(Configuration[] configurations) {
		
		for (int i = 0; i < configurations.length; i++) {
			configurations[i].initCatProbMap();
		}
	}

	private Configuration[] sampleConfigurationsNormal(Configuration[] elites,
			double[] eliteWeights, int numCandidates, int numElites, int iteration, 
			Parameter[] params, int numIterations) {
		int numSamples = numCandidates - numElites;
		if (numSamples <= 0) {
			log.info("To sample {}, but we have already {} elites: no sampling needed!", 
			    		numCandidates, numElites);
			return (elites);
		}
		log.info("Start sampling {} candidates by normal distribution ({} elites remain).", 
				numSamples, numElites);
		int dim = Configuration.dim;
		int[] sampledCounts = Randomizer.generateCountsByProb(numSamples, eliteWeights);
		log.info("Number of samples for each elite: ");
		OutputHandler.writeArray(sampledCounts);
		double numRate = -1;
		double catRate = -1;
		if (Tuner.haveNumerical) {
			// rate in numerical defines the amount of half-range as standard deviation
			numRate = computeNumRate(numCandidates, iteration, dim);
			log.info("numerical rate {}", numRate);
		}
		if (Tuner.haveCategorical) {
			// rate in categorical defines the amount being "evaporated"
			catRate = 1.0 * iteration / numIterations;
			log.info("categorical rate {}", catRate);
		}
		Configuration elite;
		Configuration candi = null;
		int count;
		Parameter param;
		ArrayList<Configuration> newGen = new ArrayList<Configuration>(numCandidates);
		newGen.addAll(Arrays.asList(elites));
		int sumCounts = numElites;
		int iterationCount;
		double[] probVector;
		HashMap<CategoricalParameter, double[]> probMap = null;
		double smoothPower;
		
		for (int i = 0; i < numElites; i++) {
			elite = elites[i];
			count = sampledCounts[i];
			sumCounts += count;
			iterationCount = 0;
			smoothPower = 0;
			
			while (true) {
			for (int j = 0; j < count; j++) {
				candi = new Configuration(dim);
				if (Tuner.haveCategorical) {
					probMap = new HashMap<CategoricalParameter, double[]>(dim);
				}
				for (int k = 0; k < dim; k++) {
					param = elite.getParam(k);
					if (param.isNumerical()) {
						Object value = elite.getValue(k);
						if (value == null) {
							value = checkEliteValue(elites, param);
						}
						
						normalNumParameter(value, candi, (NumericalParameter)param, numRate, 
								smoothPower);
						
					} else if (param.isCategorical()) {
						// sample categorical parameters by normal distribution 
						CategoricalParameter catParam = (CategoricalParameter) param;
						Object value = elite.getValue(k);
						probVector = elite.getProbVectorByParam(catParam);
						if (probVector == null) {
							value = checkEliteValue(elites, catParam);
							probVector = checkEliteProbVector(elites, catParam);
						}
						OutputHandler.writeArray(probVector);
						probVector = updateProbVector(probVector, catParam, value, catRate, 
								smoothPower);
						OutputHandler.writeArray(probVector);
						normalCatParameter(catParam, probVector, candi);
						probMap.put(catParam, probVector);
					} else {
						log.error("Unknown parameter type {}", param);
					}
				}
				newGen.add(candi);
			}
			newGen = uniqueConfigurations(newGen);
			count = sumCounts - newGen.size();
			if (count <= 0) {
				elite.setProbMap(probMap);
				break;
			} else if (iterationCount++ < MAX_ITERATION) {
				continue;
			} else {
				// max number of tries exceeded, use smoothing procedure.
				log.info("max number of tries {} exceeded, use smoothing procedure.",
						MAX_ITERATION);
				//newGen = sampleRestUniform(params, count, newGen, sumCounts);
				//break;
				smoothPower++;
				log.info("smoothPower {}", smoothPower);
				iterationCount = 0;
				continue;
			}
			
			}
		}
		return newGen.toArray(new Configuration[newGen.size()]);
	}

	private Configuration[] addDefaultToConfigs(Configuration[] configs) {
		log.info("Adding default configuration: {}", Tuner.defaultConfig);
		int numConfigs = configs.length;
		Configuration[] newConfs = new Configuration[numConfigs + 1];
		newConfs[0] = Tuner.defaultConfig;
		for (int i = 0; i < numConfigs; i++) {
			newConfs[i + 1] = configs[i];
		}
		newConfs = uniqueConfigurations(newConfs);
		return newConfs;
	}

	private Object checkEliteValue(Configuration[] elites, Parameter param) {
		Object value;
		for (int i = 0; i < elites.length; i++) {
			value = elites[i].getParamValue(param);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private double[] checkEliteProbVector(Configuration[] elites, 
			CategoricalParameter param) {
		double[] prob;
		for (int i = 0; i < elites.length; i++) {
			prob = elites[i].getProbVectorByParam(param);
			if (prob != null) {
				return prob;
			}
		}
		return null;
	}

	/**
	 * @param params
	 * @param count
	 * @param newGen
	 * @param sumCounts
	 * @return
	 */
	private ArrayList<Configuration> sampleRestUniform(Parameter[] params,
			int count, ArrayList<Configuration> newGen, int sumCounts) {
		Configuration[] candis;
		do {
			candis = URaceTuner.uniformRandomSampling(params, count);
			newGen.addAll(Arrays.asList(candis));
			newGen = uniqueConfigurations(newGen);
			count = sumCounts - newGen.size();
		} while (count > 0);
		return newGen;
	}
	
	private void normalCatParameter(CategoricalParameter param, double[] probVector,
			Configuration candi) {
		int index = Randomizer.generateIndexByProb(probVector);
		log.info("chosen index {}", index);
		candi.addParam(param, param.getValue(index), probVector);
	}

	private void smoothProbVector(double[] probVector, double rate) {
		int numLevels = probVector.length;
		double addition = (1 - rate) / numLevels;
		for (int i = 0; i < numLevels; i++) {
			probVector[i] *= rate;
			probVector[i] += addition;
		}
	}

	private double[] updateProbVector(final double[] probVector, 
			CategoricalParameter catParam, Object value, double rate, double smoothPower) {
		int numLevels = catParam.getNumValues();
		double[] updated = new double[numLevels];
		if (probVector == null || value == null) {
			double probValue = 1.0 / numLevels;
			for (int i = 0; i < numLevels; i++) {
				updated[i] = probValue;
			}
		} else {
			String strValue = (String) value;
			int catIndex = catParam.indexOfValue(strValue);
			log.info("rate {} param {} value {} elite index {}", rate, catParam, 
					strValue, catIndex);
			for (int i = 0; i < numLevels; i++) {
				updated[i] = probVector[i] * (1 - rate);
				if (i == catIndex) {
					updated[i] += rate;
				}
			}
			
			if (smoothPower > 0) {
				double smoothRate = MathHelp.power(smoothFactor, smoothPower);
				log.info("smoothRate {}", smoothRate);
				smoothProbVector(probVector, smoothRate);
			}
		}
		return updated;
	}

	private double computeNumRate(int numCandidates, int iteration, int dim) {
		log.info("numCandidates {} iteration {} dim {}", numCandidates, iteration, dim);
		return MathHelp.power(numCandidates, -1.0 * iteration / dim);
	}

	private void normalNumParameter(Object value, Configuration candi, 
			NumericalParameter param, double rate, double smoothPower) {
		double sampled;
		log.info("Numerical parameter {} rate {} value {}, smooth power {}", param, rate, 
				value, smoothPower);
		if (value == null) {
			sampled = URaceTuner.uniformNumerical(param);
		} else {
			double[] boundary;
			double range;
			double sd;
			boundary = param.getRange();
			range = boundary[1] - boundary[0];
			sd = range * rate / 2;
			if (smoothPower > 0) {
				double smoothRate = MathHelp.power(smoothFactor, smoothPower);
				log.info("Parameter {} smoothRate {}", param, smoothRate);
				sd /= smoothRate;
			}
			log.info("Numerical parameter {} boundary [{}, {}] range {} sample mean {} sd {}", 
					param, boundary[0], boundary[1], range, value, sd);
			
			Double mean = Double.valueOf(value.toString());

			do {
				sampled = Randomizer.nextGaussian(mean, sd);
				sampled = MathHelp.roundToSignifDigit(sampled, Tuner.signifDigit);
				//sampled = MathHelp.roundToDecimalPlace(sampled, Tuner.signifDigit);
			} while (sampled < boundary[0] || sampled > boundary[1]);
			
			if (param.isInteger()) {
				sampled = Math.round(sampled);
			}
		}
		log.info("Sampled {}", sampled);
		candi.addParam(param, sampled);
	}

	private ArrayList<Configuration> uniqueConfigurations(
			ArrayList<Configuration> configurations) {
		Configuration[] configs = new Configuration[configurations.size()];
		configurations.toArray(configs);
		configs = uniqueConfigurations(configs);
		configurations = new ArrayList<Configuration>(Arrays.asList(configs));

		return configurations;
	}

	private Configuration[] uniqueConfigurations(Configuration[] configurations) {
		if (Tuner.haveConditional) {
			configurations = nullifyConditionals(configurations);
		}
		AlgorithmEvaluator eval = new AlgorithmEvaluator(configurations, null);
		configurations = eval.getConfigurations();
		log.info("{} unique configurations", configurations.length);

		return configurations;
	}

	private Configuration[] getEliteConfigurations(Race racer, int numElites, 
			Configuration[] configurations) {
		Configuration[] elites = new Configuration[numElites];
		int [] eliteIndices = racer.getElitesInOrder(numElites);
		for (int i = 0; i < numElites; i++) {
			elites[i] = configurations[eliteIndices[i]];
		}

		return elites;
	}

	/**
	 * Compute the weights for each elite configurations to be sampled.
	 * @param numElites number of elites
	 * @return the probability vector
	 */
	private double[] computeEliteWeights(int numElites) {
		double[] weights = new double[numElites];
		double sumProbs = numElites * (numElites + 1) / 2;
		for (int i = 0; i < numElites; i++) {
			weights[i] = (numElites - i) / sumProbs;
		}
		return weights;
	}

	private int computeSurvivalQuota(int numParams) {
		return 2 + (int) Math.round(FastMath.log(2, numParams));
	}

	private int computeNumCandidates(int iterationBudget, int mu) {
		return iterationBudget / mu;
	}

	private int computeIterationBudget(int budget, int numIterations,
			int iteration) {
		return budget / (numIterations - iteration + 1);
	}

	/**
	 * Number of iterations.
	 * @param numParams number of parameter to tune.
	 */
	private int computeNumIterations(int numParams) {
		return 2 + (int) Math.round(FastMath.log(2, numParams));
	}

	/**
	 * mu * number of candidates = budget for one F-Race execution.
	 * mu is the candidate-evaluation trade-off factor, with value 6,  7,  ...
	 * @param iteration iteration counter
	 * @return mu
	 */
	private int computeMu(int iteration) {
		//return 5 + iteration;
		return 6 + (iteration - 1) * muInc;
	}

	
}
