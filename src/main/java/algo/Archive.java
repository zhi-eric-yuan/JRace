/*
 * Created by Zhi Yuan
 */
package algo;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author yuan
 * Created on Aug 24, 2013
 *
 */
public class Archive {
	
	protected HashMap<Configuration, HashMap<Instance, Double>> map = new 
			HashMap<Configuration, HashMap<Instance,Double>>();;
	protected int size;

	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * 
	 */
	public Archive() {
	}
	
	public Double get(Configuration configuration, Instance instance) {
		if (configuration == null || instance == null || map == null) {
			return null;
		}
		HashMap<Instance, Double> insMap = map.get(configuration);
		if (insMap == null) {
			return null;
		} else {
			return insMap.get(instance);
		}
	}
	
	public Double put(Configuration configuration, Instance instance, Double value) {
		if (configuration == null || instance == null || value == null) {
			return null;
		}
		size++;
		HashMap<Instance, Double> insMap = map.get(configuration);
		if (insMap == null) {
			insMap = new HashMap<Instance, Double>();
			insMap.put(instance, value);
			map.put(configuration, insMap);
			return null;
		} else {
			Double oldValue = insMap.get(instance);
			insMap.put(instance, value);
			map.put(configuration, insMap);
			return oldValue;
		}
		
	}
	
	public boolean containsConf(Configuration conf) {
		return map.containsKey(conf);
	}

	public boolean containsNewConf(ArrayList<Configuration> confs) {
		for (Configuration c : confs) {
			if (! containsConf(c)) {
				return true;
			}
		}
		return false;
	}

}
