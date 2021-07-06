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
import org.apache.commons.math.optimization.direct.NelderMeadSimplex;
import org.apache.commons.math.optimization.direct.SimplexOptimizer;
import org.apache.commons.math.optimization.direct.UniformRandomOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.SystemProperty;
import datahandler.OutputHandler;
import eval.AlgorithmEvaluator;
import eval.Evaluator;

/**
 * @author yuan
 *
 */
public class UbcTuner extends BocmaTuner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private boolean useSimplex = false;
	protected static final double d = SystemProperty.getDouble(SystemProperty.D, 1.0);

	/**
	 * 
	 */
	public UbcTuner() {
		// TODO Auto-generated constructor stub
	}
	
	public UbcTuner(boolean useSimplex) {
		this.useSimplex  = useSimplex;
	}

	/* (non-Javadoc)
	 * @see tune.PostSelectionTuner#oneQualification(int, int)
	 */
	@Override
	protected void oneQualification(int budget, int iterationCount) {
		if (budget < minBud) {
			log.error("budget {} is too small to start a new iteration");
			return;
		}
		log.info("Iteration {} with budget {}", iterationCount, budget);
		if (iterationCount == 1) {
			super.oneQualification(budget, iterationCount);
		} else {
			double[] bestPoint;
			if (useSimplex) {
				bestPoint = doSimplex(budget, iterationCount);
			} else {
				bestPoint = doUniform(budget);
			}
			
			if (canDoBobyqaIteration(restEval)) {
				bestPoint = doBobyqa(budget, iterationCount, bestPoint);
			}

			if (restEval < minBud) {
				return;
			}
			
			doCma(bestPoint);

		}
	}

	/**
	 * @param budget
	 * @return
	 */
	protected double[] doSimplex(int budget, int iterationCount) {
		SimplexTuner<AlgorithmEvaluator> st = new SimplexTuner<AlgorithmEvaluator>();
		double[] bestPoint;
		if (canDoSimplexIteration(budget, st)) {
			updateQualInstances();
			optim = st.doSimplex(budget, randomStart(lowers.length), numAddEval, lowers, uppers, eval);
			// To add eval or not
			if (optim.getEvaluations() >= budget) {
				
			}
			ArrayList<RealPointValuePair> bests = addEval();
			//ArrayList<RealPointValuePair> bests = optim.bestPoints;
			bestPoint = bests.get(0).getPoint();
			//log.info(Arrays.toString(bestPoint));
			restEval = addElite(bestPoint);
		} else if (optim != null && optim.bestPoints != null && optim.bestPoints.size() > 0) {
			// cannot run simplex, take the best of the previous iteration as starting point
			bestPoint = optim.bestPoints.get(0).getPoint();
		} else {
			// no previous best points, take a random point instead
			bestPoint = randomStart(dim);
		}
		return bestPoint;
	}

	/**
	 * @param budget
	 */
	protected boolean canDoSimplexIteration(int budget, SimplexTuner<AlgorithmEvaluator> st) {
		int minBudSim = st.determineIterationThreshold(dim);
		log.info("budget {} minBud {}", budget, minBudSim);
		if (budget < minBudSim) {
			log.warn("Budget {} is too small to start a new Simplex iteration ({})", budget, minBudSim);
			return false;
		}
		return true;
	}


	/**
	 * @param budget
	 * @return
	 */
	protected double[] doUniform(int budget) {
		log.info("Uniformly sample {} points", lambda);
		updateQualInstances();
		randomStart(lowers.length);
		optim = new UniformRandomOptimizer(dim, new double[][]{lowers, uppers});
		optim.initBest(numAddEval);
		optim.optimize(Math.min(lambda, budget), eval, GoalType.MINIMIZE, starts);
		// To add eval or not
		ArrayList<RealPointValuePair> bests = addEval();
		//ArrayList<RealPointValuePair> bests = optim.bestPoints;
		double[] bestPoint = bests.get(0).getPoint();
		//log.info(Arrays.toString(bestPoint));
		restEval = addElite(bestPoint);
		return bestPoint;
	}

	protected double computeStartRadius(int iterationCount) {
		if (iterationCount == 1) {
			return 0.5;
		} else {
			return 0.2;
		}
	}

}
