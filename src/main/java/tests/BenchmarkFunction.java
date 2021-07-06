/*
 * Created by Zhi Yuan
 */
package tests;

import java.util.Random;

import org.apache.commons.math.analysis.MultivariateFunction;
import org.apache.commons.math.util.FastMath;

import datahandler.OutputHandler;

/**
 * @author yuan
 * Created on Jun 19, 2012
 *
 */
public class BenchmarkFunction {

	/**
	 * 
	 */
	public BenchmarkFunction() {
	}

    public static class Sphere implements MultivariateFunction {

        public double value(double[] x) {
            double f = 0;
            for (int i = 0; i < x.length; ++i)
                f += x[i] * x[i];
            return f;
        }
    }

    public static class Cigar implements MultivariateFunction {
        private double factor;

        Cigar() {
            this(1e3);
        }

        Cigar(double axisratio) {
            factor = axisratio * axisratio;
        }

        public double value(double[] x) {
            double f = x[0] * x[0];
            for (int i = 1; i < x.length; ++i)
                f += factor * x[i] * x[i];
            return f;
        }
    }

    public static class Tablet implements MultivariateFunction {
        private double factor;

        Tablet() {
            this(1e3);
        }

        Tablet(double axisratio) {
            factor = axisratio * axisratio;
        }

        public double value(double[] x) {
            double f = factor * x[0] * x[0];
            for (int i = 1; i < x.length; ++i)
                f += x[i] * x[i];
            return f;
        }
    }

    public static class CigTab implements MultivariateFunction {
        private double factor;

        CigTab() {
            this(1e4);
        }

        CigTab(double axisratio) {
            factor = axisratio;
        }

        public double value(double[] x) {
            int end = x.length - 1;
            double f = x[0] * x[0] / factor + factor * x[end] * x[end];
            for (int i = 1; i < end; ++i)
                f += x[i] * x[i];
            return f;
        }
    }

    public static class TwoAxes implements MultivariateFunction {

        private double factor;

        TwoAxes() {
            this(1e6);
        }

        TwoAxes(double axisratio) {
            factor = axisratio * axisratio;
        }

        public double value(double[] x) {
            double f = 0;
            for (int i = 0; i < x.length; ++i)
                f += (i < x.length / 2 ? factor : 1) * x[i] * x[i];
            return f;
        }
    }

    public static class ElliRotated implements MultivariateFunction {
        private Basis B = new Basis();
        private double factor;

        ElliRotated() {
            this(1e3);
        }

        ElliRotated(double axisratio) {
            factor = axisratio * axisratio;
        }

        public double value(double[] x) {
            double f = 0;
            x = B.Rotate(x);
            for (int i = 0; i < x.length; ++i)
                f += Math.pow(factor, i / (x.length - 1.)) * x[i] * x[i];
            return f;
        }
    }

    public static class Elli implements MultivariateFunction {

        private double factor;
    	double[] shifts;
    	boolean needShift = false;
    	boolean needClamp = false;
    	double[][] boundaries;

        Elli() {
            this(1e3);
        }

        Elli(double axisratio) {
            factor = axisratio * axisratio;
        }

        Elli(double[] shifts) {
            this(shifts, null);
        }

        Elli(double[] shifts, double[][] boundaries) {
            this();
    		this.shifts = shifts;
    		if (shifts != null) {
        		needShift = true;
    		}
    		if (boundaries != null) {
                this.needClamp = true;
                this.boundaries = boundaries;
    		}
        }

        public double value(double[] x) {
        	// this really moves the x. 
        	if (needClamp) {
        		x = BoundHandler.clamp(x, boundaries);
        	}
        	
        	// this only change the x reference to a new array, x unchanged.
    		if (needShift) {
    			x = FunctionShift.shift(x, shifts);
    		}
    		
            double f = 0;
            for (int i = 0; i < x.length; ++i)
                f += Math.pow(factor, i / (x.length - 1.)) * x[i] * x[i];
            return f;
        }
    }

    public static class MinusElli implements MultivariateFunction {

        public double value(double[] x) {
            return 1.0-(new Elli().value(x));
        }
    }

    public static class DiffPow implements MultivariateFunction {

        public double value(double[] x) {
            double f = 0;
            for (int i = 0; i < x.length; ++i)
                f += Math.pow(Math.abs(x[i]), 2. + 10 * (double) i
                        / (x.length - 1.));
            return f;
        }
    }

