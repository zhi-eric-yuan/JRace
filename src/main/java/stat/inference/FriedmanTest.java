/*
 * Created by @user 
 */
package stat.inference;

import org.apache.commons.math.MathException;

/**
 * @author yuan
 * Created on Dec 1, 2011
 *
 */
public interface FriedmanTest {
	
	double friedman(double[][] x, final int k, final int m) throws IllegalArgumentException;

	double friedmanTest(double[][] x) throws IllegalArgumentException,
            MathException;

	double[][] getRanks();

	double getFriedmanStatistic();

	double[] getSumRanks();
	
	double getSumRankSquares();

	double getFriedmanT1();
	
}
