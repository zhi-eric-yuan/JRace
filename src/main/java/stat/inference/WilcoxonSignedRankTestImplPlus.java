/*
 * Created by Zhi Yuan
 */
package stat.inference;

import org.apache.commons.math.stat.inference.WilcoxonSignedRankTestImpl;
import org.apache.commons.math.stat.ranking.NaNStrategy;
import org.apache.commons.math.stat.ranking.NaturalRanking;
import org.apache.commons.math.stat.ranking.TiesStrategy;
import org.apache.commons.math.util.FastMath;

/**
 * @author yuan
 * Created on Apr 9, 2012
 *
 */
public class WilcoxonSignedRankTestImplPlus extends WilcoxonSignedRankTestImpl {

    private NaturalRanking naturalRanking;

    /**
	 * 
	 */
	public WilcoxonSignedRankTestImplPlus() {
		naturalRanking = new NaturalRanking();
	}

	/**
	 * @param nanStrategy
	 * @param tiesStrategy
	 */
	public WilcoxonSignedRankTestImplPlus(NaNStrategy nanStrategy,
			TiesStrategy tiesStrategy) {
		naturalRanking = new NaturalRanking(nanStrategy, tiesStrategy);
	}
	
    /**
     * Ensures that the provided arrays fulfills the assumptions.
     *
     * @param x first sample
     * @param y second sample
     * @throws IllegalArgumentException
     *             if assumptions are not met
     */
    private void ensureDataConformance(final double[] x, final double[] y)
            throws IllegalArgumentException {
        if (x == null) {
            throw new IllegalArgumentException("x must not be null");
        }

        if (y == null) {
            throw new IllegalArgumentException("y must not be null");
        }

        if (x.length != y.length) {
            throw new IllegalArgumentException(
                    "x and y must contain the same number of elements");
        }

        if (x.length == 0) {
            throw new IllegalArgumentException(
                    "x and y must contain at least one element");
        }
    }

	/**
     * Calculates y[i] - x[i] for all i
     *
     * @param x first sample
     * @param y second sample
     * @return z = y - x
     */
    private double[] calculateDifferences(final double[] x, final double[] y) {

        final double[] z = new double[x.length];

        for (int i = 0; i < x.length; ++i) {
            z[i] = y[i] - x[i];
        }

        return z;
    }

    /**
     * Calculates |z[i]| for all i
     *
     * @param z sample
     * @return |z|
     * @throws IllegalArgumentException
     *             if assumptions are not met
     */
    private double[] calculateAbsoluteDifferences(final double[] z)
            throws IllegalArgumentException {
        if (z == null) {
            throw new IllegalArgumentException("z must not be null");
        }

        if (z.length == 0) {
            throw new IllegalArgumentException(
                    "z must contain at least one element");
        }

        final double[] zAbs = new double[z.length];

        for (int i = 0; i < z.length; ++i) {
            zAbs[i] = FastMath.abs(z[i]);
        }

        return zAbs;
    }
   
    /**
     * {@inheritDoc}
     *
     * @param x
     *            the first sample
     * @param y
     *            the second sample
     * @return wilcoxonSignedRank statistic (the larger of W+ and W-)
     * @throws IllegalArgumentException
     *             if preconditions are not met
     */
    public double wilcoxonSignedRank(final double[] x, final double[] y)
            throws IllegalArgumentException {
        return super.wilcoxonSignedRank(x, y);
    }

    /**
     * {@inheritDoc}
     *
     * @param x
     *            the first sample
     * @param y
     *            the second sample
     * @return wilcoxonSignedRank statistic (the larger of W+ and W-)
     * @throws IllegalArgumentException
     *             if preconditions are not met
     */
    public boolean wilcoxonSignedRankCmp(final double[] x, final double[] y)
            throws IllegalArgumentException {

        ensureDataConformance(x, y);

        // throws IllegalArgumentException if x and y are not correctly
        // specified
        final double[] z = calculateDifferences(x, y);
        final double[] zAbs = calculateAbsoluteDifferences(z);

        final double[] ranks = naturalRanking.rank(zAbs);

        double Wplus = 0;

        for (int i = 0; i < z.length; ++i) {
            if (z[i] > 0) {
                Wplus += ranks[i];
            }
        }

        final int N = x.length;
        final double Wminus = (((double) (N * (N + 1))) / 2.0) - Wplus;

        return Wplus >= Wminus;
    }


}
