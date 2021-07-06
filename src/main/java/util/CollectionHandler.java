/*
 * Created by Eric Yuan on Feb 29, 2008
 */
package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author yuan
 * Created on Feb 29, 2008
 */
public class CollectionHandler {

	/**
	 * Create a new CollectionHandler.java object.
	 */
	public CollectionHandler() {
		// TODO Auto-generated constructor stub
	}

	public static double[] copyDoubleVectorIntoArray(Vector scores) {
		double [] a = new double[scores.size()];
		for (int i=0 ; i<scores.size() ; i++) {
			a[i] = Double.valueOf(scores.elementAt(i).toString());
		}
		return a;
	}
	
	public static Vector intersect(Vector v1, Vector v2) {
		Object obj;
		Vector v = new Vector();
		
		for (int i = 0; i < v1.size(); i++) {
			obj = v1.elementAt(i);
			if (v2.contains(obj)) {
				v.add(obj);
			}
			
		}
		
		return v;
	}
	
	public static Integer [] rank(final double [] data) {
		int len = data.length;
		final Integer[] order = new Integer [len];
		
		for (int i =0 ; i < len ; i++) {
			order[i] = new Integer(i);
		}

		Arrays.sort(order, new Comparator<Integer>() {
		    @Override public int compare(final Integer o1, final Integer o2) {
		        return Double.compare(data[o1], data[o2]);
		    }
		});

		return order;	
	}
	
	public static ArrayList<Object> primitiveArray2List(Object obj) {
		if (obj instanceof Object[]) {
			return new ArrayList<Object>(Arrays.asList(((Object[]) obj)));
		} else {
			if (obj instanceof double[]) {
				double[] array = (double[]) obj;
				int length = array.length;
				ArrayList<Object> list = new ArrayList<Object>(length); 
				for (double i : array) {
					list.add(i);
				}
				return list;
			} else if (obj instanceof int[]) {
				int[] array = (int[]) obj;
				int length = array.length;
				ArrayList<Object> list = new ArrayList<Object>(length); 
				for (int i : array) {
					list.add(i);
				}
				return list;
			}
		}
		return null;
	}
	
	public static int[] arrayList2Array(ArrayList<Integer> integers)
	{
	    int[] ret = new int[integers.size()];
	    Iterator<Integer> iterator = integers.iterator();
	    for (int i = 0; i < ret.length; i++)
	    {
	        ret[i] = iterator.next().intValue();
	    }
	    return ret;
	}

	public static ArrayList<Integer> newIntegerArrayList(int n) {
		ArrayList<Integer> a = new ArrayList<Integer>(n);
		for (int i = 0; i < n; i++) {
			a.add(i);
		}
		return a;
	}

	public static int[] newIntegerArray(int n) {
		int[] a = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = i;
		}
		return a;
	}

}
