/*
 * Created by Zhi Yuan
 */
package org.apache.commons.math.optimization.direct;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math.optimization.ConvergenceChecker;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import algo.Configuration;
import datahandler.OutputHandler;
import eval.AlgorithmEvaluator;
import race.Race;
import tune.Tuner;
import tune.URaceTuner;

/**
 * @author yuan
 * Created on Aug 17, 2012
 *
 */
public class CMAUOptimizer extends CMAESOptimizer {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 
	 */
	public CMAUOptimizer() {
	}

	/**
	 * @param lambda
	 */
	public CMAUOptimizer(int lambda) {
		super(lambda);
	}

	/**
	 * @param lambda
	 * @param inputSigma
	 * @param boundaries
	 */
	public CMAUOptimizer(int lambda, double[] inputSigma, double[][] boundaries) {
		super(lambda, inputSigma, boundaries);
		//this.boundaries = boundaries;
	}

	/**
	 * @param lambda
	 * @param inputSigma
	 * @param boundaries
	 * @param maxIterations
	 * @param stopFitness
	 * @param isActiveCMA
	 * @param diagonalOnly
	 * @param checkFeasableCount
	 * @param random
	 * @param generateStatistics
	 * @param checker
	 */
	public CMAUOptimizer(int lambda, double[] inputSigma,
			double[][] boundaries, int maxIterations, double stopFitness,
			boolean isActiveCMA, int diagonalOnly, int checkFeasableCount,
			RandomGenerator random, boolean generateStatistics,
			ConvergenceChecker<RealPointValuePair> checker) {
		super(lambda, inputSigma, boundaries, maxIterations, stopFitness,
				isActiveCMA, diagonalOnly, checkFeasableCount, random,
				generateStatistics, checker);
	}

	protected RealPointValuePair doOptimize() {
		//double[] startPoint = getStartPoint();
		int dimension = boundaries[0].length;
		//double numUnifFac = SystemProperty.getDouble(SystemProperty.NUM_UNIF_FAC, ct.a);
		//int numUnif = ct.determineLambdaByFactor(numUnifFac);
		//int numUnif = ct.getLambda0();
		
		double[] bestPoint = null;
		ArrayList<RealPointValuePair> bests = null;
		if (! Tuner.hasDefault) {
			int numUnif = ct.getLambda();
			log.info("Uniformly sample {} points", numUnif);
			UniformRandomOptimizer unifOptim = new UniformRandomOptimizer(dimension, boundaries);
			if (! ct.race || ct.numEval <= ct.firstTest) {
				ct.updateQualInstances();
				unifOptim.initBest(ct.numAddEval);
				unifOptim.optimize(numUnif, function, goal, start);
				// Additional evaluation disabled for the first iteration
	//			ArrayList<RealPointValuePair> bests = ct.restartElites.size() == 0? unifOptim.bestPoints :
	//				ct.addEval(unifOptim);
				//ArrayList<RealPointValuePair> bests = unifOptim.bestPoints;
				bests = ct.addEval(unifOptim);
				//bestOverTime.addAll(unifOptim.bestOverTime);
			} else {
				int numInstances = 10;
				ct.updateQualInstances(numInstances, ct.firstTest);
				Configuration[] configs = URaceTuner.uniformRandomSampling(ct.params, numUnif);
				AlgorithmEvaluator evalTmp = new AlgorithmEvaluator(configs, null);
				configs = evalTmp.getConfigurations();
				AlgorithmEvaluator eval = (AlgorithmEvaluator) function;
				eval.setConfigurations(configs);
				int numConfig = configs.length;

				log.info("{} unique uniformly sampled configurations", numConfig);
				for (int i = 0; i < numConfig; i++) {
					OutputHandler.writeln(configs[i].toString());
				}
				// TODO should read number of instances from file
				int budget = numUnif * ct.numEval;
				Race racer = new Race(numConfig, numInstances, eval, budget, Tuner.useFRace, 
						ct.firstTest);
				int bestIndex = racer.race();
				log.info("Best parameter configuration found: {}", configs[bestIndex].toString());
				int[] order = racer.computeCandidateOrders();
				double[] scores = racer.getScores();
				int index;
				bests = new ArrayList<RealPointValuePair>(ct.numAddEval);
				for (int i = 0; i < ct.numAddEval; i++) {
					index = order[i];
					bests.add(new RealPointValuePair(configs[index].scale(), scores[index]));
				}
				
			}
			bestPoint = bests.get(0).getPoint();
		} else {
			log.info("Adopt default configuration {} for CMAES starting point", 
					Tuner.defaultConfig.toString());
			bestPoint = Tuner.defaultConfig.scale();
		}
		restEval = ct.addElite(bestPoint);

		int startLambda = ct.determineStartLambda(restEval);
		
		// if the rest budget less than lambda
		if (startLambda <= 0) {
			this.bestPoints = bests;
			return bests.get(0);
		} else {
			this.lambda = startLambda;
		}

		//CMAESOptimizer optim = new CMAESOptimizer(0, null, boundaries);
		//optim.preIterationCount = 1;
		//optim.earlyQualification = true;
		//optim.ct = ct;
		//RealPointValuePair result = super.optimize(restEval, function, GoalType.MINIMIZE, 
		//		bestPoint);
		start = bestPoint.clone();
		log.info("Starting point for CMAES: {}", Arrays.toString(start));
		RealPointValuePair result = super.doOptimize();
		//modifyBestOverTime(usedEval);
		//addToFrontBestOverTime(unifOptim.bestOverTime);
		
		log.info("Budget left from qualification: {}", restEval);
		//System.out.println("flag");

		// XXX stop here
//		unifOptim.optimize(lambda, func, GoalType.MINIMIZE, startPoint);
//        RealPointValuePair unifResult = unifOptim.optimize(lambda, func, GoalType.MINIMIZE, startPoint);
//		CMAESOptimizer optim = new CMAESOptimizer(0, null, boundaries);
//        RealPointValuePair result = optim.optimize(BUDGET - lambda, func, GoalType.MINIMIZE, startPoint);
		return result;

	}

	private void addToFrontBestOverTime(ArrayList<double[]> list) {
		double[] timeVal;
		for (int i = 0; i < list.size(); i++) {
			timeVal = list.get(i);
			bestOverTime.add(i, timeVal);
		}
	}

	private void modifyBestOverTime(int usedEval) {
		double[] timeVal;
		for (int i = 0; i < bestOverTime.size(); i++) {
			timeVal = bestOverTime.get(i);
			timeVal[0] += usedEval;
//			bestOverTime.add(new double[]{timeVal[0] + usedEval, timeVal[1]});
		}
	}
}
