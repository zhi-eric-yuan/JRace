/*
 * Created by Zhi Yuan
 */
package tune;

import java.util.Arrays;

import org.apache.commons.math.exception.MathIllegalStateException;
import org.apache.commons.math.exception.TooManyEvaluationsException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.direct.BOBYQAOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.MathHelp;
import util.SystemProperty;
import datahandler.OutputHandler;

/**
 * @author yuan
 * Created on Jul 26, 2013
 *
 */
public class BobyqaTuner<Evaluator> extends PostSelectionTuner {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	protected int bobyqaRestartType = SystemProperty.getInteger(SystemProperty.BOBYQA_RESTART_TYPE, 1);
	private int numInterpolationPoints;
	private double endRadius;
	public final double volRate = 0.8;
	public final double minRadius = 0.2;
	
	/**
	 * 
	 */
	public BobyqaTuner() {}

	public void oneQualification(int budget) {
		oneQualification(budget, 1);
	}

	/**
	 * 
	 */
	protected void oneQualification(int budget, int iterationCount) {
		double startRadius = computeStartRadius(iterationCount);
		getIterationStartPoint(iterationCount);
		optim = new BOBYQAOptimizer(numInterpolationPoints, startRadius, endRadius);
		optim.initBest(numAddEval);
		//starts = new double[]{0.5, 1.0};
		updateQualInstances();
        //System.out.println(optim.getLowerBound());
		try {
			((BOBYQAOptimizer)optim).optimize(budget, eval, GoalType.MINIMIZE, 
					starts, lowers, uppers);
		} catch(TooManyEvaluationsException tme) {
			OutputHandler.writeln(tme.getMessage());
		} catch (MathIllegalStateException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		// XXX Best points over time?
		OutputHandler.printArrayList(optim.bestOverTime);
		//OutputHandler.writeln(Arrays.toString(optim.getCurrentBest().getDataRef()));
		OutputHandler.writeln(Arrays.toString(optim.bestPoints.get(0).getPointRef()));
		OutputHandler.writeln(optim.getEvaluations());
		//if (iterationCount == 1 && (! (this instanceof BsTuner) || ((BsTuner) this).firstSave)) {
		//if (iterationCount == 1 || (this instanceof BsTuner && ((BsTuner) this).firstSave)) {
		if (this instanceof BsTuner && ((BsTuner) this).firstSave) {
			addElite();
		} else if (addEvalHistBest) {
			addElites();
		} else {
			addEliteWithSecondEval();
		}
		
	}
/*
	protected double computeStartRadius(int iterationCount) {
		double dimRate = MathHelp.power(volRate, 1.0 / dim);
		double rate = MathHelp.power(dimRate, iterationCount - 1);
		double radius = Math.max(0.5 * rate, minRadius);
		log.info("Start BOBYQA tuner with radius {}", radius);

		return radius;
	}
*/

	/**
	 * 
	 */
	protected void getIterationStartPoint(int iterationCount) {
		if (bobyqaRestartType == 3 && iterationCount % 2 == 0) {
			starts = optim.bestPoints.get(0).getPoint();
			log.info("Starting from best point of last iteration: {}", Arrays.toString(starts));
		} else {
			randomStart(dim);
			log.info("Starting from a random point: {}", Arrays.toString(starts));
		}

	}

	protected double computeStartRadius(int iterationCount) {
		double radius;
		if (iterationCount == 1) {
			radius = 0.5;
		} else if (bobyqaRestartType == 1 || (bobyqaRestartType == 3 && iterationCount % 2 == 0)) {
			radius = minRadius;
		} else {
			double dimRate = MathHelp.power(volRate, 1.0 / dim); 
			double multiple = iterationCount - 1;
			if (bobyqaRestartType == 3) {
				multiple /= 2;
			}
			double rate = MathHelp.power(dimRate, multiple);
			radius = Math.max(0.5 * rate, minRadius);
		}
			
		log.info("Start BOBYQA tuner with radius {}", radius);
		return radius;
	}

	/**
	 * @param dim
	 */
	protected void initBobyqa(int dim) {
		numInterpolationPoints = 2 * dim + 1;
		endRadius = MathHelp.power(0.1, Tuner.signifDigit);
	}

	@Override
	protected void initSearch(int dim) {
		initBobyqa(dim);
		initBobyqaRestart();
	}
	protected void initBobyqaRestart() {
		switch (bobyqaRestartType) {
		case 1:
			break;
		case 2: 
			break;
		case 3: 
			break;
		default: 
			log.error("Invalid BOBYQA restart type: {}. Use type 1 as default", 
					bobyqaRestartType);
			bobyqaRestartType = 1;
		}
		log.info("Bobyqa restart type: {}", bobyqaRestartType);
	}

	protected int determineMinNewCan() {
		return 2 * dim + 2;
	}
	
	protected int determineNumAddEval() {
		int defaultNumAddEval = 2;
		numAddEval = SystemProperty.getInteger(SystemProperty.S_NUM_ADD_EVAL, defaultNumAddEval);
		if (numAddEval == 0) {
			numAddEval = dim;
		} else if (numAddEval < 0) {
			log.error("Invalid number of additional evaluated candidates: {}", numAddEval);
			numAddEval = defaultNumAddEval;
		}
		log.info("Number of additionally evaluated candidates: {}", numAddEval);
		return numAddEval;
	}
	
	public int determineIterationThreshold(int dim) {
		this.dim = dim;
		int threshold = determineMinNewCan() * numEval;
		/*if (addEvalHistBest) {
			threshold += 2;
		}*/
		//threshold += determineNumAddEval();
		log.info("Min budget to start the next Bobyqa iteration: {}", threshold);

		return threshold;
	}
 
	
}
