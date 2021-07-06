/**
 * 
 */
package tune;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math.exception.MathIllegalStateException;
import org.apache.commons.math.exception.TooManyEvaluationsException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.direct.BOBYQAOptimizer;
import org.apache.commons.math.optimization.direct.CMAESOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.MathHelp;
import datahandler.OutputHandler;
import eval.Evaluator;

/**
 * @author yuan
 *
 */
public class BocmaTuner extends CmaesTuner {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	public final double volRate = 0.8;
	public final double minRadius = 0.2;
	protected int numInterpolationPoints;
	protected double endRadius;

	/* (non-Javadoc)
	 * @see tune.PostSelectionTuner#oneQualification(int, int)
	 */
	@Override
	protected void oneQualification(int budget, int iterationCount) {
		double[] bestPoint;
		if (canDoBobyqaIteration(budget)) {
			// run bobyqa
			bestPoint = doBobyqa(budget, iterationCount);
		} else if (optim != null && optim.bestPoints != null && optim.bestPoints.size() > 0) {
			// cannot run bobyqa, take the best of the previous iteration as starting point
			bestPoint = optim.bestPoints.get(0).getPoint();
		} else {
			// no previous best points, take a random point instead
			bestPoint = randomStart(dim);
		}

		doCma(bestPoint);

	}

	/**
	 * @param startPoint
	 */
	protected void doCma(double[] startPoint) {
		lambda = determineStartLambda(restEval);
		if (lambda <= 0) {
			log.error("budget {} is too small to start a new iteration", restEval);
			return;
		}
		optim = new CMAESOptimizer(lambda, null, new double[][]{lowers, uppers});
		optim.initBest(numAddEval);
		optim.earlyQualification = true;
		((CMAESOptimizer) optim).ct = this;
		//((CMAESOptimizer) optim).setMaxIterations(iterationCount + 1);

		log.info("Start CMAES tuner with starting point {}", Arrays.toString(startPoint));

		optim.optimize(restEval, eval, GoalType.MINIMIZE, startPoint.clone());
		
		log.info("Budget left from qualification: {}", restEval);
	}

	protected double[] doBobyqa(int budget, int iterationCount, double[] startPoint) {
		double[] bestPoint;
		double startRadius = computeStartRadius(iterationCount);
		optim = new BOBYQAOptimizer(numInterpolationPoints, startRadius, endRadius);
		optim.initBest(numAddEval);
		log.info("Start BOBYQA tuner with radius {} and start point {}", startRadius, 
				Arrays.toString(startPoint));
		updateQualInstances();
		//System.out.println(optim.getLowerBound());
		try {
			((BOBYQAOptimizer)optim).optimize(budget, eval, 
					GoalType.MINIMIZE, startPoint, lowers, uppers);
		} catch(TooManyEvaluationsException tme) {
			OutputHandler.writeln(tme.getMessage());
		} catch (MathIllegalStateException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
				
		ArrayList<RealPointValuePair> bests = iterationCount == 1? optim.bestPoints : addEval();

		bestPoint = bests.get(0).getPoint();
		//log.info(Arrays.toString(bestPoint));
		restEval = addElite(bestPoint);
		return bestPoint;
	}

	/**
	 * @param budget
	 * @param iterationCount
	 * @return
	 */
	protected double[] doBobyqa(int budget, int iterationCount) {
		double[] start = randomStart();
		return doBobyqa(budget, iterationCount, start);
	}

	/**
	 * @param budget
	 */
	protected boolean canDoBobyqaIteration(int budget) {
		int minBudBbq = new BobyqaTuner<Evaluator>().determineIterationThreshold(dim);
		log.info("budget {} minBud {}", budget, minBudBbq);
		if (budget < minBudBbq) {
			log.warn("Budget {} is too small to start a new Bobyqa iteration ({})", budget, minBudBbq);
			return false;
		}
		return true;
	}

	protected double computeStartRadius(int iterationCount) {
		double dimRate = MathHelp.power(volRate, 1.0 / dim);
		double rate = MathHelp.power(dimRate, iterationCount - 1);
		double radius = Math.max(0.5 * rate, minRadius);
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
		super.initSearch(dim);
		initBobyqa(dim);
	}

}
