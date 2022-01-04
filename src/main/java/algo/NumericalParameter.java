/*
 * Created by Zhi Yuan
 */
package algo;

/**
 * @author yuan
 * Created on Apr 16, 2013
 *
 */
public class NumericalParameter extends Parameter {
	/**
	 * The range of the numerical parameter. Two-element array with {lower bound, upper bound}.
	 */
	protected transient double[] range;
	
	protected transient double defaultValue = Double.MIN_VALUE;

	/**
	 * @return the defaultValue
	 */
	public double getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @return the range
	 */
	public double[] getRange() {
		return range;
	}

	/**
	 * 
	 */
	public NumericalParameter(String name, String rep, char type, double [] range) {
		super(name, rep, type);
		this.range = range;
	}

	public NumericalParameter(String name, String rep, char type, double [] range, double def) {
		super(name, rep, type);
		this.range = range;
		this.defaultValue = def;
	}

	public boolean checkBound(Number value) {
		if (value == null) {
			return false;
		}
		if (value.doubleValue() < range[0] || value.doubleValue() > range[1]) {
			return false;
		}
		return true;
	}

}
