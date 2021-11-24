/*
 * Created by Zhi Yuan
 */
package algo;

import java.util.HashMap;

/**
 * @author yuan
 * Created on Apr 16, 2013
 *
 */
public class Parameter {
	protected Parameter conditionalTo;
	/**
	 * @return the conditionalTo
	 */
	public Parameter getConditionalTo() {
		return conditionalTo;
	}

	protected HashMap<Object, Boolean> conditionalValues;
	protected boolean conditional;
	/**
	 * @return the conditional
	 */
	public boolean isConditional() {
		return conditional;
	}

	/**
	 * Parameter name
	 */
	protected String name;

	public String getName() {
		return name;
	}

	/**
	 * Parameter representation, e.g. "--alpha= ". 
	 */
	protected String rep;
	/**
	 * @return the rep
	 */
	public String getRep() {
		return rep;
	}

	/**
	 * Parameter type, "r" for real-valued, "i" for integer-valued, "c" for categorical
	 */
	protected char type;

	/**
	 * @param type 
	 * @param rep 
	 * @param name 
	 * 
	 */
	public Parameter(String name, String rep, char type) {
		this.name = name;
		this.rep = rep;
		this.type = type;
	}

	public boolean isNumerical() {
		return type == 'r' || type == 'i';
	}

	public boolean isReal() {
		return type == 'r';
	}

	public boolean isInteger() {
		return type == 'i';
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(name);
		return sb.toString();
	}

	public String toJsonString() {
		return new StringBuilder("\"").append(name).append("\"").toString();
	}
	
	public boolean equals(Object obj) {
		return obj instanceof Parameter && ((Parameter)obj).name.equals(name);
	}

	public boolean isCategorical() {
		return type == 'c';
	}

	public void setConditional(Parameter parentParam, String[] values) {
		conditionalTo = parentParam;
		int numValues = values.length;
		conditional = true;
		conditionalValues = new HashMap<Object, Boolean>(numValues);
		for (int i = 0; i < numValues; i++) {
			conditionalValues.put(values[i], true);
		}
	}

	public boolean isConditionalActivated(Object value) {
		return conditionalValues.get(value) != null;
	}


	public boolean isBoolean() {
		return isCategorical() && this.isBoolean();
	}
}
