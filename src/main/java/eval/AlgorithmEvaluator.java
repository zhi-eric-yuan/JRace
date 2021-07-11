/*
 * Created by Zhi Yuan
 */
package eval;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tune.Tuner;
import util.MathHelp;
import util.Randomizer;
import util.SystemCaller;
import util.SystemProperty;
import algo.Configuration;
import algo.Instance;
import algo.Parameter;
import datahandler.OutputHandler;

/**
 * @author yuan Created on Dec 2, 2011
 * 
 */
public class AlgorithmEvaluator implements Evaluator {

	Configuration[] configurations;
	double penalty = 10;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * The maximum number of trials for evaluating an algorithm.  
	 */
	int maxTrial = 5;
	
	/**
	 * The maximum number of times that a search method can read consecutively from archive
	 * without sampling any new points. 
	 */
	int maxConsecutiveArchiveRead = 50;
	
	int numConsecutiveArchiveRead = 0;

	/**
	 * @return the numConsecutiveArchiveRead
	 */
	public int getNumConsecutiveArchiveRead() {
		return numConsecutiveArchiveRead;
	}

	/**
	 * @return the numConsecutiveArchiveRead
	 */
	public boolean exceedMaxConsecutiveArchiveRead() {
		return numConsecutiveArchiveRead >= maxConsecutiveArchiveRead;
	}

	/**
	 * @return the configurations
	 */
	public Configuration[] getConfigurations() {
		return configurations;
	}

	public int getNumConfigurations() {
		return configurations.length;
	}

	Instance[] instances;

	/**
	 * @return the instances
	 */
	public Instance[] getInstances() {
		return instances;
	}

	/**
	 * @param instances
	 *            the instances to set
	 */
	public void setInstances(final Instance[] instances) {
		this.instances = instances;
	}

	Parameter[] parameters;
	protected double[][] boundaries;
	private int numExp = 0;

	/**
	 * @return the numExp
	 */
	public int getNumExp() {
		return numExp;
	}

	/**
	 * Initialize an {@link AlgorithmEvaluator} object with a set of
	 * configurations and a set of instances. Repeated configurations will be
	 * removed.
	 * 
	 * @param configurations
	 *            an array algorithm configurations
	 * @param instances
	 *            an array of instances
	 */
	public AlgorithmEvaluator(Configuration[] configurations,
			Instance[] instances) {
		this.configurations = configurations;
		this.instances = instances;
		initConfigurations();
	}

	/**
	 * Initialize an {@link AlgorithmEvaluator} object with an list of
	 * parameters and the boundaries of each parameter. Used for evaluating each
	 * individual configuration by calling the value(double point) method.
	 * 
	 * @param parameters
	 *            a list of parameters
	 * @param boundaries
	 *            the boundaries of each parameter
	 */
	public AlgorithmEvaluator(Parameter[] parameters,
			double[][] boundaries) {
		this.parameters = parameters;
		this.boundaries = boundaries;
	}

