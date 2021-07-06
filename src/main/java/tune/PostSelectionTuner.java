package tune;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math.analysis.MultivariateFunction;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.direct.BaseAbstractScalarOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.CollectionHandler;
import util.Randomizer;
import util.SystemProperty;
import algo.Configuration;
import algo.Instance;
import algo.NumericalParameter;
import algo.Parameter;
import eval.AlgorithmEvaluator;

public abstract class PostSelectionTuner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * lambda = 4 + (int) (a ln(D))
	 */
	public static final double a = SystemProperty.getDouble(SystemProperty.A, 3.0);
	//private static final double a = SystemProperty.getDouble(SystemProperty.A, 1.0);
	protected BaseAbstractScalarOptimizer<MultivariateFunction> optim;
	/**
	 * Original parameter boundary
	 */
	protected double[][] boundaries;
	public int numEval = SystemProperty.getInteger(SystemProperty.NUM_EVAL, 1);
	public int maxNumEval = 5;
	/**
	 * Lower boundaries with 0
	 */
	protected double[] lowers;
	/**
	 * Upper boundaries with 1
	 */
	protected double[] uppers;
	protected double[] starts;
	protected ArrayList<Configuration> elites;
	public ArrayList<Configuration> restartElites;
	Instance[] instances;
	protected AlgorithmEvaluator eval;
	public Parameter[] params;
	public int budget;
	public int instanceIndex;
	protected int restEval;
	protected int dim;
	protected int lambda;
	protected int lambda0;

	protected boolean firstPostSelectionFinished = false;
	
	protected boolean incrementInstance = false;
	public boolean addEvalHistBest = SystemProperty.getBoolean(SystemProperty.EVAL_HIST_BEST, false);
	protected boolean qualifyMultiElites = SystemProperty.getBoolean(SystemProperty.MULTI_ELITES, 
			false);
	protected boolean useFloor = true;
	
	protected int numElitePerQuali = 1;
	protected final int maxEvalInstances = 5;
	protected int minNewCan;
	protected int numElite2add;
	protected boolean shouldStop = false;
	protected int minBud;
	public int numAddEval;

	public PostSelectionTuner() {
		super();
	}

	/**
	 * @param params
	 */
	protected void initTuner(Parameter[] params, int budget, Instance[] instances) {
		this.instances = instances;
		this.params = params;
		this.budget = budget;
		elites = new ArrayList<Configuration>();
		restartElites = new ArrayList<Configuration>();
		initParams();
		eval = new AlgorithmEvaluator(params, boundaries);
		initSearch(dim);
		log.info("Additional evaluation for historical best? {}", addEvalHistBest);
		log.info("Qualify multiple elites? {}", qualifyMultiElites);

	}

	/**
	 * Initialize the search method. Note that this search setting stays across all restarts.  
	 * @param dim
	 */
	protected abstract void initSearch(int dim);

	/**
	 * @param params
	 * @return
	 */
	private int initParams() {
		dim = params.length;
		//lambda0 = 4 + (int) (a * 3.0 * Math.log(dim));
		//lambda0 = 4 + (int) (a * Math.log(dim));
		//lambda0 = determineLambdaByFactor(a);
		lambda0 = determineLambdaByFactor(Math.min(a, 3.0));
		try {
			checkParamNumerical(dim);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		createBoundaries(dim, params);
		lowers = new double[dim];
		uppers = new double[dim];
		for (int i = 0; i < dim; i++) {
			lowers[i] = 0;
			uppers[i] = 1;
		}
		randomStart(dim);
		if (qualifyMultiElites) {
			if (useFloor) {
				numElitePerQuali = dim / 2;
			} else {
				numElitePerQuali = (int) Math.ceil(dim / 2.);
			}
			log.info("dimension {} number of elites per quali {}", dim, numElitePerQuali);
			if (numElitePerQuali > 1) {
				// reserve evaluations for post-selection
				restEval = countRestEval();
			}
		}
		log.info("dimension {}, lambda factor a {}, lambda0 {}", dim, a, lambda0);
		return dim;
	}

	protected double[] randomStart() {
		return randomStart(dim);
	}
	/**
	 * @param dim
	 */
	protected double[] randomStart(int dim) {
		starts = new double[dim];
		for (int i = 0; i < dim; i++) {
			starts[i] = Randomizer.nextDouble();
		}
		return starts;
	}

	private void checkParamNumerical(int dim)
			throws Exception {
				for (int i = 0; i < dim; i++) {
					if (! params[i].isNumerical()) {
						throw new Exception("Parameter " + params[i].toString() 
								+ " is not numerical for BOBYQA.");
					}
				}
			}

	/**
	 * @param dim
	 * @param bound
	 * @return
	 */
	private double[][] createBoundaries(int dim, Parameter[] params) {
		boundaries = new double[2][dim];
		NumericalParameter param;
		double[] range;
		
		for (int i = 0; i < dim; i++) {
			param = (NumericalParameter) params[i];
			range = param.getRange();
			boundaries[0][i] = range[0];
			boundaries[1][i] = range[1];
		}
		return boundaries;
	}

	/**
	 * @param params
	 * @param budget
	 * @param instances
	 * @param bt
	 */
	public Configuration tune(Parameter[] params, int budget, Instance[] instances) {
		// although initTuner set the random start points, it is usually reset in each oneQualification
		initTuner(params, budget, instances);
		restEval = budget;
		numAddEval = determineNumAddEval();
		minNewCan = determineMinNewCan();
		int iterationCount = 1;
		Configuration bestConf;
		
		do {
			if (iterationCount != 1 && this instanceof CmaesTuner) {
				// a restart, reset cmaes with smaller lambda, without population adaptation
				((CmaesTuner)this).d = 1;
				iterationCount = 1;
			}
			instanceIndex = 0;
			minBud = determineMinBud(iterationCount);
			while (restEval >= minBud && ! shouldStop) {
				// One restart iteration
				log.info("Minimum number of new candidates {}, number of evaluation {}, min budget {}", 
						minNewCan, numEval, minBud);
				numElite2add = determineNumElitePerQual(iterationCount);
				oneQualification(restEval / numEval, iterationCount);
		
				iterationCount++;
				minBud = determineMinBud(iterationCount);
								
				checkConsecutiveArchiveRead();
				checkIterationCount(iterationCount);
			}
			
			addRestartElites();
			bestConf = postSelect(instances, elites.size());
			int used = getUsedBudget();
			restEval = budget - used;
			log.info("Post-selection tuner finishes. {} evaluations used, {} left.", used, restEval);
			firstPostSelectionFinished = true;
		} while (restEval >= 11 * numEval * (lambda0 + 20 + (this instanceof SimplexTuner? 5 * dim : 0) 
				+ (this instanceof BobyqaTuner? 2 * dim : 0)) + getNumElites() 
				&& ! shouldStop);//bobyqa may use more than lambda evaluations, use 11.
		
		return bestConf;
	}

	private void checkIterationCount(int iterationCount) {
		if (iterationCount > budget * 2) {
			log.error("Iteration counter {} greater than budget {}, probably something wrong", 
					iterationCount, budget);
			shouldStop = true;
		}
	}

	protected abstract int determineNumAddEval();

	/**
	 * Procedure after one qualification iteration
	 * @param minBud minimum budget
	 * @param iterationCount iteration counter (after +1)
	 * @return minimum budget
	 */
	protected int determineMinBud(int iterationCount) {
		if (incrementInstance) {
			numEval = determineNumInstances(iterationCount);
		}
		minBud = minNewCan * numEval;
		// number of additional evaluation per iteration
		//minBud += numAddEval;
		return minBud;
	}

	/**
	 * If the consecutive archive read is too large (> 10000), which is usually due to error in search, 
	 * stop the tuning process. 
	 * @return whether the tuning process should be stopped.
	 */
	private void checkConsecutiveArchiveRead() {
		int numConsRead = eval.getNumConsecutiveArchiveRead();
		if (numConsRead > 10000) {
			log.error("The tuning process should be stopped due to {} consective archive reads", 
					numConsRead);
			shouldStop = true;
		}
	}

	/**
	 * @param numElite2add
	 */
	protected void addElites() {
		addElites(numElite2add);
	}
	
	/**
	 * @param numElite2add
	 */
	protected void addElites(int numElite2add) {
		if (addEvalHistBest) {
			ArrayList<double[]> historicalBest = optim.historicalBest;
			int numHistoricalBest = historicalBest.size();
			log.info("{} historical best, {} best over time", numHistoricalBest, 
					optim.bestOverTime.size());
			if (numHistoricalBest == 0) {
				// no best except the initial building points of the simplex
				addElite();
			} else {
				int rest = countRestEval();
				// rest may be negative, clamp to 0
				rest = Math.max(rest, 0);
				int numSecond = Math.min(numHistoricalBest, rest);
				// numSecond can be 0, clamp to at least one elite to be added
				numSecond = Math.max(numSecond, 1);
				int smallestIndex = numHistoricalBest - numSecond;
				log.info("{} budget left, {} evaluations for second qualification", rest, numSecond);
				if (numSecond <= numElite2add) {
					// Second qualification evaluations less than or equal the required number of elites  
					//int smallestIndex = Math.max(0, numSecond - numElitePerQuali);
					for (int i = numHistoricalBest - 1; i >= smallestIndex; i--) {
						addElite(historicalBest.get(i));
					}
				} else {
					// More candidates than elites, need second qualification
					setAdditionalInstance();
					double[] values = new double[numHistoricalBest];
					for (int i = 0; i < smallestIndex; i++) {
						values[i] = Double.MAX_VALUE;
					}
					for (int i = smallestIndex; i < numHistoricalBest; i++) {
						values[i] = eval.value(historicalBest.get(i)) + optim.bestOverTime.get(i)[1];
					}
					log.info("evaluations after second qualification: {}", Arrays.toString(values));
					Integer[] order = CollectionHandler.rank(values);
					log.info("ranks after second qualification: {}", Arrays.toString(order));
					for (int i = 0; i < numElite2add; i++) {
						addElite(historicalBest.get(order[i]));
					}
				}
			}
		} else {
			addElite();
		}
	}
	
	protected int determineNumElitePerQual(int iterationCount) {
		int num;
		if (iterationCount <= 1) {
			num = 1;
		} else {
			num = numElitePerQuali;
		}
		log.info("{} elites to be qualified", num);
		return num;
	}

	/**
	 * @return The minimum number of new candidates to start a new qualification iteration.  
	 */
	abstract protected int determineMinNewCan();

	/**
	 * @param iterationCount
	 * @return
	 */
	protected int determineNumInstances(int iterationCount) {
		int numInstances;
		if (! incrementInstance || firstPostSelectionFinished) {
			numInstances = 1;
		} else {
			numInstances = (iterationCount - 1) % maxEvalInstances + 1;
		}
		log.info("Evaluation with {} instances.", numInstances);
		return numInstances;
	}

	/**
	 * 
	 */
	protected void addRestartElites() {
		int numRestartElites = getNumElites();
		log.info("{} restart elites to be added to {} elites", numRestartElites, elites.size());
		Configuration conf;
		for (int i = 0; i < getNumElites(); i++) {
			conf = restartElites.get(i);
			if (! elites.contains(conf)) {
				elites.add(conf);
			}
		}
		log.info("Total {} elites", elites.size());
		restartElites.clear();
	}

	/**
	 * @param instances
	 * @param rest
	 * @param numElites
	 * @return
	 */
	protected Configuration postSelect(Instance[] instances, int numElites) {
		// bobyqa calculated one more evaluation when budget is exceeded.
		int budgetPost = budget - getUsedBudget();
//		if (iterationCount > 10) {
//			budgetPost += 20 * iterationCount;
//		} else {
//			budgetPost += 2 * iterationCount * iterationCount;
//		}
		
		if (numElites > 0) {
			log.info("Start elite selection after {} qualification iterations with total budget for elite selection {}", 
					numElites, budgetPost);
			if (numElites == 1) {
				if (budgetPost >= 2 && optim.bestPoints != null && optim.bestPoints.size() >= 2) {
					// if only one elite is added and still budget left, add the second best point
					log.info("Only one elite while still {} budget left, added new elite", budgetPost);
					elites.add(new Configuration(params, eval.toParamValues(
							optim.bestPoints.get(1).getPoint())));
					numElites++;
				} else {
					return elites.get(0);
				}
			}
		} else {
			log.error("Post-selection with {} elite configurations. Exit.", numElites);
			System.exit(1);
		}
		
		int firstTest = Math.min(10, numElites + 2);
		if (numElites == 2) {
			// if only two elites, use up all the rest of budget
			firstTest = Integer.MAX_VALUE;
		}
		PostSelection ps = new PostSelection(elites);
		Configuration bestConf = ps.eliteSelection(budgetPost, instances, Tuner.useFRace, 
				firstTest);
		return bestConf;
	}

	/**
	 * Update the instances used for evaluation 
	 */
	public void updateQualInstances() {
		updateQualInstances(numEval);
	}

	/**
	 * Update the instances used for evaluation 
	 */
	public void updateQualInstances(int numInstances) {
		updateQualInstances(numInstances, numInstances);
	}

	/**
	 * 
	 * @param numInstances number of qualification instances 
	 * @param numIncrease number of increase in instance index
	 */
	public void updateQualInstances(int numInstances, int numIncrease) {
		Instance[] qualificationInstances;
		qualificationInstances = new Instance[numInstances];
		
		for (int i = 0; i < numInstances; i++) {
			qualificationInstances[i] = instances[instanceIndex + i];
		}

		eval.setInstances(qualificationInstances);
		instanceIndex += numIncrease;
	}

	public void setAdditionalInstance() {
		// used a random first 10 instances for additional evaluation
		int index = instanceIndex < 10 ? instanceIndex : Randomizer.nextInt(10);
		eval.setInstances(new Instance[]{instances[index]});
	}

	public int addElite() {
		return addElite(optim.bestPoints.get(0).getPoint());
	}

	public int addElite(double[] bestPoint) {
		return addElite(new Configuration(params, eval.toParamValues(bestPoint)));
	}

	public int addElite(Configuration elite) {
		log.info("Best: {}", elite.toString());
		if (restartElites.contains(elite)) {
			log.info("Already exists as elite, ignore.");
		} else {
			restartElites.add(elite);
			log.info("Added as elite.");
		}
		
		restEval = countRestEval();
		
		//log.info(Arrays.toString(bestPoint));
		//OutputHandler.writeln(Arrays.toString(optim.getCurrentBest().getDataRef()));
		//log.info(Arrays.toString(bestPoint));
		return restEval;
	}

	protected void addEliteWithSecondEval() {
		addEliteWithSecondEval(numEval);
	}

	/**
	 * 
	 */
	protected void addEliteWithSecondEval(int numPrevEval) {
		ArrayList<RealPointValuePair> points = addEval(optim, numPrevEval);
		for (int i = 0; i < numElite2add; i++) {
			addElite(points.get(i).getPoint());
		}
	}

	public ArrayList<RealPointValuePair> addEval(
			BaseAbstractScalarOptimizer<MultivariateFunction> optim) {
		return addEval(optim, numEval);
	}
	public ArrayList<RealPointValuePair> addEval(
			BaseAbstractScalarOptimizer<MultivariateFunction> optim, int numPrevEvals) {
		int numRest = countRestEval();
		if (numAddEval < 2 || numRest <= 2) {
			return optim.bestPoints;
		} 
		int num2eval = Math.min(numRest, numAddEval);
		
		setAdditionalInstance();
		double[] values = new double[num2eval];
		log.info("{} candidates for additional evaluation:", num2eval);
		RealPointValuePair point;
		for (int i = 0; i < num2eval; i++) {
			point = optim.bestPoints.get(i);
			log.info("{}: {} with evaluation {}", i + 1, Arrays.toString(point.getPoint()), 
					point.getValue());
			values[i] = eval.value(point.getPoint()) + point.getValue() * numPrevEvals;
		}
		log.info("evaluations after second qualification: {}", Arrays.toString(values));
		Integer[] order = CollectionHandler.rank(values);
		log.info("ranks after second qualification: {}", Arrays.toString(order));
		swapBestPoints(optim.bestPoints, order);
		return optim.bestPoints;
	}
	private void swapBestPoints(ArrayList<RealPointValuePair> bestPoints,
			Integer[] order) {
		int num = order.length;
		ArrayList<RealPointValuePair> tmp = new ArrayList<RealPointValuePair>(num);
		for (int i = 0; i < num; i++) {
			tmp.add(bestPoints.get(order[i]));
		}
		for (int i = 0; i < num; i++) {
			bestPoints.set(i, tmp.get(i));
		}
	}

	/**
	 * @return
	 */
	protected ArrayList<RealPointValuePair> addEval() {
		return addEval(optim);
	}

	/**
	 * 
	 */
	public int countRestEval() {
		int reserve;
		int numElites = getNumElites();
		int numEliteReserve = numElites + numElite2add;
		
		reserve = getBudgetReserve(numElites, numEliteReserve);

		int used = getUsedBudget();
		int rest = budget - used - reserve;
		
		log.info("used {} reserve {} rest {}", used, reserve, rest);
		return rest;
	}

	/**
	 * @return
	 */
	public int getNumElites() {
		return restartElites.size();
	}

	/**
	 * @return
	 */
	public int getUsedBudget() {
		return Tuner.arch.getSize();
	}

	public int getBudgetReserve() {
		int numElites = getNumElites();
		return getBudgetReserve(numElites, numElites);
	}
	/**
	 * @param currentNumElites
	 * @param numElite2Reserve
	 * @return
	 */
	public int getBudgetReserve(int currentNumElites, int numElite2Reserve) {
		int reserve;
		if (currentNumElites <= 0) {
			// If no elite is added, reserve nothing
			reserve = 0;
		} else if (numElite2Reserve < 10) {
			reserve = 2 * numElite2Reserve * numElite2Reserve;
		} else {
			reserve = 20 * numElite2Reserve;
		}
		return reserve;
	}

	public int determineLambdaByFactor(double factor) {
		return 4 + (int) (factor * Math.log(dim));
	}

	public int getLambda() {
		if (lambda > 0) {
			return lambda;
		} else {
			return lambda0;
		}
	}

	public int determineSimplexBudget(int budget) {
		return determineSimplexBudget(budget, getLambda());
	}
	public int determineSimplexBudget(int budget, int numCanSim) {
		int budgetSim = numCanSim;
		if (budgetSim + numAddEval + getBudgetReserve(1, 2) + minBud > budget) {
			// there is no next iteration, set budget to max
			budgetSim = budget;
		}
		log.info("Start simplex with budget {}", budgetSim);
		return budgetSim;
	}

	public int getLambda0() {
		return lambda0;
	}

	protected abstract void oneQualification(int budget, int iterationCount);

}