    public static class SsDiffPow implements MultivariateFunction {

        public double value(double[] x) {
            double f = Math.pow(new DiffPow().value(x), 0.25);
            return f;
        }
    }

    public static class Rosen implements MultivariateFunction {

    	double[] shifts;
    	boolean needShift = false;
    	boolean noisy = false;
    	Noise noise;
    	
    	public Rosen() {
    		this(null);
    	}
    	
    	public Rosen(double[] shifts) {
    		this(shifts, 1, 0, 0);
    	}
    	
    	public Rosen(double[] shifts, double scale, double noiseLevel, long seed) {
    		this.shifts = shifts;
    		if (shifts != null) {
        		needShift = true;
    		}
    		if (scale != 1 && noiseLevel != 0) {
    			noisy = true;
    			Noise noise = new Noise(scale, noiseLevel);
    		}
    	}
    	
        public double value(double[] x) {
            //OutputHandler.writeArray(x);
    		if (needShift) {
    			x = FunctionShift.shift(x, shifts);
    		}
            //OutputHandler.writeArray(x);

            double f = 0;
            
            for (int i = 0; i < x.length - 1; ++i)
                f += 1e2 * (x[i] * x[i] - x[i + 1]) * (x[i] * x[i] - x[i + 1])
                + (x[i] - 1.) * (x[i] - 1.);
            
            if (noisy) {
            	f = noise.multiply(f);
            }
            return f;
        }
    }

    public static class RosenShift extends Rosen {
    	double[] shifts;
    	
    	public RosenShift(double[] shifts) {
    		this.shifts = shifts;
    	}

        public double value(double[] x) {
        	double[] shifted = FunctionShift.shift(x, shifts);
        	return super.value(shifted);
         }
    }
    
    public static class RosenShiftClamp extends RosenShift {
    	double [][] boundaries;
    	public RosenShiftClamp(double[] shifts, double[][] boundaries) {
    		super(shifts);
    		this.boundaries = boundaries;
    	}
        public double value(double[] x) {
        	x = BoundHandler.clamp(x, boundaries);
        	return super.value(x);
         }
    }
    
    public static Random random = new Random();
    
    private static class Noise {
    	double scale;
    	double level;
    	
    	public Noise(double scale, double level) {
    		this.scale = scale;
    		this.level = level;
    	}
    	
    	public double multiply(double value) {
    		return value * scale * FastMath.exp(random.nextGaussian() * level);
    	}
    }
    
    private static class BoundHandler {
        /**
         * Clamps
         * @param x Normalized objective variables.
         * @return the repaired objective variables - all in bounds.
         */
        public static double[] clamp(double[] x, final double[][] boundaries) {
            for (int i = 0; i < x.length; i++) {
                if (x[i] < boundaries[0][i]) {
                    x[i] = boundaries[0][i];
                } else if (x[i] > boundaries[1][i]) {
                    x[i] = boundaries[1][i];
                } else {
                    x[i] = x[i];
                }
            }
            return x;
        }
    }

    public static class Ackley implements MultivariateFunction {
        private double axisratio;

        Ackley(double axra) {
            axisratio = axra;
        }

        public Ackley() {
            this(1);
        }

        public double value(double[] x) {
            double f = 0;
            double res2 = 0;
            double fac = 0;
            for (int i = 0; i < x.length; ++i) {
                fac = Math.pow(axisratio, (i - 1.) / (x.length - 1.));
                f += fac * fac * x[i] * x[i];
                res2 += Math.cos(2. * Math.PI * fac * x[i]);
            }
            f = (20. - 20. * Math.exp(-0.2 * Math.sqrt(f / x.length))
                    + Math.exp(1.) - Math.exp(res2 / x.length));
            return f;
        }
    }

    public static class AckleyShift extends Ackley {
    	double[] shifts;
    	
    	public AckleyShift(double[] shifts) {
    		super();
    		this.shifts = shifts;
    	}

        public double value(double[] x) {
        	double[] shifted = FunctionShift.shift(x, shifts);
        	return super.value(shifted);
         }
    }

    public static class AckleyShiftClamp extends AckleyShift {
    	double [][] boundaries;
    	public AckleyShiftClamp(double[] shifts, double[][] boundaries) {
    		super(shifts);
    		this.boundaries = boundaries;
    	}
        public double value(double[] x) {
        	x = BoundHandler.clamp(x, boundaries);
        	return super.value(x);
         }
    }
    