	/**
	 * Evaluate a configuration on an instance with index given. 
	 * @param conf configuration to be evaluated. 
	 * @param instanceIndex index of the instance to be evaluated on.  
	 * @return double-value evaluation, or Double.MAX_VALUE if the evaluation fails, 
	 * either the target algorithm evaluation response is empty or its last line is 
	 * of invalid format. 
	 */
	public double evaluate(Configuration conf, int instanceIndex) {
		Instance ins = instances[instanceIndex];
		if (conf.isOutOfBound()) {
			log.warn("Configuration {} is out of bound, return a large value as evaluation.", conf);
			numExp = 0;
			return Double.MAX_VALUE;
		}
		log.info("evaluate configuration {} on instance {}", conf, ins);
		Double value = Tuner.arch.get(conf, ins);
		if (value != null) {
			log.info("Read from archive: {}", value);
			numConsecutiveArchiveRead++;
			numExp = 0;
			return value;
		} else {
			numConsecutiveArchiveRead = 0;
		}

		int trial = 0;
		String line;
		double opt;
		double qualScale;
		do {
			line = evaluateAlgo(conf, ins);
			
			if (line == null) {
				// command line is empty
				numExp = 1;
				return Double.MAX_VALUE;
			}

			// nonempty last line
			try {
				if (Tuner.goal == 'q') {
					value = Double.valueOf((new StringTokenizer(line)).nextToken()) * -1;
				} else if (Tuner.goal == 't') {
					StringTokenizer st = new StringTokenizer(line);
					value = Double.valueOf(st.nextToken());
					double qual = Double.valueOf(st.nextToken());
					//if (value >= Tuner.cutoffTime) {
					if (st.hasMoreTokens()
							&& ! st.nextToken().toLowerCase().startsWith("sat")) {
						// The modified PAR10 criterion for tuning computation time
						if (Tuner.opt == null) {
							// no optimum info provided. return 0.01 and assume it represents the gap in 100%
							opt = 0.01;
							qualScale = 100;
							qual = Math.max(qual, opt);
							// only for tuning cplex with gap as quality, 
							// the gap shouldn't be larger than 100%.
							if (qual >= qualScale) {
								qual = penalty * qualScale;
							}
							//qual = Math.min(qual, qualScale);
						} else {
							opt = Tuner.opt.get(ins.getName());
							qualScale = opt;
						}
						value = penalty * Tuner.cutoffTime
								* (1 + penalty * Math.abs(qual - opt) / qualScale);
						//value = Tuner.cutoffTime * qual / opt;
					}
					//value = Math.log(value + 1);
				}
				break;
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Invalid last line of response: {}", line);
			}
		} while (++trial < maxTrial);
		
		if (trial == maxTrial) {
			// invalid last line after max number of trials 
			log.error("max number {} trials exceeded. Invalid last line of target algorithm response: {}", maxTrial, line);
			numExp = 1;
			return Double.MAX_VALUE;
		}

		numExp = 1;
		Tuner.arch.put(conf, ins, value);
		log.info("Evaluation result: {}", value);
		return value;

		// return 10 - column;
		// return Math.max(3, column);
		// return (column + 1) * (row + 1);
		// return 1;
		// return Math.max(column, 2);

	}

	/**
	 * Evaluate a configuration on an instance
	 * @param conf configuration to be evaluated. 
	 * @param ins instance to be evaluated on. 
	 * @return the last line of the target algorithm evaluation in String. 
	 * Returns null if target algorithm evaluation returns empty response.
	 */
	private String evaluateAlgo_cmd(Configuration conf, Instance ins) {
		StringBuffer cmd = new StringBuffer(SystemProperty.get(SystemProperty.EXEC));
		cmd.append(" ");
		cmd.append(ins.getInsInit());
		cmd.append(" ");
		cmd.append(ins.getDir());
		cmd.append(File.separator);
		cmd.append(ins.getName());
		cmd.append(" ");

		double cutoffTime = Tuner.cutoffTime;
		if (cutoffTime != Double.MIN_VALUE) {
			String timeInit = SystemProperty.get(SystemProperty.TIME_INIT);
			if (timeInit != null) {
				cmd.append(timeInit);
				cmd.append(" ");
				cmd.append(cutoffTime);
				cmd.append(" ");
			}
		}

		cmd.append(ins.getSeedInit());
		cmd.append(" ");
		cmd.append(ins.getSeed());

		Double opt = null;
		if (Tuner.opt != null) {
			opt = Tuner.opt.get(ins.getName());
			if (opt != null) {
				cmd.append(SystemProperty.get(SystemProperty.OPT_INIT));
				cmd.append(" ");
				cmd.append(opt);
				cmd.append(" ");
			}
		}

		cmd.append(" ");
		String configInit = SystemProperty.get(SystemProperty.CONFIG_INIT);
		if (configInit != null) {
			cmd.append(configInit);
		}
		cmd.append(conf.toString());
		String configEnd = SystemProperty.get(SystemProperty.CONFIG_END);
		if (configEnd != null) {
			cmd.append(configEnd);
		}

		String cmdEnd = SystemProperty.get(SystemProperty.CMD_END);
		if (cmdEnd != null) {
			cmd.append(cmdEnd);
		}
		//cmd = new StringBuffer("bash randomEval");

		// TODO Here only read the evaluation result from the last line of the
		// screen output
		// of running the target algorithm. Should also consider other
		// possibilities.
		String response;
		int trial = 0;
		
		do {
			response = SystemCaller.call(cmd.toString());
		} while ((response == null || response.trim().length() == 0) && (++trial < maxTrial));
		
		if (trial == maxTrial) {
			// maximum number of trials called with no valid response
			log.error("{} trials exceeded. The response of the target algorithm is empty. The command line called: {}", maxTrial, cmd.toString());
			return null;
		}

		int pos = response.lastIndexOf('\n');
		int pos2;
		String line = response.substring(pos + 1);

		while (line.length() == 0) {
			pos2 = response.substring(0, pos).lastIndexOf('\n');
			line = response.substring(pos2 + 1, pos);
			pos = pos2;
		}
		log.info("Response: {}", line);

		return line;
		//return String.valueOf(-System.currentTimeMillis());
		//return String.valueOf(Randomizer.nextDouble());
		//return String.valueOf(Randomizer.nextDouble()) + " " + String.valueOf(Randomizer.nextDouble());
	}

