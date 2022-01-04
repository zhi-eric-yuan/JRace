/*
 * Author: Eric Yuan
 * Created on Aug 23, 2005
 */
package datahandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.SystemProperty;
import algo.CategoricalParameter;
import algo.Instance;
import algo.NumericalParameter;
import algo.Parameter;

/**
 * Enter type description here.
 * @author Eric Yuan
 * <p>
 * Created on Aug 23, 2005
 */
public class InputHandler {
	
	private static Logger log = LoggerFactory.getLogger(InputHandler.class);

	public static String function;
	public static int dim;
	public static double lowerBound;
	public static double upperBound;
	public static double [][] shifts;
	public static double [][] startPoints;
	
	private static HashMap<String, Parameter> paramMap;
	
	/**
	 * Create a new InputHandler object.
	 */
	public InputHandler() {
		super();
	}
	
	public static HashMap<String, String> readSystemProperties(String filename) {
		log.info("Start reading system properties===================");
		//String path = VisualGraph.systemPath;
		//String path = ".";
		//String path = System.getProperty("user.dir");
		//System.out.println(path);
		InputReader reader = new InputReader(filename);
		HashMap<String, String> table = new HashMap<String, String>();
		int index;
		String line;
		String key;
		String value;
		
		while (reader.readLine()) {
			line = reader.getLine();
			index = line.indexOf("//");
			if (index >= 0) {
				line = line.substring(0, index).trim();
			}
			index = line.indexOf("=");
			if (index >= 0) {
				key = line.substring(0, line.indexOf("=")).trim();
				value = line.substring(line.indexOf("=") + 1).trim();
				table.put(key, value);
			}
		}
		
		return table;
	}
	
	
	public static String readFile(String filename) {
		return readFile(System.getProperty("user.dir"), filename);
	}
	
	public static String readFile(String path, String name) {
		InputReader reader = new InputReader(path, name);
		return reader.readAll();
	}
	
	public static void readFunction(String filename) {
		//readFunction(System.getProperty("user.dir"), filename);
		readFunction("", filename);
	}
	
	public static void readFunction(String path, String name) {
		InputReader reader = new InputReader(path, name);
		reader.readLine();
		function = reader.nextString();
		dim = reader.nextInt();
		lowerBound = reader.nextDouble();
		upperBound = reader.nextDouble();
		int ncol = 0;
		double [] aShift;
		Vector<double[]> v = new Vector<double[]>();
		
		while (reader.readLine()) {
			ncol = reader.tokenCount();
			aShift = new double[ncol];
			for (int i = 0; i < ncol; i++) {
				aShift[i] = reader.nextDouble();
			}
			v.add(aShift);
		}
		
		shifts = new double[v.size()][ncol];
		v.copyInto(shifts);

	}

	public static void readStartPt(String filename) {
		//readStartPt(System.getProperty("user.dir"), filename);
		readStartPt("", filename);
	}
	
	public static void readStartPt(String path, String name) {
		InputReader reader = new InputReader(path, name);
		int ncol = 0;
		double [] aPoint;
		Vector<double[]> v = new Vector<double[]>();
		
		while (reader.readLine()) {
			ncol = reader.tokenCount();
			aPoint = new double[ncol];
			for (int i = 0; i < ncol; i++) {
				aPoint[i] = reader.nextDouble();
			}
			v.add(aPoint);
		}
		
		startPoints = new double[v.size()][ncol];
		v.copyInto(startPoints);
	}

	public static Parameter[] readParams(String paramFile) {
		log.info("Start reading parameters to be tuned: {}", paramFile);
		//String paramDirFile = SystemProperty.addWorkDir(paramFile);
		String paramDirFile = paramFile;
		log.info(paramDirFile);
		InputReader reader = new InputReader(paramDirFile);
		String line;
		String paramName;
		String paramRep;
		int pos;
		int pos2;
		char paramType;
		double lower;
		double upper;
		ArrayList<Parameter> paramList = new ArrayList<Parameter>();
		paramMap = new HashMap<String, Parameter>();
		String[] values;
		String value;
		double def;
		String defCat;
		Parameter param;
		
		while (reader.readLine()) {
			line = reader.getLine();
			paramName = reader.nextString();
			int singleQuoteIndex = line.indexOf('\'');
			if (singleQuoteIndex >= 0) {
				pos = singleQuoteIndex + 1;
				pos2 = line.indexOf('\'', singleQuoteIndex);
			} else {
				pos = line.indexOf('\"') + 1;
				pos2 = line.indexOf('\"', pos);
				
			}
			paramRep = line.substring(pos, pos2);
			line = line.substring(pos2 + 1).trim();
			paramType = line.charAt(0);
			values = extractParamValues(line);
			value = null;
			if (line.indexOf('{') >= 0) {
				value = extractTextBetween(line, '{', '}').trim();
			}
			if (paramType == 'r' || paramType == 'i') {
				if (values.length != 2) {
					log.error("Parameter {} type {} does not properly define its value: {}", 
							paramName, paramType, line);
				}
				lower = Double.valueOf(values[0].trim());
				upper = Double.valueOf(values[1].trim());
				if (value != null) {
					def = Double.valueOf(value);
					if (def < lower || def > upper) {
						log.error("Parameter {} type {} with default value {} outside its boundary {}", 
								paramName, paramType, value, line);
						param = new NumericalParameter(paramName, paramRep, paramType, 
								new double[]{lower, upper});
					} else {
						param = new NumericalParameter(paramName, paramRep, paramType, 
								new double[]{lower, upper}, def);
					}
				} else {
					param = new NumericalParameter(paramName, paramRep, paramType, 
							new double[]{lower, upper});
				}
				addAParam(paramName, paramList, param);
			} else if (paramType == 'c') {
				if (value != null) {
					if (! Arrays.asList(values).contains(value)) {
						log.error("Parameter {} type {} with default value {} undefined by {}", 
								paramName, paramType, value, line);
						param = new CategoricalParameter(paramName, paramRep, paramType, values);
					} else {
						param = new CategoricalParameter(paramName, paramRep, paramType, values, 
								value);
					}
				} else {
					param = new CategoricalParameter(paramName, paramRep, paramType, values);
				}
				addAParam(paramName, paramList, param);
			} else {
				log.error("Unrecognized parameter type {}", paramType);
			}
		}
		readConditionalParameters(paramDirFile, paramList);
		Parameter[] params = new Parameter[paramList.size()];
		paramList.toArray(params);
		return params;
	}

