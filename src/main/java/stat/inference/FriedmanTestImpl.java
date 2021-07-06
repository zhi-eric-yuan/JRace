/*
 * Created by @user 
 */
package stat.inference;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistribution;
import org.apache.commons.math.distribution.FDistribution;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.util.LocalizedFormats;
import org.apache.commons.math.stat.ranking.NaNStrategy;
import org.apache.commons.math.stat.ranking.NaturalRanking;
import org.apache.commons.math.stat.ranking.TiesStrategy;

/**
 * @author yuan
 * Created on Dec 1, 2011
 *
 */
public class FriedmanTestImpl implements FriedmanTest {

    private NaturalRanking naturalRanking;
    private double [][] ranks;
    private double friedmanStatistic;
    private double friedmanT1;
	private double friedmanT2;
	private double [] sumRanks;
	private double sumRankSquares;
	private boolean twoWay = false;
	private final double maxValue = 1e100;

	/**
	 * @return the friedmanT1
	 */
	public double getFriedmanT1() {
		return friedmanT1;
	}


	/**
	 * @return the sumRankSquares
	 */
	public double getSumRankSquares() {
		return sumRankSquares;
	}

	/**
	 * @return the sumRanks
	 */
	public double[] getSumRanks() {
		return sumRanks;
	}

	/**
	 * @return the friedmanStatistic
	 */
	public double getFriedmanStatistic() {
		return friedmanStatistic;
	}

	/**
	 * @return the ranks
	 */
	public double[][] getRanks() {
		return ranks;
	}

	/**
	 * 
	 */
    public FriedmanTestImpl() {
        naturalRanking = new NaturalRanking(NaNStrategy.FIXED,
                TiesStrategy.AVERAGE);
    }

	/**
	 * 
	 */
    public FriedmanTestImpl(boolean twoWay) {
        this();
    	this.twoWay = twoWay;
    }

	/* (non-Javadoc)
	 * @see stat.inference.FriedmanTest#friedman(double[], double[])
	 */
	@Override
	public double friedman(double[][] x, final int k, final int m)
			throws IllegalArgumentException {
		ensureDataConformance(x);
				
		ranks = new double[k][m];
		sumRanks = new double[m];
		
		for (int i = 0; i < k; i++) {
			ranks[i] = naturalRanking.rank(x[i]);
		}
		
		double sumUp = 0;
		double rankDiff;
		for (int i = 0; i < m; i++) {
			sumRanks[i] = 0;
			for (int j = 0; j < k; j++) {
				sumRanks[i] += ranks[j][i];
			}
			rankDiff = sumRanks[i] - k * (m + 1) / 2.0;
			sumUp += rankDiff * rankDiff;
		}
		
		sumRankSquares = 0;
		for (int i = 0; i < k; i++) {
			for (int j = 0; j < m ; j++) {
				sumRankSquares += ranks[i][j] * ranks[i][j];
			}
		}
		
		friedmanT1 = (m - 1) * sumUp / (sumRankSquares - k * m * (m + 1) * (m + 1) / 4.0);
		
		if (twoWay) {
			friedmanT2 = (k - 1) * friedmanT1 / (k * (m - 1) - friedmanT1);
			if (friedmanT2 == 1.0 / 0.0) {
				friedmanT2 = maxValue;
			}
			friedmanStatistic = friedmanT2;
		} else {
			friedmanStatistic = friedmanT1;
		}
			
		return friedmanStatistic;
	}
	
	/* (non-Javadoc)
	 * @see stat.inference.FriedmanTest#wilcoxonSignedRankTest(double[], double[], boolean)
	 */
	@Override
	public double friedmanTest(double[][] x) throws IllegalArgumentException, MathException {
		// the number of rows
		final int k = x.length;
		
		// the number of columns
		final int m = x[0].length;
		
		double statistic = friedman(x, k, m);
		double pValue;
		
		if (twoWay) {
			FDistribution fDist = new FDistribution(m - 1, (k - 1) * (m - 1));
			pValue = 1 - fDist.cumulativeProbability(friedmanT2);
		} else {
	        ChiSquaredDistribution distribution = new ChiSquaredDistribution(x[0].length - 1);
	        pValue = 1 - distribution.cumulativeProbability(friedmanT1);
		}
		
        return pValue;
	}

    /**
     * Ensures that the provided arrays fulfills the assumptions.
     *
     * @param x first sample
     * @param y second sample
     * @throws IllegalArgumentException
     *             if assumptions are not met
     */
    private void ensureDataConformance(final double[][] x)
            throws IllegalArgumentException {
        if (x == null) {
            throw new IllegalArgumentException("x must not be null");
        }

        if (x.length == 0) {
            throw new IllegalArgumentException(
                    "x and y must contain at least one element");
        }
        
        checkMatrix(x);
    }

	private void checkMatrix(double[][] x) {
		for (int i = 1; i < x.length; i++) {
			if (x[i].length != x[0].length) {
				throw new IllegalArgumentException(
						"data x with row index " + i + " has " + x[i].length + " != " + x[0].length + " elements ");
			}
		}
	}

}
