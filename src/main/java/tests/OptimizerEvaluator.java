/*
 * Created by Zhi Yuan
 */
package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.math.analysis.MultivariateFunction;
import org.apache.commons.math.exception.MathIllegalStateException;
import org.apache.commons.math.exception.TooManyEvaluationsException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.MultiStartMultivariateRealOptimizer;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.direct.BOBYQAOptimizer;
import org.apache.commons.math.optimization.direct.CMAESOptimizer;
import org.apache.commons.math.optimization.direct.CMAUOptimizer;
import org.apache.commons.math.optimization.direct.NelderMeadSimplex;
import org.apache.commons.math.optimization.direct.SimplexOptimizer;
import org.apache.commons.math.optimization.direct.UniformRandomOptimizer;
import org.apache.commons.math.random.JDKRandomGenerator;

import datahandler.CmdLineHandler;
import datahandler.CopsOptionParser;
import datahandler.InputHandler;
import datahandler.MathOptionParser;
import datahandler.OutputHandler;
import jargs.gnu.CmdLineParser;
import tests.BenchmarkFunction.AckleyShift;
import tests.BenchmarkFunction.AckleyShiftClamp;
import tests.BenchmarkFunction.Elli;
import tests.BenchmarkFunction.GriewankShift;
import tests.BenchmarkFunction.GriewankShiftClamp;
import tests.BenchmarkFunction.RastriginShift;
import tests.BenchmarkFunction.RastriginShiftClamp;
import tests.BenchmarkFunction.Rosen;
import tests.BenchmarkFunction.RosenShiftClamp;
import tune.Tuner;

/**
 * @author yuan
 * Created on Aug 13, 2012
 *
 */
public class OptimizerEvaluator {

	public static final String filename = "ra4.txt";
	public static final String start_pt = "start_ra4.txt";
	public static final int BUDGET = 100;
	public static ArrayList<double[]> bestOverTime;
	//public static String algo = "bobyqa";
	
	/**
	 * 
	 */
	public OptimizerEvaluator() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		CmdLineHandler chandler = new CmdLineHandler();
		MathOptionParser parser = (MathOptionParser) chandler.readCmdLineArgs(args, false);
		Tuner.parser = new CopsOptionParser();
		
		String function = (String)parser.getOptionValue(MathOptionParser.FUNCTION);
		int dim = ((Integer) parser.getOptionValue(MathOptionParser.DIM)).intValue();
		String funFile = null;
		String classPath = System.getProperty("user.dir");
		if (classPath.indexOf(':') >= 0) {
			classPath = classPath.substring(0, classPath.indexOf(':'));
		}
		String startFile= null;
		if (function.equalsIgnoreCase("rosenbrock")) {
			function = "rosenbrock";
			OutputHandler.writeln("Solving Rosenbrock function");
			funFile = classPath + File.separator 
				+ "rosenbrock" + File.separator + "r" + dim + ".txt";
			startFile = classPath + File.separator 
				+ "rosenbrock" + File.separator + "start_r" + dim + ".txt";
		} else if (function.equalsIgnoreCase("rastrigin")) {
			function = "rastrigin";
			OutputHandler.writeln("Solving Rastrigin function");
			funFile = classPath + File.separator 
				+ "rastrigin" + File.separator + "ra" + dim + ".txt";
			startFile = classPath + File.separator 
				+ "rastrigin" + File.separator + "start_ra" + dim + ".txt";
		} else if (function.equalsIgnoreCase("ackley")) {
			function = "ackley";
			OutputHandler.writeln("Solving Ackley function");
			funFile = classPath + File.separator 
				+ "ackley" + File.separator + "a" + dim + ".txt";
			startFile = classPath + File.separator 
				+ "ackley" + File.separator + "start_a" + dim + ".txt";
		} else if (function.equalsIgnoreCase("griewank")) {
			function = "griewank";
			OutputHandler.writeln("Solving Griewank function");
			funFile = classPath + File.separator 
				+ "griewank" + File.separator + "g" + dim + ".txt";
			startFile = classPath + File.separator 
				+ "griewank" + File.separator + "start_g" + dim + ".txt";
		} else if (function.equalsIgnoreCase("ellipsoid")) {
			function = "ellipsoid";
			OutputHandler.writeln("Solving Ellipsoid function");
			funFile = classPath + File.separator 
				+ "ellipsoid" + File.separator + "e" + dim + ".txt";
			startFile = classPath + File.separator 
				+ "ellipsoid" + File.separator + "start_e" + dim + ".txt";
		}

