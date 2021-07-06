/*
 * Created by Zhi Yuan
 */
package util;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;

import org.apache.commons.math.util.FastMath;

/**
 * @author yuan
 * Created on Jul 29, 2013
 *
 */
public class MathHelp {

	/**
	 * 
	 */
	public MathHelp() {
	}

	public static double power(double a, double b) {
		long up = (long) Math.ceil(b);
		long low = (long) Math.floor(b);
		if (b == up) {
			return intPower(a, up);
		} else if (b == low) {
			return intPower(a, low);
		} else {
			return FastMath.pow(a, b);
		}
		
	}

	private static double intPower(double a, long b) {
		if (b < 0) {
			return 1 / intPower(a, -b);
		}
		char[] bits = Long.toBinaryString(b).toCharArray();
		double product = 1;
		double binPow = a;
		
		for (int i = bits.length - 1; i >= 0; i--) {
			if (bits[i] == '1') {
				product *= binPow;
			}
			binPow *= binPow;
		}
		return product;
		
	}
	
	public static double[] roundArrayToSignifDigit(double[] a, int n) {
		int dim = a.length;
		double[] rounded = new double[dim];
		for (int i = 0; i < dim; i++) {
			rounded[i] = roundToSignifDigit(a[i], n);
		}
		
		return rounded;
	}
	
	public static double[] roundArrayToDecimalPlace(double[] a, int n) {
		int dim = a.length;
		double[] rounded = new double[dim];
		for (int i = 0; i < dim; i++) {
			rounded[i] = roundToDecimalPlace(a[i], n);
		}
		
		return rounded;
	}

	/**
	 * Round a given double to a given decimal place n
	 * @param num number to round
	 * @param n the dicimal place
	 * @return rounded number
	 */
	public static double roundToDecimalPlace(double num, int n) {
		double tenPower = intPower(10, n);
	    return Math.round(num * tenPower) / tenPower;
	}


	public static double roundToSignifDigit(double num, int n) {
	    if(num == 0) {
	        return 0;
	    }

	    final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
	    final int power = n - (int) d;

	    final double magnitude = Math.pow(10, power);
	    final long shifted = Math.round(num * magnitude);
	    return shifted / magnitude;
	}
	
	public static double mean(double[] a) {
		double num = a.length;
		double sum = a[0];
		for (int i = 1; i < num; i++) {
			sum += a[i];
		}
		return sum / num;
	}

}
