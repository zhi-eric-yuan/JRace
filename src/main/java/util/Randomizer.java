package util;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import datahandler.OutputHandler;

/**Insert Type's description here.
 * <p>
 * @author Eric
 * <p>
 * Created on 07.08.2005
 */

public class Randomizer {

	public static Random random;
	
    /**
     * Constructs a new Randomizer object
     */
    public Randomizer () {
        super ();
    }
    
    public static void init() {
        random = new Random();
		initSeed();
    }
    public static void init(long seed) {
        random = new Random(seed);
		initSeed();
    }

	/**
	 * 
	 */
	private static void initSeed() {
		for (int i = 0; i < 100; i++) {
			nextDouble();
		}
	}

    /**
     * Generate random permutation from 0 .. n-1. 
     * @param n range
     * @return a random permutation
     */
	public static int[] generateRandomPermutation(int n){
		return generateRandomPermutation(n, n);
	}
	/**
	 * Generate m random ordered numbers from 0 .. n, without replacement. 
	 * @param n the range is 0 .. n
	 * @param m the number of picks, should be less or equal to n
	 * @return an m-element permutation from 0 .. n
	 */
	public static int[] generateRandomPermutation(int n, int m){
		int[] temp = new int[n];
		int[] perm = new int[m];
		
		//initialize temp with 1...n
		for(int i=0 ; i<n ; i++){
			temp[i] = i;
		}
		
		//generate random permutation, stored in perm
		for(int i=0 ; i<m ; i++){
			int pick = nextInt(n-i);
			perm[i] = temp[pick];
			
			for(int j=pick+1 ; j<n-i ; j++){
				temp[j-1] = temp[j];
			}
		}
/*
		//OutputHandler.debug = true;
		for (int i=0 ; i<m ; i++) {
			OutputHandler.write(perm[i]+" ");
		}
		//OutputHandler.debug = false;
*/		
		return perm;
	}

	/**
	 * Generate m random sequence from start .. end (included), without replacement. 
	 * @param n the range is 0 .. n
	 * @param m the number of picks, should be less or equal to n
	 * @return an m-element permutation from 0 .. n
	 */
	public static int[] generateRandomSequence(int start, int end){
		int n = end - start + 1;
		
		return generateRandomSequence(start, end, n);
	}

	/**
	 * Generate m random sequence from start .. end (included), without replacement. 
	 * @param n the range is 0 .. n
	 * @param m the number of picks, should be less or equal to n
	 * @return an m-element permutation from 0 .. n
	 */
	public static int[] generateRandomSequence(int start, int end, int num){
		int n = end - start + 1;
		int[] a = generateRandomPermutation(n, num);
		for (int i = 0; i < a.length; i++) {
			a[i] += start;
		}

		return a;
	}

	
	public static int nextInt(int n){
	    return random.nextInt(n);
	}
	
	/**
	 * Returns a pseudorandom, uniformly distributed int value 
	 * between <code>lower</code> (inclusive) and <code>upper</code> (exclusive).
	 * @param lower inclusive
	 * @param upper exclusive
	 * @return
	 */
	public static int nextInt(int lower, int upper) {
		return nextInt(upper - lower) + lower;
	}

	public static Object generateRandomItem (Vector v){
		int pos = nextInt(v.size());
		return v.elementAt(pos);
	}
	
	public static <E> E generateRandomItem(ArrayList<E> v){
		int pos = nextInt(v.size());
		return v.get(pos);
	}
	
	public static Vector generateRandomPermutation(Vector v) {
		Vector v2 = new Vector();
		int [] perm = generateRandomPermutation(v.size());
		for (int i=0 ; i<perm.length ; i++) {
			v2.addElement(v.elementAt(perm[i]));
		}
		return v2;
	}


	public static double nextDouble(){
	    return random.nextDouble();
	}

	public static double nextDouble(double lowerBound, double upperBound){
	    return lowerBound + (upperBound - lowerBound) * random.nextDouble();
	}

	/**
	 * Tossing a coin with a winning probability.
	 * @param wp the winning probability.
	 * @return true if wins, false elsewise.
	 */
	public static boolean tossCoin(double wp) {
		return ( nextDouble() < wp );
	}
	/**
	 * Tossing a coin with 50% winning probability.
	 * @return true if wins, false elsewise.
	 */
	public static boolean tossCoin() {
		return tossCoin(0.5);
	}
	
	public static int generateIndexByProb(double[] prob) {
		double val = nextDouble();
		double sum = 0;
		
		for (int i=0 ; i<prob.length ; i++) {
			sum += prob[i];
			if (val < sum) {
				return i;
			}
		}
		
		return prob.length - 1;
	}
	
	public static int[] generateIndicesByProb(int size, double[] prob) {
		int[] indices = new int[size];
		for (int i = 0; i < size; i++) {
			indices[i] = generateIndexByProb(prob);
		}
		return indices;
	}
	
	public static int[] generateCountsByProb(int size, double[] prob) {
		int numCategories = prob.length;
		int[] counts = new int[numCategories];
		
		for (int i = 0; i < size; i++) {
			counts[generateIndexByProb(prob)]++;
		}
		return counts;
	}
	
	public static double[] generateUniformRandomVector(int dim, double[][] boundaries) {
		double[] vector = new double[dim];
		for (int i = 0; i < dim; i++) {
			vector[i] = nextDouble(boundaries[0][i], boundaries[1][i]);
		}
		return vector;
	}
	
	public static void setSeed(long seed) {
		init(seed);
	}

	public static double nextGaussian() {
		return random.nextGaussian();
	}

	public static double nextGaussian(double mean, double sd) {
		return mean + sd * nextGaussian();
	}

}
