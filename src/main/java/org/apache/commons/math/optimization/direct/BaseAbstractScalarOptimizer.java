/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.math.optimization.direct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.commons.math.analysis.MultivariateFunction;
import org.apache.commons.math.exception.MaxCountExceededException;
import org.apache.commons.math.exception.NullArgumentException;
import org.apache.commons.math.exception.TooManyEvaluationsException;
import org.apache.commons.math.optimization.BaseMultivariateRealOptimizer;
import org.apache.commons.math.optimization.ConvergenceChecker;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.SimpleScalarValueChecker;
import org.apache.commons.math.util.Incrementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eval.AlgorithmEvaluator;

/**
 * Base class for implementing optimizers for multivariate scalar functions.
 * This base class handles the boiler-plate methods associated to thresholds
 * settings, iterations and evaluations counting.
 *
 * @param <FUNC> Type of the objective function to be optimized.
 *
 * @version $Id$
 * @since 2.2
 */
public abstract class BaseAbstractScalarOptimizer<FUNC extends MultivariateFunction>
    implements BaseMultivariateRealOptimizer<FUNC> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	public ArrayList<double[]> bestOverTime = new ArrayList<double[]>();
	public ArrayList<double[]> historicalBest = new ArrayList<double[]>();

	public ArrayList<RealPointValuePair> bestPoints;
	//public double[] bestPoint;
	//public ArrayList<Double> bestValues;
	int numBest;
	//public double secondBestValue = Double.MAX_VALUE;
	
	public boolean earlyQualification = false;
	
	/** Evaluations counter. */
    protected final Incrementor evaluations = new Incrementor();
    /** Convergence checker. */
    private ConvergenceChecker<RealPointValuePair> checker;
    /** Type of optimization. */
    protected GoalType goal;
    /** Initial guess. */
    protected double[] start;
    /** Objective function. */
    protected MultivariateFunction function;

    /**
     * Simple constructor with default settings.
     * The convergence check is set to a {@link SimpleScalarValueChecker} and
     * the allowed number of evaluations is set to {@link Integer#MAX_VALUE}.
     */
    protected BaseAbstractScalarOptimizer() {
        this(new SimpleScalarValueChecker());
    }
    /**
     * @param checker Convergence checker.
     */
    protected BaseAbstractScalarOptimizer(ConvergenceChecker<RealPointValuePair> checker) {
        this.checker = checker;
    }

    /** {@inheritDoc} */
    public int getMaxEvaluations() {
        return evaluations.getMaximalCount();
    }

    /** {@inheritDoc} */
    public int getEvaluations() {
        return evaluations.getCount();
    }

    /** {@inheritDoc} */
    public ConvergenceChecker<RealPointValuePair> getConvergenceChecker() {
        return checker;
    }

    /**
     * Compute the objective function value.
     *
     * @param point Point at which the objective function must be evaluated.
     * @return the objective function value at the specified point.
     * @throws TooManyEvaluationsException if the maximal number of
     * evaluations is exceeded.
     */
    protected double computeObjectiveValue(double[] point) {
        //OutputHandler.writeArray(point);
        if (! evaluations.canIncrement()) {
        	throw new TooManyEvaluationsException(getMaxEvaluations());
        }
        double value = function.value(point);
        if (! (function instanceof AlgorithmEvaluator) 
        		|| ((AlgorithmEvaluator)function).getNumExp() == 1) {
            incrementEvalCount();
        }
        //OutputHandler.writeArray(point);

        //System.out.println(evaluations.getCount() + " " + value);
        return value;
    }
	/**
	 * 
	 */
	protected void incrementEvalCount() {
		try {
		    evaluations.incrementCount();
		} catch (MaxCountExceededException e) {
		    throw new TooManyEvaluationsException(e.getMax());
		}
	}

    /** {@inheritDoc} */
    public RealPointValuePair optimize(int maxEval, FUNC f, GoalType goalType,
                                       double[] startPoint) {
        // Checks.
        if (f == null) {
            throw new NullArgumentException();
        }
        if (goalType == null) {
            throw new NullArgumentException();
        }
        if (startPoint == null) {
            throw new NullArgumentException();
        }

        // Reset.
        evaluations.setMaximalCount(maxEval);
        evaluations.resetCount();

        // Store optimization problem characteristics.
        function = f;
        goal = goalType;
        start = startPoint.clone();

        // Perform computation.
        return doOptimize();
    }
    
    public void initBest(int numBest) {
    	this.numBest = Math.max(2, numBest);
    	int initCap = Math.max(this.numBest * 2, 10);
    	bestPoints = new ArrayList<RealPointValuePair>(initCap);
    }
    
    public void updateBest(double value, double[] point) {
    	RealPointValuePair p = new RealPointValuePair(point, value);
    	RealPointValuePair best;
    	if (bestPoints.size() < numBest) {
    		if (bestPoints.size() == 0) {
    			insertBest(0, p);
    			return;
    		} else {
	    		// not filled yet
	    		for (int i = bestPoints.size() - 1; i >= 0; i--) {
	    			best = bestPoints.get(i);
	    			if (value < best.getValue()) {
	    				continue;
	    			} else {
	    				insertBest(i + 1, p);
	    				return;
	    			}
	    		}
    		}
    		// not inserted, better than all elements in the list
			insertBest(0, p);
    	} else {
    		// list established
    		for (int i = numBest - 1; i >= 0; i--) {
    			best = bestPoints.get(i);
    			if (value < best.getValue()) {
    				continue;
    			} else if (i == numBest - 1){
    				return;
    			} else {
    				// found the position to insert
    				insertBest(i + 1, p);
    				return;
    			}
    		}
    		// not inserted, better than all elements in the list
    		insertBest(0, p);
    	}
    }
    
    protected void insertBest(int pos, RealPointValuePair point) {
    	// check whether the current point is already in the best list
    	for (int i = 0; i < pos; i++) {
    		if (function instanceof AlgorithmEvaluator && ((AlgorithmEvaluator) function).isSameConf(
    				point.getPoint(), bestPoints.get(i).getPoint())) {
    			return;
    		}
    	}
		bestPoints.add(pos, point);
		log.info("{} with value {} is added to best list at position {}", 
				Arrays.toString(point.getPoint()), point.getValue(), pos);
    }

    /**
     * @return the optimization type.
     */
    public GoalType getGoalType() {
        return goal;
    }

    /**
     * @return the initial guess.
     */
    public double[] getStartPoint() {
        return start.clone();
    }

    /**
     * Perform the bulk of the optimization algorithm.
     *
     * @return the point/value pair giving the optimal value for the
     * objective function.
     */
    protected abstract RealPointValuePair doOptimize();
}
