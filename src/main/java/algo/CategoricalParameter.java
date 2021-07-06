/*
 * Created by Zhi Yuan
 */
package algo;

/**
 * @author yuan
 * Created on Sep 12, 2013
 *
 */
public class CategoricalParameter extends Parameter {
	
	String [] values;
	String defaultValue;

	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	public CategoricalParameter(String name, String rep, char type, String [] values) {
		super(name, rep, type);
		this.values = values;
	}

	public CategoricalParameter(String name, String rep, char type, String [] values, String def) {
		super(name, rep, type);
		this.values = values;
		this.defaultValue = def;
	}

	public int getNumValues() {
		return values.length;
	}

	public String getValue(int index) {
		return values[index];
	}
	
	/**
	 * Given a categorical value, check its index in this categorical parameter. 
	 * @param value a categorical value in String
	 * @return its index in this categorical parameter. 
	 * Return -1 if it does not match any categorical values
	 */
	public int indexOfValue(String value) {
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(value)) {
				return i;
			}
		}
		return -1;
	}
}
