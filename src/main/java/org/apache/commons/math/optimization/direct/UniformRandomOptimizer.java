/*
 * Created by Zhi Yuan
 */
package org.apache.commons.math.optimization.direct;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math.analysis.MultivariateFunction;
import org.apache.commons.math.optimization.MultivariateRealOptimizer;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datahandler.OutputHandler;
import util.Randomizer;

/**
 * @author yuan
 * Created on Aug 17, 2012
 *
 */
public class UniformRandomOptimizer extends BaseAbstractScalarOptimizer<MultivariateFunction>
		implements MultivariateRealOptimizer {

	private final Logger log = LoggerFactory.getLogger(getClass());
	public ArrayList<double[]> bestOverTime = new ArrayList<double[]>();

	protected int dimension;
	protected double[][] boundaries;
	
	/**
	 * 
	 */
	public UniformRandomOptimizer(int dimension, double[][] boundaries) {
		super();
		this.dimension = dimension;
		this.boundaries = boundaries;
	}


	/* (non-Javadoc)
	 * @see org.apache.commons.math.optimization.direct.BaseAbstractScalarOptimizer#doOptimize()
	 */
	@Override
	protected RealPointValuePair doOptimize() {
		int budget = getMaxEvaluations();
		double[] point;
		double bestValue = Double.MAX_VALUE;
		double[] best = null;
		double value;
		double[] newBestOverTime;
				
		for (int i = 0; i < budget; i++) {
			point = Randomizer.generateUniformRandomVector(dimension, boundaries);
			value = computeObjectiveValue(point);
			if (value < bestValue) {
				bestValue = value;
				best = point.clone();
				//log.info(Arrays.toString(bestPoint));
				newBestOverTime = new double[2];
				newBestOverTime[0] = getEvaluations();
				newBestOverTime[1] = bestValue;
				bestOverTime.add(newBestOverTime.clone());
			}
			updateBest(value, point);
		}
		
		
		RealPointValuePair p = new RealPointValuePair(best, bestValue);
		return p;
	}
}