	/**
	 * Evaluate a configuration on an instance
	 * @param conf configuration to be evaluated. 
	 * @param ins instance to be evaluated on. 
	 * @return the last line of the target algorithm evaluation in String. 
	 * Returns null if target algorithm evaluation returns empty response.
	 */
	private String evaluateAlgo(Configuration conf, Instance ins) {
		ArrayList<String> cmdArrays = new ArrayList<String>();
		String exec = SystemProperty.get(SystemProperty.EXEC);
		cmdArrays.addAll(separateByQuoteAndSpace(exec));
		cmdArrays.add(ins.getInsInit());
		cmdArrays.add(new StringBuffer(ins.getDir()).append(File.separator)
				.append(ins.getName()).toString());
		cmdArrays.add(ins.getSeedInit());
		cmdArrays.add(String.valueOf(ins.getSeed()));

		double cutoffTime = Tuner.cutoffTime;
		if (cutoffTime != Double.MIN_VALUE) {
			String timeInit = SystemProperty.get(SystemProperty.TIME_INIT);
			if (timeInit != null) {
				cmdArrays.add(timeInit);
				cmdArrays.add(String.valueOf(cutoffTime));
			}
		}

		if (Tuner.opt != null) {
			Double opt = Tuner.opt.get(ins.getName());
			if (opt != null) {
				cmdArrays.add(SystemProperty.get(SystemProperty.OPT_INIT));
				cmdArrays.add(String.valueOf(opt));
			}
		}
		
		StringBuffer conf2cmd = new StringBuffer();
		String configInit = SystemProperty.get(SystemProperty.CONFIG_INIT);
		if (configInit != null) {
			conf2cmd.append(configInit);
		}
		conf2cmd.append(conf.toString());
		String configEnd = SystemProperty.get(SystemProperty.CONFIG_END);
		if (configEnd != null) {
			conf2cmd.append(configEnd);
		}
		cmdArrays.addAll(separateByQuoteAndSpace(conf2cmd.toString()));

		String cmdEnd = SystemProperty.get(SystemProperty.CMD_END);
		if (cmdEnd != null) {
			cmdArrays.add(cmdEnd);
		}
		
		String[] cmdarray = cmdArrays.toArray(new String[0]);
		/*cmdarray = new String[] {SystemProperty.get(SystemProperty.EXEC), ins.getInsInit(),
				new StringBuffer(ins.getDir()).append(File.separator)
				.append(ins.getName()).toString(), ins.getSeedInit(), 
				String.valueOf(ins.getSeed()), SystemProperty.CONFIG_INIT, conf.toString()};
		
		
		cmdarray = new String[] {"python", SystemProperty.get(SystemProperty.EXEC), "--instance",
				new StringBuffer(ins.getDir()).append(File.separator)
				.append(ins.getName()).toString(), "--seed", 
				String.valueOf(ins.getSeed()), SystemProperty.get(SystemProperty.CONFIG_INIT), 
				conf.toString(), SystemProperty.get(SystemProperty.CONFIG_END)};
		*/
		//cmd = new StringBuffer("bash randomEval");

		// TODO Here only read the evaluation result from the last line of the
		// screen output
		// of running the target algorithm. Should also consider other
		// possibilities.
		String response;
		int trial = 0;
		
		do {
			//response = SystemCaller.call(cmd.toString());
			response = SystemCaller.call(cmdarray);
		} while ((response == null || response.trim().length() == 0) && (++trial < maxTrial));
		
		if (trial == maxTrial) {
			// maximum number of trials called with no valid response
			log.error("{} trials exceeded. The response of the target algorithm is empty. The command line called: {}", maxTrial, Arrays.toString(cmdarray));
			return null;
		}

		int pos = response.lastIndexOf('\n');
		int pos2;
		String line = response.substring(pos + 1);

		while (line.length() == 0) {
			pos2 = response.substring(0, pos).lastIndexOf('\n');
			line = response.substring(pos2 + 1, pos);
			pos = pos2;
		}
		log.info("Response: {}", line);
		//OutputHandler.writeln(line);
		return line;
		//return String.valueOf(-System.currentTimeMillis());
		//return String.valueOf(Randomizer.nextDouble());
		//return String.valueOf(Randomizer.nextDouble()) + " " + String.valueOf(Randomizer.nextDouble());
	}

