/*
 * Created by Zhi Yuan 
 */
package race;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.TDistribution;
import org.apache.commons.math.linear.AbstractRealMatrix;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.inference.TestUtils;
import org.apache.commons.math.stat.inference.WilcoxonSignedRankTest;
import org.apache.commons.math.stat.ranking.NaNStrategy;
import org.apache.commons.math.stat.ranking.NaturalRanking;
import org.apache.commons.math.stat.ranking.TiesStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stat.inference.FriedmanTest;
import stat.inference.FriedmanTestImpl;
import stat.inference.WilcoxonSignedRankTestImplPlus;
import util.CollectionHandler;
import util.SystemProperty;
import datahandler.OutputHandler;
import eval.AlgorithmEvaluator;
import eval.Evaluator;

/**
 * @author yuan
 * Created on Dec 1, 2011
 *
 */
public class Race {

	private final Logger log = LoggerFactory.getLogger(getClass());
	public static boolean interactive = 
		SystemProperty.get(SystemProperty.INTERACTIVE).equalsIgnoreCase("true");
	//public static int maxExp = Integer.parseInt(SystemProperty.get(SystemProperty.maxExp));
	private int maxExp;
	private AbstractRealMatrix results;
	/**
	 * @return the results
	 */
	public AbstractRealMatrix getResults() {
		return results;
	}

	public int EvaluationCounter = 0;
	private int numCandidates;
	private int numInstances;
	private Evaluator evaluator;
	private int firstTest = 5;
	private boolean isFRace;
	private boolean isTnoRace;
	private boolean [] alive;
	private int [] rowIndices;
	private int [] survivorIndices;
	private double signifLevel = 0.05;
	private int numAlive;
	private int numAliveAtCut;
	private double[] scores;

	private int stopMinCand = 1;
	private int plusNumCand = SystemProperty.getInteger(SystemProperty.PLUS_NUM_CAND, 1);
	private int bestIndex = -1;
	/**
	 * The mean value for each currently alive candidate.
	 */
	private double[] meanCandidates;
	/**
	 * The order of candidates currently alive, indexed from 0...numAlive. 
	 * The real index of the candidates should be survivorIndices[orders[i]]. 
	 */
	private Integer [] orders;
	private double bestRankSum;
	private double bestMean;
	private int numTasks;
	private int numExp;

	/**
	 * Used in irace. TODO should figure out where to place this method
	 * @param numElites number of elites
	 * @return the elite indices
	 */
	public int [] getElitesInOrder(int numElites) {
		int [] elites = new int[numElites];
		RealMatrix data = results.getSubMatrix(createIncrementalArray(numTasks), 
				survivorIndices);
		int numCandi = survivorIndices.length;
		double[] scores = new double[numCandi];
		
		if (isFRace) {
			double[][] ranks = new double[numTasks][numCandi];
			NaturalRanking nr = new NaturalRanking(NaNStrategy.FIXED, TiesStrategy.AVERAGE);
			
			for (int i = 0; i < numTasks; i++) {
				ranks[i] = nr.rank(data.getRow(i));
				for (int j = 0; j < numCandi; j++) {
					scores[j] += ranks[i][j];
				}
			}
		} else {
			for (int i = 0; i < numCandi; i++) {
				scores[i] = StatUtils.mean(data.getColumn(i));
			}
		}
		
		orders = CollectionHandler.rank(scores);
		for (int i = 0; i < numElites; i++) {
			elites[i] = survivorIndices[orders[i]];
		}
		return elites;
	}
	
	/**
	 * @return the numExp
	 */
	public int getNumExp() {
		return numExp;
	}

	/**
	 * 
	 */
	public Race(int numCandidates, int numInstances, Evaluator evaluator, int maxExp) {
		this(numCandidates, numInstances, evaluator, maxExp, true);
	}
	
	public Race(int numCandidates, int numInstances, Evaluator evaluator, int maxExp, 
			boolean isFriedman) {
		this(numCandidates, numInstances, evaluator, maxExp, isFriedman, 5);
	}
	
	public Race(int numCandidates, int numInstances, Evaluator evaluator, int maxExp, 
			boolean isFriedman, int firstTest) {
		this.numCandidates = numCandidates;
		this.numInstances = numInstances;
		this.evaluator = evaluator;
		this.maxExp = maxExp;
		alive = new boolean [numCandidates];
		numAlive = numCandidates;
		isFRace = isFriedman;
		isTnoRace = ! isFriedman;
		if (isFRace) {
			OutputHandler.writeln("F-Race is used.");
		} else {
			OutputHandler.writeln("tNo-Race is used.");
		}
		
		this.firstTest = firstTest;
		/*if (evaluator instanceof AlgorithmEvaluator) {
			((AlgorithmEvaluator)evaluator).initConfigurations();
		}*/
	}