    public static class Rastrigin implements MultivariateFunction {

        private double axisratio;
        private double amplitude;

        Rastrigin() {
            this(1, 10);
        }

        Rastrigin(double axisratio, double amplitude) {
            this.axisratio = axisratio;
            this.amplitude = amplitude;
        }

        public double value(double[] x) {
            double f = 0;
            double fac;
            for (int i = 0; i < x.length; ++i) {
                fac = Math.pow(axisratio, (i - 1.) / (x.length - 1.));
                if (i == 0 && x[i] < 0)
                    fac *= 1.;
                f += fac * fac * x[i] * x[i] + amplitude
                * (1. - Math.cos(2. * Math.PI * fac * x[i]));
            }
            return f;
        }
    }

    public static class RastriginShift extends Rastrigin {
    	double[] shifts;
    	
    	public RastriginShift(double[] shifts) {
    		super();
    		this.shifts = shifts;
    	}

        public double value(double[] x) {
        	double[] shifted = FunctionShift.shift(x, shifts);
        	return super.value(shifted);
         }
    }

    public static class RastriginShiftClamp extends RastriginShift {
    	double [][] boundaries;
    	public RastriginShiftClamp(double[] shifts, double[][] boundaries) {
    		super(shifts);
    		this.boundaries = boundaries;
    	}
        public double value(double[] x) {
        	x = BoundHandler.clamp(x, boundaries);
        	return super.value(x);
         }
    }

    public static class Griewank implements MultivariateFunction {

        public double value(double[] x) {
            double f = 1;
            double sum = 0;
            int n = x.length;
            
            for (int i = 0; i < n; i++) {
            	sum += x[i] * x[i];
            }
            
            f += sum / 4000.0;
            
            double product = 1;
            for (int i = 0; i < n; i++) {
            	product *= Math.cos(x[i] / (i+1));
            }
            
            f -= product;
            
            return f;
        }
    }

    public static class GriewankShift extends Griewank {
    	double[] shifts;
    	
    	public GriewankShift(double[] shifts) {
    		this.shifts = shifts;
    	}

        public double value(double[] x) {
        	double[] shifted = FunctionShift.shift(x, shifts);
        	return super.value(shifted);
         }
    }

    public static class GriewankShiftClamp extends GriewankShift {
    	double [][] boundaries;
    	public GriewankShiftClamp(double[] shifts, double[][] boundaries) {
    		super(shifts);
    		this.boundaries = boundaries;
    	}
        public double value(double[] x) {
        	x = BoundHandler.clamp(x, boundaries);
        	return super.value(x);
         }
    }

    private static class Basis {
        double[][] basis;
        Random rand = new Random(2); // use not always the same basis

        double[] Rotate(double[] x) {
            GenBasis(x.length);
            double[] y = new double[x.length];
            for (int i = 0; i < x.length; ++i) {
                y[i] = 0;
                for (int j = 0; j < x.length; ++j)
                    y[i] += basis[i][j] * x[j];
            }
            return y;
        }

        void GenBasis(int DIM) {
            if (basis != null ? basis.length == DIM : false)
                return;

            double sp;
            int i, j, k;

            /* generate orthogonal basis */
            basis = new double[DIM][DIM];
            for (i = 0; i < DIM; ++i) {
                /* sample components gaussian */
                for (j = 0; j < DIM; ++j)
                    basis[i][j] = rand.nextGaussian();
                /* substract projection of previous vectors */
                for (j = i - 1; j >= 0; --j) {
                    for (sp = 0., k = 0; k < DIM; ++k)
                        sp += basis[i][k] * basis[j][k]; /* scalar product */
                    for (k = 0; k < DIM; ++k)
                        basis[i][k] -= sp * basis[j][k]; /* substract */
                }
                /* normalize */
                for (sp = 0., k = 0; k < DIM; ++k)
                    sp += basis[i][k] * basis[i][k]; /* squared norm */
                for (k = 0; k < DIM; ++k)
                    basis[i][k] /= Math.sqrt(sp);
            }
        }
    }

    public static abstract class FunctionShift {
        public static double[] shift(final double[] x, final double[] shifts) {
        	double[] shifted = new double[x.length];
        	for (int i = 0; i < x.length; i++) {
        		shifted[i] = x[i] - shifts[i];
        	}
        	return shifted;
        }
    }

}