		OutputHandler.writeln("Dimension " + dim);
		//System.getProperties().list(System.out);
		System.out.println(funFile);
		
		InputHandler.readFunction(funFile);

		if (Boolean.TRUE.equals(parser.getOptionValue(MathOptionParser.EVA))) {
			OutputHandler.debug = Boolean.TRUE.equals(
					parser.getOptionValue(MathOptionParser.DEBUG));
			evalAPoint(parser, dim, funFile, function);
		} else {
			InputHandler.readStartPt(startFile);
			
			String algo = (String) parser.getOptionValue(MathOptionParser.ALGO);

			//System.out.println(algo);
			testOptimizer(algo);
		}

	}


	/**
	 * @param parser
	 */
	private static void evalAPoint(CmdLineParser parser, int dim, String funFile, 
			String function) {
		OutputHandler.writeln("Start evaluating a point by a function");
		
		String pointInput = (String)parser.getOptionValue(MathOptionParser.POINT);
		OutputHandler.writeln("Read input point as " + pointInput);
		StringTokenizer st = new StringTokenizer(pointInput);
		int dimP = st.countTokens();
		if (dimP != dim) {
			System.err.println("The input point " + pointInput + " has " + dimP
					+ " values, not equal to the dimension " + dim 
					+ " indicated in the instance file" + funFile + ". Exit.");
			System.exit(1);
		}
		double [] point = new double[dim];
		for (int i = 0 ; st.hasMoreTokens() ; i++) {
			point[i] = Double.parseDouble(st.nextToken());
		}
		
		double [][] shifts = InputHandler.shifts;
		int index = ((Integer) parser.getOptionValue(CopsOptionParser.SCENARIO)).intValue();
		OutputHandler.writeln("Scenario index " + index);
		double[] shift = shifts[index - 1];

		MultivariateFunction func = determineFunction(function, shift);
		double value = func.value(point);
		
		System.out.println(value);
	}

	private static void testOptimizer() {
		testOptimizer("bobyqa");
	}

	/**
	 * 
	 */
	private static void testOptimizer(String algo) {
		OutputHandler.writeln("Testing algorithm " + algo);
		String function = InputHandler.function;
		int dim = InputHandler.dim;
		double[] bound = {InputHandler.lowerBound, InputHandler.upperBound};
		double [][] shifts = InputHandler.shifts;
		double [][] startPoints = InputHandler.startPoints;
		//OutputHandler.writeln(InputHandler.function);
		//OutputHandler.writeln(dim);
		OutputHandler.writeln("bound: " + bound[0] + " " + bound[1]);
		int index;
		File resultDir = new File("results");
		resultDir.mkdirs();
		String resultFile;
		

		for (int i = 0; i < shifts.length; i++) {
		//for (int i = 1; i < 2; i++) {
			index = i + 1;
			
			if (algo.equalsIgnoreCase("bobyqa")) {
				resultFile = resultDir + File.separator + "b_" + function + "_" + dim + "_" 
							+ index + ".res";
				evalBobyqa(function, dim, bound, shifts[i], startPoints[i], resultFile);
			} else if (algo.equalsIgnoreCase("cmaes")) {
				resultFile = resultDir + File.separator + "c_" + function + "_" + dim + "_" 
					+ index + ".res";
				evalCmaes(function, dim, bound, shifts[i], startPoints[i], resultFile);
			} else if (algo.equalsIgnoreCase("simplex")) {
				resultFile = resultDir + File.separator + "s_" + function + "_" + dim + "_" 
					+ index + ".res";
				evalSimplex(function, dim, bound, shifts[i], startPoints[i], resultFile);
			} else if (algo.equalsIgnoreCase("cmau")) {
				resultFile = resultDir + File.separator + "cu_" + function + "_" + dim + "_" 
						+ index + ".res";
				evalCmau(function, dim, bound, shifts[i], startPoints[i], resultFile);
			} else if (algo.toLowerCase().startsWith("uni")) {
				resultFile = resultDir + File.separator + "u_" + function + "_" + dim + "_" 
						+ index + ".res";
				evalUniform(function, dim, bound, shifts[i], startPoints[i], resultFile);
			}
			
			//evalMultiStartBobyqa(function, dim, bound, shifts[i], startPoints[i], resultFile);
			
			//evalCmau(function, dim, bound, shifts[i], startPoints[i], resultFile);
		}
	}

	private static RealPointValuePair evalUniform(String function, int dim, double[] bound, double[] shift, 
			double[] startPoint, String resultFile) {
		MultivariateFunction func = determineFunction(function, shift);
		double[][] boundaries = createBoundaries(dim, bound);
		UniformRandomOptimizer optim = new UniformRandomOptimizer(boundaries[0].length, boundaries);
        RealPointValuePair result = optim.optimize(BUDGET, func, GoalType.MINIMIZE, startPoint);
        //System.out.println(result.getValue());
        /*List<Double> hist = optim.getStatisticsFitnessHistory();
        for (int i = 0; i < hist.size(); i++) {
        	OutputHandler.out.append(hist.get(i));
        	System.out.println(hist.get(i));
        }*/
        //OutputHandler.print2File(resultFile, OutputHandler.out.toString());
		OutputHandler.printArrayList2File(resultFile, optim.bestOverTime);
        return result;
		
	}

	private static void evalSimplex(String function, int dim, double[] bound,
			double[] shift, double[] startPoint, String resultFile) {
		double[][] boundaries = createBoundaries(dim, bound);
		MultivariateFunction func = determineFunction(function, shift, true, boundaries);
		SimplexOptimizer optim = new SimplexOptimizer(1e-16, 1e-16);
		optim.initBest(1);
		double[] steps = computeSteps(startPoint, boundaries);
		optim.setSimplex(new NelderMeadSimplex(steps));
		RealPointValuePair result = null;
		try {
			result = optim.optimize(BUDGET, func, GoalType.MINIMIZE, startPoint);
		} catch(TooManyEvaluationsException e) {
			result = optim.getBest();
		}
        //System.out.println(result.getValue());
        /*List<Double> hist = optim.getStatisticsFitnessHistory();
        for (int i = 0; i < hist.size(); i++) {
        	OutputHandler.out.append(hist.get(i));
        	System.out.println(hist.get(i));
        }*/
        OutputHandler.appendTimeValue(optim.getEvaluations(), result.getValue());
		OutputHandler.print2File(resultFile, OutputHandler.out.toString());
		OutputHandler.out = new StringBuffer();
		//OutputHandler.printArrayList2File(resultFile, optim.getEvaluations());

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

	private static RealPointValuePair evalCmau(String function, int dim, double[] bound,
			double[] shift, double[] startPoint, String resultFile) {
		//OutputHandler.out = new StringBuffer();
		//bestOverTime = new ArrayList<double[]>();
		MultivariateFunction func = determineFunction(function, shift);
		double[][] boundaries = createBoundaries(dim, bound);
		CMAUOptimizer optim = new CMAUOptimizer(0, null, boundaries);
        RealPointValuePair result = optim.optimize(BUDGET, func, GoalType.MINIMIZE, startPoint);
        //System.out.println(result.getValue());
        /*List<Double> hist = optim.getStatisticsFitnessHistory();
        for (int i = 0; i < hist.size(); i++) {
        	OutputHandler.out.append(hist.get(i));
        	System.out.println(hist.get(i));
        }*/
        //OutputHandler.print2File(resultFile, OutputHandler.out.toString());
		OutputHandler.printArrayList2File(resultFile, optim.bestOverTime);
        return result;

	}

	private static RealPointValuePair evalCmaes(String function, int dim,
			double[] bound, double[] shift, double[] startPoint, String resultFile) {
		//OutputHandler.out = new StringBuffer();
		//bestOverTime = new ArrayList<double[]>();
		MultivariateFunction func = determineFunction(function, shift);
		double[][] boundaries = createBoundaries(dim, bound);
		CMAESOptimizer optim = new CMAESOptimizer(0, null, boundaries);
        RealPointValuePair result = optim.optimize(BUDGET, func, GoalType.MINIMIZE, startPoint);
        System.out.println(result.getValue());
        /*List<Double> hist = optim.getStatisticsFitnessHistory();
        for (int i = 0; i < hist.size(); i++) {
        	OutputHandler.out.append(hist.get(i));
        	System.out.println(hist.get(i));
        }*/
		//OutputHandler.print2File(resultFile, OutputHandler.out.toString());
		OutputHandler.printArrayList2File(resultFile, optim.bestOverTime);

        return result;
	}

	/**
	 * @param dim
	 * @param bound
	 * @return
	 */
	private static double[][] createBoundaries(int dim, double[] bound) {
		double[][] boundaries = new double[2][dim];
		for (int i = 0; i < dim; i++) {
			boundaries[0][i] = bound[0];
		}
		for (int i = 0; i < dim; i++) {
			boundaries[1][i] = bound[1];
		}
		return boundaries;
	}

	private static RealPointValuePair evalBobyqa(String function, int dim, double[] bound,
			double[] shift, double[] startPoint, String resultFile) {
		
		//OutputHandler.out = new StringBuffer();
		//bestOverTime = new ArrayList<double[]>();
		MultivariateFunction func = determineFunction(function, shift);
		int m = 2 * dim + 1;
		double[][] boundaries = createBoundaries(dim, bound);
		double range = bound[1] - bound[0];
		double startRadius = 0.5 * range;
		BOBYQAOptimizer optim = new BOBYQAOptimizer(m, startRadius, 1e-16);
        //System.out.println(optim.getLowerBound());
		RealPointValuePair result = null;
		try {
			result = optim.optimize(BUDGET, func, GoalType.MINIMIZE, startPoint, 
					boundaries[0], boundaries[1]);
		} catch (MathIllegalStateException e) {
			System.err.println(e.getMessage());
			System.err.println("Error by BOBYQA on " + resultFile + ", continue.");
		} /*catch (PathIsExploredException e) {
			System.err.println(e.getMessage());
			System.err.println("Error by BOBYQA on " + resultFile + ", continue.");			
		}*/
		//OutputHandler.print2File(resultFile, OutputHandler.out.toString());
		OutputHandler.printArrayList2File(resultFile, optim.bestOverTime);
		return result;
	}

	/**
	 * @param function
	 * @param shift
	 * @return
	 */
	private static MultivariateFunction determineFunction(String function,
			double[] shift) {
		return determineFunction(function, shift, false, null);
	}
	/**
	 * @param function
	 * @param shift
	 * @return
	 */
	private static MultivariateFunction determineFunction(String function,
			double[] shift, boolean needClamping, double[][] boundaries) {
		MultivariateFunction func = null;
		System.out.println(function);
		System.exit(0);
		if (function.equalsIgnoreCase("rosenbrock")) {
			func = needClamping? 
					new RosenShiftClamp(shift, boundaries) : new Rosen(shift);
		} else if (function.equalsIgnoreCase("ackley")) {
			func = needClamping? 
					new AckleyShiftClamp(shift, boundaries) : new AckleyShift(shift);
		} else if (function.equalsIgnoreCase("rastrigin")) {
			func = needClamping? 
					new RastriginShiftClamp(shift, boundaries) : new RastriginShift(shift);
		} else if (function.equalsIgnoreCase("griewank")) {
			func = needClamping? 
					new GriewankShiftClamp(shift, boundaries) : new GriewankShift(shift);
		} else if (function.equalsIgnoreCase("ellipsoid")) {
			func = needClamping? 
					new Elli(shift, boundaries) : new Elli(shift);
		}
		return func;
	}

}