	/**
	 * @param paramName
	 * @param paramList
	 * @param param
	 */
	private static void addAParam(String paramName,
			ArrayList<Parameter> paramList, Parameter param) {
		paramList.add(param);
		paramMap.put(paramName, param);
	}

	private static void readConditionalParameters(String paramDirFile, 
			ArrayList<Parameter> paramList) {
		log.info("Start reading conditional parameters: {}", paramDirFile);
		InputReader reader = new InputReader(paramDirFile);
		String line;
		int pos;
		String[] values;
		String paramName, parentParamName;
		Parameter param, parentParam;
	
		while (reader.readLine()) {
			line = reader.getLine();
			paramName = reader.nextString();
			param = paramMap.get(paramName);
			pos = line.indexOf('|');
			if (pos >= 0) {
				line = line.substring(pos + 1).trim();
				pos = line.indexOf(' ');
				parentParamName = line.substring(0, pos).trim();
				parentParam = paramMap.get(parentParamName);
				line = line.substring(pos + 1).trim();
				values = extractParamValues(line);
				param.setConditional(parentParam, values);
			}
			
		}
		

	}

	private static String[] extractParamValues(String line) {
		return extractParamValues(line, '[', ']', ",");
	}
	/**
	 * @param line
	 * @return
	 */
	private static String[] extractParamValues(String line, char start, char end, 
			String sep) {
		String extracted = extractTextBetween(line, start, end);
		StringTokenizer st = new StringTokenizer(extracted, sep);
		int length = st.countTokens();
		String[] values = new String[length];
		for (int i = 0; i < length; i++) {
			values[i] = st.nextToken().trim();
		}
		
		return values;
	}

	/**
	 * @param line
	 * @param start
	 * @param end
	 * @param sep
	 * @return
	 */
	protected static String extractTextBetween(String line, char start, char end) {
		int pos;
		int pos2;
		pos = line.indexOf(start) + 1;
		pos2 = line.indexOf(end, pos);
		if (pos < 0 || pos2 < 0) {
			log.error("Error while parsing parameter values from string \"{}\": it should be enclosed within \"{}\" and \"{}\"", 
					line, start, end);
			return null;
		} else {
			return line.substring(pos, pos2);
		}
	}
	
	public static Instance[] readInstanceSeed(String insFile) {
		return readInstanceSeed(insFile, SystemProperty.get(SystemProperty.INS_DIR));
	}

	public static Instance[] readInstanceSeed(String insFile, String dir) {
		log.info("Start reading instances and seeds: {}", insFile);
		//InputReader reader = new InputReader(SystemProperty.addWorkDir(insFile));
		InputReader reader = new InputReader(insFile);
		String insInit = SystemProperty.get(SystemProperty.INSINIT);
		String seedInit = SystemProperty.get(SystemProperty.SEEDINIT);

		ArrayList<Instance> insList = new ArrayList<Instance>();
		
		Instance ins;
		long seed;
		String insName;
		//String insDir = SystemProperty.addWorkDir(dir);
		String insDir = dir;
		
		
		while (reader.readLine()) {
			seed = Long.valueOf(reader.nextString());
			insName = reader.nextString();
			
			ins = new Instance(seed, insName, insDir, insInit, seedInit);
			insList.add(ins);
		}
		
		Instance[] instances = new Instance[insList.size()];
		insList.toArray(instances);
		return instances;

	}

	public static HashMap<String, Double> readOpt(String optFile) {
		OutputHandler.writeln("Start reading optimum. ");
		//InputReader reader = new InputReader(SystemProperty.addWorkDir(optFile));
		InputReader reader = new InputReader(optFile);
		String key;
		double value;
		HashMap<String, Double> opt = new HashMap<String, Double>();

		while (reader.readLine()) {
			key = reader.nextString();
			value = reader.nextDouble();
			opt.put(key, value);
		}
		
		return opt;
	}
	
}
