/*
 * Created by Zhi Yuan
 */
package algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.CollectionHandler;

/**
 * @author yuan
 * Created on Apr 16, 2013
 *
 */
public class Configuration {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	protected boolean outOfBound = false;
	/**
	 * The penalty value for a configuration being out of bound. 
	 * By default, 1 means no penalty, and it should be a multiplicative factor greater than 1. 
	 */
	protected double outOfBoundPenalty = 1;
	
	/**
	 * @return the outOfBound
	 */
	public boolean isOutOfBound() {
		return outOfBound;
	}

	/**
	 * The number of parameters in each configuration 
	 * (or the dimension of the parameter space).
	 */
	public static int dim;

	/**
	 * The parameter definition list.
	 */
	ArrayList<Parameter> parameters;
	/**
	 * The parameter value list.
	 */
	ArrayList<Object> values;
	
	private HashMap<CategoricalParameter, double[]> probMap;
	
	/**
	 * @param probMap the probMap to set
	 */
	public void setProbMap(HashMap<CategoricalParameter, double[]> probMap) {
		this.probMap = probMap;
	}

	/**
	 * The command line output of the configuration
	 */
	private String cmdOutput;

	/**
	 * 
	 */
	public Configuration(int dim) {
		Configuration.dim = dim;
		parameters = new ArrayList<Parameter>(dim);
		values = new ArrayList<Object>(dim);
	}

	/**
	 * Construct a configuration by an array of parameters, and an array of their values. 
	 * @param params an array of parameters. 
	 * @param vals an array of parameter values. 
	 */
	public Configuration(Parameter[] params, Object vals) {
		parameters = new ArrayList<Parameter>(Arrays.asList(params));
		dim = parameters.size();
		values = CollectionHandler.primitiveArray2List(vals);
		if (values == null) {
			values = parse(vals);
			if (values == null) {
				log.error("The input configuration is not valid: {}", vals);
				System.exit(3);
			} else {
				checkNumber();
			}
		}
		checkIntegers();
		checkBounds();
	}

	private ArrayList<Object> parse(Object vals) {
		if (vals instanceof String) {
			// configuration is given in format "param1 value1 param2 value2 ..."
			ArrayList<Object> list = new ArrayList<Object>(dim);
			for (int i = 0; i < dim; i++) {
				list.add(null);
			}
			String confString = (String) vals;
			StringTokenizer st;
			Parameter param;
			int index;
			String name;
			String val;
			
			for (int i = 0; i < dim; i++) {
				param = parameters.get(i);
				name = param.getRep();
				index = confString.indexOf(name);
				if (index < 0) {
					// may be conditional parameters
					log.warn("The input configuration does not contain value for parameter {}: {}", 
							param.toString(), confString);
					//return null;
				} else {
					st = new StringTokenizer(confString.substring(index + name.length()));
					val = st.nextToken();
					log.info("Parameter {} set with value {}", param, val);
					list.set(i, val);
				}
			}
			return list;
		}
		return null;
	}

	private void checkNumber() {
		Object obj;
		for (int i = 0; i < values.size(); i++) {
			obj = values.get(i);
			if (parameters.get(i).isNumerical() && obj instanceof String) {
				values.set(i, Double.valueOf((String) obj));
			}
		}
	}

	private void checkBounds() {
		Object value;
		Parameter param;
		double penalty;
		for (int i = 0; i < dim; i++) {
			value = values.get(i);
			param = parameters.get(i);
			if (param.isNumerical() && ! ((NumericalParameter)param).checkBound((Number)value)) {
				log.error("Configuration {} with parameter {} is out of bound", this, param);
				outOfBound = true;
			}
		}
	}

	public void checkIntegers() {
		Object value;
		for (int i = 0; i < dim; i++) {
			value = values.get(i);
			if (parameters.get(i).isInteger() && value instanceof Double) {
				values.set(i, new Integer((int)Math.round((Double)value)));
			}
		}
	}

	public void addParam(Parameter param, Object value) {
		parameters.add(param);
		if (param.isInteger() && value instanceof Double) {
			values.add(new Integer((int) Math.round((Double) value)));
		} else {
			values.add(value);
		}
	}
	
	public Parameter getParam(int index) {
		return parameters.get(index);
	}