	public Race(int numCandidates, int numInstances, Evaluator evaluator, int maxExp, 
			boolean isFriedman, int firstTest, int stopMinCan) {
		this(numCandidates, numInstances, evaluator, maxExp, isFriedman, firstTest);
		this.stopMinCand = stopMinCan;
	}

	public int race() {
		initRace();
		RealMatrix dataForTest;
		int iterationNumExp = numAlive;
		
		for (int i = 0; i < numInstances; i++) {
			// Note: use a loose stopping criterion "numExp + min{numAlive, iterationNumExp} > maxExp". 
			// Some evaluations might be archived.  
			// The total number of evaluations is rarely possible to exceed the maximum amount. 
			if ((i >= firstTest - 1 && survivorIndices.length <= stopMinCand) 
					|| numExp >= maxExp 
					//|| numExp + numAlive > maxExp) {
					|| numExp + Math.min(numAlive, iterationNumExp) > maxExp) {
				if (i == 0) {
					log.error("Cannot start race with budget {} for {} candidates", maxExp, numAlive);
					break;
				}
				int numSurvive = survivorIndices.length;
				if (plusNumCand > 1 && numAlive > 1 && maxExp - numExp >= plusNumCand) {
					if (numSurvive >= plusNumCand) {
						numAliveAtCut = Math.max(numAliveAtCut, numAlive);
						for (int j = plusNumCand; j < numSurvive; j++) {
							alive[survivorIndices[orders[j]]] = false;
						}
						updateSurvivors();
						//updateOrders();
					}
				} else {
					break;
				}
			}
			numTasks = i + 1;
			iterationNumExp = 0;
			for (int j = 0; j < numAlive; j++) {
				evaluateEntry(i, survivorIndices[j]);
				
				if (evaluator instanceof AlgorithmEvaluator) {
					iterationNumExp += ((AlgorithmEvaluator)evaluator).getNumExp();
				} else {
					iterationNumExp++;
				}
				
				if (numExp + iterationNumExp > maxExp) {
					log.error("The total number of evaluations has exceeds the maximum budget {}", 
							maxExp);
					break;
				}
			}
			numExp += iterationNumExp;
			if (numExp > maxExp) {
				break;
			}
			if (i >= firstTest - 1) {
				dataForTest = results.getSubMatrix(createIncrementalArray(i + 1), survivorIndices);
				if (isFRace) {
					fRace(dataForTest);
				} else if (isTnoRace) {
					tNoRace(dataForTest);
				}
			} else {
				// no test is performed yet
				if (interactive) {
					System.out.print("|x|");
				}
				double[] scores = null;
				if (isFRace) {
					scores = computeRankSum();
				} else if (isTnoRace) {
					scores = computeMean();
				}
				idIncumbent(scores);
			}
			if (interactive) {
				System.out.print(String.format("%11d", numTasks) + "|");
				System.out.print(String.format("%11d", numAlive) + "|");
				System.out.print(String.format("%11d", (bestIndex + 1)) + "|");
				System.out.print(String.format("%15f", bestMean) + "|");
				System.out.print(String.format("%11d", numExp) + "|\n");
			}
			//OutputHandler.writeln(survivorIndices.length);
		}
		System.out.print("+-+-----------+-----------+-----------+---------------+-----------+\n\n"
				+ "Selected candidate:" + String.format("%12d", (bestIndex + 1))
				+ "\tmean value: " + String.format("%11f", bestMean) + "\n\n");
		numAlive = Math.max(numAlive, numAliveAtCut);
		return bestIndex;
	}
	
	public int[] computeCandidateOrders() {
		scores = computeMean(numTasks, createIncrementalArray(numCandidates));
		Integer[] candOrders = CollectionHandler.rank(scores);
		int[] order = new int[numCandidates];
		
		for (int i = 0; i < numCandidates; i++) {
			order[i] = candOrders[i];
		}
		return order;
	}

	private double[] computeMean() {
		return computeMean(numTasks, createIncrementalArray(numAlive));
	}

	private double[] computeMean(int numTasks, int[] candidates) {
		int numCandidates = candidates.length;
		double [] means = new double[numCandidates];
		double [] sums = new double[numCandidates];
		
		//for (int j = 0; j < numCandidates; j++) {
		for (int j = 0; j < numCandidates; j++) {
			for (int i = 0; i < numTasks; i++) {
				sums[j] += results.getEntry(i, candidates[j]);
			}
			means[j] = sums[j] / numTasks;
		}
		return means;
	}

