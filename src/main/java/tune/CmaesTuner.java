/*
 * Created by Zhi Yuan
 */
package tune;

import java.util.Arrays;

import org.apache.commons.math.exception.MathIllegalStateException;
import org.apache.commons.math.exception.TooManyEvaluationsException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.direct.BOBYQAOptimizer;
import org.apache.commons.math.optimization.direct.CMAUOptimizer;
import org.apache.commons.math.optimization.direct.UniformRandomOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import algo.Instance;
import util.MathHelp;
import util.Randomizer;
import util.SystemProperty;
import datahandler.OutputHandler;

/**
 * @author yuan
 * Created on Nov 1, 2013
 *
 */
public class CmaesTuner extends PostSelectionTuner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * IPOP factor
	 */
	public static double d = SystemProperty.getDouble(SystemProperty.D, 1.0);
	public static final int popLevel = (int) SystemProperty.getDouble(
			SystemProperty.POP_LEVEL, 1);
	protected static final int cNumAddEval = (int) SystemProperty.getDouble(
			SystemProperty.C_NUM_ADD_EVAL, 2);
	public boolean race = SystemProperty.getBoolean(SystemProperty.CRACE, false);
	public boolean adaptMu = SystemProperty.getBoolean(SystemProperty.ADAPT_MU, false);
	public int firstTest = SystemProperty.getInteger(SystemProperty.QUAL_FIRST_TEST, 2);
	public double maxLambda;
	public final int maxResample = 10;
	public boolean isIncrease = true;
	public double lambdaFactor;
	/**
	 * 
	 */
	public CmaesTuner() {
	}

	public CmaesTuner(boolean race) {
		this.race = race;
	}

	protected void oneQualification(int budget, int iterationCount) {
		lambda = determineStartLambda(budget * numEval);
		if (lambda <= 0) {
			log.error("budget {} is too small to start a new iteration");
			return;
		}
		log.info("CMAES tuner starts with race {} adapt mu {} first test {} numEval {}", race, 
				adaptMu, firstTest, numEval);
		optim = new CMAUOptimizer(lambda, null, new double[][]{lowers, uppers});
		optim.initBest(numAddEval);
		optim.earlyQualification = true;
		((CMAUOptimizer) optim).ct = this;
		randomStart(lowers.length);
		try {
			((CMAUOptimizer)optim).optimize(budget, eval, GoalType.MINIMIZE, starts);
		} catch(TooManyEvaluationsException tme) {
			OutputHandler.writeln(tme.getMessage());
		} catch (MathIllegalStateException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		//OutputHandler.printArrayList(optim.bestOverTime);
		//OutputHandler.writeln(Arrays.toString(optim.getCurrentBest().getDataRef()));
		//log.info(Arrays.toString(optim.bestPoint));
		log.info("Budget used: {}", Tuner.arch.getSize());
	}

	public int determineLambdaByFactor() {
		lambda = determineLambdaByFactor(lambdaFactor);
		return determineStartLambda(restEval);
	}


	public int determineLambda(int iterationCount) {
		if (d > 1) {
			lambdaFactor = a;
			//lambda = 4 + (int) (12. * Math.log(dim));
		} else {
			lambdaFactor = a / MathHelp.power(d, popLevel - 1);
			//lambda = 4 + (int) (24. * Math.log(dim));
			//lambda = 4 + (int) (a * d * d * Math.log(dim));
		}
		lambda = determineLambdaByFactor();
		log.info("IPOP factor d {}, iteration {}, pop level {}, population size lambda {}, factor {}", 
				d, iterationCount, popLevel, lambda, lambdaFactor);
		return lambda;
	}

	@Override
	protected void initSearch(int dim) {
		maxLambda = Math.min(10 * dim * dim, 6 + 32 * Math.log(dim));
	}

	@Override
	protected int determineMinNewCan() {
		//return getLambda();
		return lambda0;
	}
	
	/**
	 * Procedure after one qualification iteration
	 * @param minBud minimum budget
	 * @param iterationCount iteration counter (after +1)
	 * @return minimum budget
	 */
	protected int determineMinBud(int iterationCount) {
		minBud = super.determineMinBud(iterationCount);
		determineLambda(iterationCount);
		minNewCan = determineMinNewCan();
		minBud = minNewCan * numEval;
		//if (addEvalHistBest) {
		//minBud += numAddEval;
		//}
		return minBud;
	}
	
	public int determineStartLambda(int restEval) {
		int threshold = determineIterationThreshold();
		if (restEval >= threshold) {
			return getLambda();
		} else {
			log.info("The evaluations left {} is smaller than CMAES min budget ({}).", 
					restEval, threshold);
			int lambdaAdapted = restEval / numEval;
			if (lambdaAdapted >= lambda0) {
				log.info("Set starting lambda to {}", lambdaAdapted);
				return lambdaAdapted;
			} else {
				log.info("Terminate.");
				return -1; 
			}

		}
		
	}
	
	public int determineIterationThreshold() {
		int threshold = getLambda() * numEval;
		//int threshold = getLambda();
		/*if (addEvalHistBest) {
			threshold += 2;
		}*/
		//threshold += numAddEval;
		log.info("Min budget to start the next CMAES iteration: {}", threshold);

		return threshold;
	}

	@Override
	protected int determineNumAddEval() {
		log.info("The number of candidates for second evaluation: {}", cNumAddEval);
		return cNumAddEval;
	}

	public void updateQualInstances() {
		if (! race || numEval <= firstTest) {
			updateQualInstances(numEval);
		} else {
			int numInstances;
			boolean[] instanceRemoved = new boolean[10];
			if (instanceIndex < 10) {
				numInstances = 10 + firstTest - Math.min(firstTest, 10 - instanceIndex);
			} else {
				numInstances = 10 + firstTest;
			}
			log.info("Total {} instances this iteration", numInstances);
			Instance[] iterInstances = new Instance[numInstances];
			for (int i = 0; i < firstTest; i++) {
				iterInstances[i] = instances[instanceIndex + i];
				if (instanceIndex + i < 10) {
					instanceRemoved[instanceIndex + i] = true;
				}
			}
			int[] order = Randomizer.generateRandomSequence(0, 9);
			int index;
			for (int i = 0, count = firstTest; i < 10; i++) {
				index = order[i];
				if (! instanceRemoved[index]) {
					iterInstances[count] = instances[index];
					count++;
				}
			}
			//System.out.println(Arrays.toString(iterInstances));
			eval.setInstances(iterInstances);
			instanceIndex += firstTest;
		}
	}

}
