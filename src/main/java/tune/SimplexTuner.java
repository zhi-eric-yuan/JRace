/*
 * Created by Zhi Yuan
 */
package tune;

import java.util.Arrays;

import org.apache.commons.math.analysis.MultivariateFunction;
import org.apache.commons.math.exception.MathIllegalStateException;
import org.apache.commons.math.exception.TooManyEvaluationsException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.direct.BaseAbstractScalarOptimizer;
import org.apache.commons.math.optimization.direct.NelderMeadSimplex;
import org.apache.commons.math.optimization.direct.SimplexOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.SystemProperty;
import datahandler.OutputHandler;
import eval.AlgorithmEvaluator;

/**
 * @author yuan
 * Created on Jul 26, 2013
 *
 */
public class SimplexTuner<Evaluator> extends PostSelectionTuner {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private double[] steps;
	protected boolean simplexEarly = SystemProperty.getBoolean(SystemProperty.SIMPLEX_EARLY, false);
	protected int sMax = SystemProperty.getInteger(SystemProperty.S_MAX, 0);

	/**
	 * 
	 */
	public SimplexTuner() {}

	public void oneQualification(int budget) {
		oneQualification(budget, 1);
	}

	/**
	 * 
	 */
	protected void oneQualification(int budget, int iterationCount) {
		updateQualInstances(numEval);
		int budgetSim = simplexEarly? determineSimplexBudget(budget) : budget;
		doSimplex(budgetSim);
		//OutputHandler.printArrayList(optim.bestOverTime);
		//OutputHandler.writeln(optim.historicalBest.size());
		//OutputHandler.writeln(Arrays.toString(optim.getCurrentBest().getDataRef()));
		
		//OutputHandler.writeln(Arrays.toString(optim.bestPoint));
		//OutputHandler.writeln(optim.getEvaluations());
//		log.info("{} {}", ((SimplexOptimizer)optim).simplex.getPoint(0).getValue(), 
//				Arrays.toString(eval.toParamValues(((SimplexOptimizer)optim).simplex.getPoint(0).getPointRef())));
//		log.info("{} {}", ((SimplexOptimizer)optim).simplex.getPoint(1).getValue(), 
//				Arrays.toString(eval.toParamValues(((SimplexOptimizer)optim).simplex.getPoint(1).getPointRef())));
//		log.info("{} {}", ((SimplexOptimizer)optim).simplex.getPoint(2).getValue(), 
//				Arrays.toString(eval.toParamValues(((SimplexOptimizer)optim).simplex.getPoint(2).getPointRef())));
		OutputHandler.writeOut();
		if (addEvalHistBest) {
			addElites();
		} else {
			addEliteWithSecondEval();
		}
		
	}

	private int getSimCan() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param budget
	 */
	protected BaseAbstractScalarOptimizer<MultivariateFunction> doSimplex(int budget) {
		randomStart(lowers.length);
		return doSimplex(budget, starts, numAddEval, lowers, uppers, eval);
	}

	/**
	 * @param budget
	 */
	protected BaseAbstractScalarOptimizer<MultivariateFunction> doSimplex(int budget, 
			double[] startPoint, int numAddEval, double[] lowers, double[] uppers, 
			AlgorithmEvaluator eval) {
		log.info("Starting point in Simplex: {}", Arrays.toString(startPoint));
		optim = new SimplexOptimizer(1e-16, 1e-16);
		optim.initBest(numAddEval);
		steps = computeSteps(startPoint, new double[][]{lowers, uppers});
		((SimplexOptimizer)optim).setSimplex(new NelderMeadSimplex(steps));
        //System.out.println(optim.getLowerBound());
		return doSimplex(optim, budget, eval, startPoint);
	}

	/**
	 * @param budget
	 * @param startPoint
	 * @param eval
	 * @return
	 */
	protected BaseAbstractScalarOptimizer<MultivariateFunction> doSimplex(
			BaseAbstractScalarOptimizer<MultivariateFunction> optim, 
			int budget, AlgorithmEvaluator eval, double[] startPoint) {
		try {
			optim.optimize(budget, eval, GoalType.MINIMIZE, startPoint);
		} catch(TooManyEvaluationsException tme) {
			OutputHandler.writeln(tme.getMessage());
		} catch (MathIllegalStateException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return optim;
	}

	private static double[] computeSteps(double[] startPoint, double[][] boundaries) {
		int n = startPoint.length;
		double low;
		double up;
		double midRange;
		double midPoint;
		double[] steps = new double[n];
		
		for (int i = 0 ; i < n ; i++) {
			low = boundaries[0][i];
			up = boundaries[1][i];
			midRange = (up - low) / 2;
			midPoint = (up + low) / 2;
			if (startPoint[i] < midPoint) {
				steps[i] = midRange;
			} else {
				steps[i] = -midRange;
			}
		}
		return steps;
	}


	@Override
	protected void initSearch(int dim) {
	}

	/**
	 * Set minimum budget for start a new qualification iteration. 
	 * It is set to dimension + 2 for Simplex, and add more for additional evaluations 
	 */
	protected int determineMinNewCan() {
		return dim + 2;
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

	protected int determineNumElitePerQual(int iterationCount) {
		int num;
		if (! qualifyMultiElites || iterationCount <= 1) {
		//if (iterationCount <= 1) {
			num = 1;
		} else {
			num = useFloor? dim / 2 : (int) Math.ceil(dim / 2.);
		}
		log.info("{} elites to be qualified", num);
		return num;
	}

	public int determineIterationThreshold(int dim) {
		this.dim = dim;
		int threshold = determineMinNewCan() * numEval;
		log.info("Min budget to start the next Simplex iteration: {}", threshold);

		return threshold;
	}
	
	/**
	 * The simplex budget is determined by lambda with a=24 in superclass if sMax is not specified; 
	 * otherwise, it will be set to sMax + dim + 1. 
	 */
	public int determineSimplexBudget(int budget) {
		if (sMax == 0) {
			return super.determineSimplexBudget(budget);
		} else {
			return super.determineSimplexBudget(budget, sMax + dim + 1);
		}
	}
}