	private void tNoRace(RealMatrix dataForTest) {
		double pValue = 1;
		int numIns = dataForTest.getRowDimension();
		int numCan = dataForTest.getColumnDimension();
		double[][] dataCandi = new double[numCan][numIns];
		double[] sumCandi = new double[numCan];
		meanCandidates = new double[numCan];
		double bestSum = Double.MAX_VALUE;
		int minIndex = -1;
		int index;
		boolean dropAny = false;
		
		for (int i = 0; i < numCan; i++) {
			dataCandi[i] = dataForTest.getColumn(i);
			for (int j = 0; j < numIns; j++) {
				sumCandi[i] += dataCandi[i][j];
			}
			if (sumCandi[i] < bestSum) {
				bestSum = sumCandi[i];
				minIndex = i;
			}
			meanCandidates[i] = sumCandi[i] / numTasks;
		}
		/*int bestCandiIndex = survivorIndices[index];
		OutputHandler.writeln("Best candidate " + bestCandiIndex + " with mean " 
				+ (bestSum / numInstances));*/
		idIncumbent(meanCandidates);

		for (int i = 1; i < numCan; i++) {
			index = orders[i];
			try {
				pValue = TestUtils.pairedTTest(dataCandi[minIndex], dataCandi[index]);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MathException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			OutputHandler.writeln("candidate " + (index + 1) + " p value: " + pValue);
			if (! Double.isNaN(pValue) && pValue < signifLevel) {
				for (int j = i ; j < numCan ; j++) {
					alive[survivorIndices[orders[j]]] = false;
				}
				dropAny = true;
				updateSurvivors();
				break;
				
			}
		}
		
		if (interactive) {
			if (dropAny) {
				System.out.print("|-|");
			} else {
				System.out.print("|=|");
			}
		} 

	}

	/**
	 * @param dataForTest
	 * @param pValue
	 * @return
	 */
	private double fRace(RealMatrix dataForTest) {
		double pValue = 1;
		double[] dataCandi1;
		double[] dataCandi2;
		FriedmanTest ft;
		WilcoxonSignedRankTest wt;
		if (numAlive == 2) {
			// only 2 candidates left, use wilcoxon signed rank test
			wt = new WilcoxonSignedRankTestImplPlus();
			dataCandi1 = dataForTest.getColumn(0);
			dataCandi2 = dataForTest.getColumn(1);
			try {
				pValue = wt.wilcoxonSignedRankTest(dataCandi1, dataCandi2, false);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (MathException e) {
				e.printStackTrace();
			}
			OutputHandler.writeln("Wilcox p-value: " + pValue);
			boolean better1 = ((WilcoxonSignedRankTestImplPlus)wt).wilcoxonSignedRankCmp(
					dataCandi1, dataCandi2);
			
			if (! Double.isNaN(pValue) && pValue < signifLevel) {
				if (interactive) {
					System.out.print("|-|");
				}
				if (better1) {
					alive[survivorIndices[1]] = false;
				} else {
					alive[survivorIndices[0]] = false;
				}
				updateSurvivors();
				bestIndex = survivorIndices[0];
			} else {
				// Wilcoxon null hypothesis not rejected 
				if (interactive) {
					System.out.print("|=|");
				}
				
				if (better1) {
					bestIndex = survivorIndices[0];
				} else {
					bestIndex = survivorIndices[1];
				}
			}
			
			if (better1) {
				bestMean = StatUtils.mean(dataCandi1);
			} else {
				bestMean = StatUtils.mean(dataCandi2);
			}

		} else {
			// use friedman test
			ft = new FriedmanTestImpl();
			//ft = new FriedmanTestImpl(true);
			try {
				pValue = ft.friedmanTest(dataForTest.getData());
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MathException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			idIncumbent(ft.getSumRanks());
			OutputHandler.writeln("Friedman P Value: " + pValue);
			if (! Double.isNaN(pValue) && pValue < signifLevel) {
				if (interactive) {
					System.out.print("|-|");
				}
				friedmanPostTest(ft);
				updateSurvivors();
			} else {
				// Don't discard anything
				if (interactive) {
					System.out.print("|=|");
				}
			}
		}
		return pValue;
	}

	/**
	 * Compute rank sum only when none of the candidates are eliminated from race. 
	 * @return the rank sum
	 */
	private double[] computeRankSum() {
		NaturalRanking nr = new NaturalRanking();
		double [][] ranks = new double[numTasks][numCandidates];
		double [] rankSums = new double[numCandidates];
		for (int j = 0; j < numTasks; j++) {
			ranks[j] = nr.rank(results.getRow(j));
			for (int k = 0 ; k < numCandidates ; k++) {
				rankSums[k] += ranks[j][k];
			}
		}
		return rankSums;
	}

	private void updateSurvivors() {
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < numCandidates; i++) {
			if (alive[i]) {
				list.add(new Integer(i));
			}
		}
		
		survivorIndices = new int[list.size()];
		Iterator<Integer> itr = list.iterator();
	    for (int i = 0; itr.hasNext(); i++) {
	    	survivorIndices[i] = itr.next().intValue();
	    }
	    
	    numAlive = survivorIndices.length;
	    //XXX must also change for f-race
	    double[] scores = computeMean(numTasks, survivorIndices);
	    idIncumbent(scores);

	}

	private void friedmanPostTest(FriedmanTest ft) {
		final double [][] ranks = ft.getRanks();
		final int k = ranks.length;
		final int m = ranks[0].length;
		final double friedmanT1 = ft.getFriedmanT1();
		final double [] sumRanks = ft.getSumRanks();
		final double sumRankSquares = ft.getSumRankSquares();
		double up;
		double pValue;
		int index;

		for (int i = 1; i < m; i++) {
			index = orders[i];
			
			up = sumRanks[index] - bestRankSum;
			pValue = up / Math.sqrt(2.0 * k * (1.0 - friedmanT1 / k / (m - 1.0))
					* (sumRankSquares - k * m * (m + 1) * (m + 1) / 4.0) 
					/ (k - 1.0) / (m - 1.0));
			TDistribution tDist = new TDistribution((k - 1) * (m - 1));
			double threshold = tDist.inverseCumulativeProbability(1 - signifLevel / 2.0);
			OutputHandler.writeln("candidate " + (orders[i] + 1) + " p value: " + pValue);
			OutputHandler.writeln("candidate " + (orders[i] + 1) + " root: " + (2 * k * (1 - friedmanT1 / k / (m - 1))
					* (sumRankSquares - k * m * (m + 1) * (m + 1) / 4) 
					/ (k - 1) / (m - 1)));
			OutputHandler.writeln("candidate " + (orders[i] + 1) + " t test threshold: " 
					+ threshold);
			if (pValue > threshold) {
				// Significantly worse candidate identified. 
				// All the candidates hereafter should be discarded.
				for (int j = i ; j < m ; j++) {
					alive[survivorIndices[orders[j]]] = false;
				}
				break;
			}
		}
	}

	/**
	 * @param scores in the case of F-Race, the score is the sum of ranks; 
	 * in the case of tRace, the score is the mean (or sum) of evaluation values.  
	 */
	private void idIncumbent(final double[] scores) {
		orders = CollectionHandler.rank(scores);
		OutputHandler.writeln("Scores for candidates: " + Arrays.toString(scores));
		OutputHandler.writeln("Order of candidates: " + Arrays.toString(orders));
		int index = orders[0];
		bestIndex = survivorIndices[index];
		if (isFRace) {
			bestRankSum = scores[index];
		}
		bestMean = StatUtils.mean(results.getColumn(bestIndex), 0, numTasks);
	}

	private void initRace() {
		if (interactive) {
			System.out.println("Statistical racing for selection of the best.");
			System.out.println("Java implementation by Zhi Eric Yuan, ");
			System.out.println("according to the R implementation by Mauro Birattari.");
			System.out.println("This software comes with ABSOLUTELY NO WARRANTY\n");
			System.out.print("                                Markers:                           \n"
			       + "                                   x No test is performed.         \n"
			       + "                                   - The test is performed and     \n"
			       + "                                     some candidates are discarded.\n" 
			       + "                                   = The test is performed but     \n"
			       + "                                     no candidate is discarded.    \n"
			       + "                                                                   \n"
			       + "                                                                   \n"
			       + "+-+-----------+-----------+-----------+---------------+-----------+\n"
			       + "| |       Task|      Alive|       Best|      Mean best| Exp so far|\n"
			       + "+-+-----------+-----------+-----------+---------------+-----------+\n");			
		}
		for (int i = 0; i < numCandidates; i++) {
			alive[i] = true;
		}
		
		survivorIndices = new int[numCandidates];
		for (int i = 0; i < numCandidates; i++) {
			survivorIndices[i] = i;
		}
				
		results = new Array2DRowRealMatrix(numInstances, numCandidates);
		initResults(1e100);

	}

	/**
	 * initialize the result entries to a large value, instead of 0 by default
	 * @param d the initial large value of the result matrix
	 */
	private void initResults(double d) {
		for (int i = 0; i < numInstances; i++) {
			for (int j = 0; j < numCandidates; j++) {
				results.setEntry(i, j, d);
			}
		}
	}

	/**
	 * Create an incremental array from 0 to n-1. 
	 * @param n size of the array
	 * @return an incremental array
	 */
	private int [] createIncrementalArray(int n) {
		int [] a = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = i;
		}
		return a;
	}

	private void evaluateEntry(int row, int col) {
		results.setEntry(row, col, evaluator.evaluate(row, col));
	}
	/**
	 * @return the numAlive
	 */
	public int getNumAlive() {
		return numAlive;
	}

	/**
	 * @return the scores
	 */
	public double[] getScores() {
		return scores;
	}

}