	private ArrayList<String> separateByQuoteAndSpace(final String text) {
		ArrayList<String> list = new ArrayList<String>();
		ArrayList<String> byQuotes = separateString(text, '\"');
		if (byQuotes.size() > 1) {
			boolean nowInQuote = text.charAt(0) == '\"';
			for (String i: byQuotes) {
				if (nowInQuote) {
					list.add(i);
				} else {
					list.addAll(separateString(i, ' '));
				}
				nowInQuote = !nowInQuote;
			}
		}
		return list;
	}

	private ArrayList<String> separateString(final String text, char by) {
		int index;
		ArrayList<String> list = new ArrayList<String>();
		String part = text;
		while ((index = part.indexOf(by)) > 0) {
			list.add(part.substring(0, index).trim());
			part = part.substring(index + 1).trim();
		}
		if (! part.isEmpty()) {
			list.add(part);
		}
		return list;
	}

	public double evaluate(int row, int column) {
		Configuration conf = configurations[column];
		// return 10 - column;
		return evaluate(conf, row);
	}

	@Override
	/**
	 * Evaluate each configuration as a given double array. 
	 * @param point an array of values of one configuration
	 */
	public double value(double[] point) {
		// OutputHandler.writeln(Arrays.toString(point));
		double[] paramValues = toParamValues(point);
		try {
			shift(point, paramValues);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		double value = Double.MIN_VALUE;
		try {
			value = evaluate(paramValues);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return value;
	}

	/**
	 * @param point
	 * @return
	 */
	public double[] toParamValues(double[] point) {
		double[] paramValues = null;
		try {
			paramValues = rescale(point);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (parameters != null) {
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i].isInteger()) {
					paramValues[i] = Math.round(paramValues[i]);
				}
			}
		}
		paramValues = MathHelp.roundArrayToSignifDigit(paramValues, Tuner.signifDigit);
		//paramValues = MathHelp.roundArrayToDecimalPlace(paramValues, Tuner.signifDigit);
		return paramValues;
	}

	private double[] rescale(final double[] point) throws Exception {
		if (boundaries != null) {
			int dim = point.length;
			double[] scaled = new double[dim];
			double[] lower = boundaries[0];
			double range;
			if (dim != lower.length) {
				throw new Exception("The parameter range and values have different length");
			}
			for (int i = 0; i < dim; i++) {
				range = boundaries[1][i] - lower[i];
				scaled[i] = point[i] * range + lower[i];
			}
			return scaled;
		} else {
			return point.clone();
		}
	}