	public Object getValue(int index) {
		return values.get(index);
	}

	public String toString() {
		if (cmdOutput == null) {
			StringBuffer s = new StringBuffer();
			Object value;
			for (int i = 0; i < dim; i++) {
				value = getValue(i);
				if (value != null) {
					s.append(getParam(i).getRep());
					s.append(value);
					s.append(" ");
				}
			}
			cmdOutput = s.toString();
		}
		return cmdOutput;
	}
	
	public boolean equals_bk(Object obj) {
		if (! (obj instanceof Configuration)) {
			return false;
		}
		Configuration conf = (Configuration) obj;
		for (int i = 0; i < dim; i++) {
			if (! (getParam(i).equals(conf.getParam(i)) 
					&& getValue(i).equals(conf.getValue(i)))) {
				return false;
			}
		}
		
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Configuration other = (Configuration) obj;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

	public Object[] getValueInArray() {
		return values.toArray();
	}

	/**
	 * Initialize the probability vector for categorical parameters by uniform distribution
	 */
	public void initCatProbMap() {
		probMap = new HashMap<CategoricalParameter, double[]>(dim);
		Parameter param;
		int numLevels;
		double prob;
		double[] probVector;
		Object value;
		
		for (int i = 0; i < dim; i++) {
			param = parameters.get(i);
			value = values.get(i);
			if (param.isCategorical() && value != null) {
				numLevels = ((CategoricalParameter) param).getNumValues();
				prob = 1.0 / numLevels;
				probVector = new double[numLevels];
				for (int j = 0; j < numLevels; j++) {
					probVector[j] = prob;
				}
				probMap.put((CategoricalParameter) param, probVector);
			}
		}
	}
	
	public double[] getProbVectorByParam(CategoricalParameter param) {
		if (probMap == null) {
			return null;
		} else {
			return probMap.get(param);
		}
	}

	private void addProbVectorForParam(CategoricalParameter param, double[] prob) {
		if (probMap == null) {
			probMap = new HashMap<CategoricalParameter, double[]>(dim);
		}
		probMap.put(param, prob);
	}

	public void addParam(CategoricalParameter param, String value, double[] prob) {
		addParam(param, value);
		addProbVectorForParam(param, prob);
	}

	public void nullifyConditional() {
		Parameter param, predecessor;
		Object predecessorValue;
		
		for (int i = 0; i < dim; i++) {
			param = parameters.get(i);
			
			if (param.isConditional()) {
				predecessor = param.getConditionalTo();
				predecessorValue = getParamValue(predecessor);
				if (! param.isConditionalActivated(predecessorValue)) {
					values.set(i, null);
					if (probMap != null) {
						probMap.remove(param);
					}
					
				}
			}
			
		}
	}

	public Object getParamValue(Parameter param) {
		for (int i = 0; i < dim; i++) {
			if (getParam(i).equals(param)) {
				return getValue(i);
			}
		}
		log.error("Parameter {} does not have a value.", param);
		return null;
	}
	
	/**
	 * scale a numerical configuration to [0, 1] value.  
	 * @return
	 */
	public double[] scale() {
		Parameter param;
		double[] range;
		double rangeLength;
		Number value;
		double[] scaled = new double[dim];
		
		for (int i = 0; i < dim; i++) {
			param = parameters.get(i);
			if (! (param instanceof NumericalParameter)) {
				return null;
			} else {
				range = ((NumericalParameter) param).getRange();
				rangeLength = range[1] - range[0];
				value = (Number) values.get(i);
				scaled[i] = (value.doubleValue() - range[0]) / rangeLength;
			}
		}
		return scaled;
	}

    public String ToJsonString() {
		StringBuilder sb = new StringBuilder("{");
		Parameter param;
		Object value;
		for (int i = 0; i < dim; i++) {
			value = getValue(i);
			if (value != null) {
				param = getParam(i);
				sb.append(param.toJsonString()).append(":");
				if (param.isCategorical()) {
					if (param.isBoolean()) {
						sb.append(((String)value).toLowerCase());
					} else {
						sb.append("\"").append(value).append("\"");
					}
				} else {
					sb.append(value);
				}
				if (i < dim - 1) {
					sb.append(",");
				} else {
					sb.append("}");
				}
			}
		}

		return sb.toString();
    }
}