	/**
	 * Shift the point value to the actual rounded evaluated value.
	 * 
	 * @param toShift
	 *            the original point
	 * @param paramValues
	 *            the actual parameter values
	 * @return the shifted point
	 * @throws Exception
	 *             if dimension doesn't match
	 */
	public double[] shift(double[] toShift, final double[] paramValues)
			throws Exception {
		if (boundaries != null) {
			int dim = paramValues.length;
			double[] scaled = new double[dim];
			double[] lower = boundaries[0];
			double range;
			if (dim != lower.length) {
				throw new Exception("The parameter range and values have different length");
			}
			for (int i = 0; i < dim; i++) {
				range = boundaries[1][i] - lower[i];
				scaled[i] = (paramValues[i] - lower[i]) / range;
				toShift[i] = scaled[i];
			}
			return scaled;
		} else {
			return paramValues.clone();
		}
	}

	private double evaluate(double[] point) {
		Configuration conf = new Configuration(parameters, point);
		return evaluate(conf);
	}

	/**
	 * Check whether the two points given represent the same configuration
	 * @param point1 
	 * @param point2
	 * @return
	 */
	public boolean isSameConf(double[] point1, double[] point2) {
		boolean b = Arrays.equals(toParamValues(point1), toParamValues(point2));
		log.info("{} and {} same conf? {}", point1, point2, b);
		return b;
		/*Configuration conf1 = new Configuration(parameters, point1);
		Configuration conf2 = new Configuration(parameters, point2);
		return conf1.equals(conf2);*/
	}
	
	/**
	 * Evaluate one input configuration on all 
	 * @param conf
	 * @return
	 */
	private double evaluate(Configuration conf) {
		double sum = 0;
		int numInstances = getNumInstances();
		for (int i = 0; i < numInstances; i++) {
			sum += evaluate(conf, i);
		}
		
		return sum / numInstances;
	}

	public int getNumInstances() {
		return instances.length;
	}

	/**
	 * Initialize. Remove repeated configurations.
	 * 
	 */
	private void initConfigurations() {
		int numConfs = configurations.length;
		ArrayList<Configuration> uniqConfs = new ArrayList<Configuration>(
				numConfs);
		HashMap<Configuration, Object> confMap = new HashMap<Configuration, Object>(
				numConfs);
		Configuration conf;

		for (int i = 0; i < numConfs; i++) {
			conf = configurations[i];
			if (confMap.get(conf) == null) {
				uniqConfs.add(conf);
				confMap.put(conf, new Object());
			}
		}
		configurations = new Configuration[uniqConfs.size()];
		uniqConfs.toArray(configurations);
	}

	public double[][] evaluateAll() {
		int numIns = getNumInstances();
		int numConf = configurations.length;
		double[][] results = new double[numIns][numConf];
		for (int i = 0; i < numIns; i++) {
			for (int j = 0; j < numConf; j++) {
				results[i][j] = evaluate(i, j);
			}
		}
		return results;
	}

	public String validateConf(int index) {
		Configuration conf = configurations[index];
		int numIns = getNumInstances();
		StringBuffer sb = new StringBuffer();
		int trial;
		String line = null;
		
		for (int i = 0; i < numIns; i++) {
			trial = 0;
			do {
				//line = evaluateAlgo(conf, instances[i]);
				line = String.valueOf(evaluate(conf, i));
				//line = evaluateAlgo(conf, instances[i]);
			} while (line == null && ++trial < maxTrial);
			
			if (line == null) {
				// the last line of the target algorithm response is empty.
				// writes an empty line for this validation instance.
				line = "";
			}
			
			sb.append(line);
			sb.append("\n");
		}
		return sb.toString();
	}

	public void setConfigurations(Configuration[] configurations) {
		this.configurations = configurations;
	}

}
