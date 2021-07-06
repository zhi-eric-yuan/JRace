package tune;

import org.apache.commons.math.optimization.direct.NelderMeadSimplex;
import org.apache.commons.math.optimization.direct.SimplexOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.SystemProperty;
import algo.Configuration;
import algo.Instance;
import eval.AlgorithmEvaluator;
import eval.Evaluator;

public class BsTuner extends BobyqaTuner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	protected boolean firstSave = SystemProperty.getBoolean(SystemProperty.FIRST_SAVE, false);
	protected int BsType = SystemProperty.getInteger(SystemProperty.BS_TYPE, 1);

	SimplexTuner<Evaluator> st;

	protected void initBobyqaRestart() {
		bobyqaRestartType = 3;
	}

	@Override
	protected void oneQualification(int budget, int iterationCount) {
		int simNumPoints = dim + 1;
		if (iterationCount % 2 == 1) {
			// BOBYQA iteration
			numAddEval = simNumPoints;
			super.oneQualification(budget, iterationCount);
			minNewCan = dim;
		} else {
			// Simplex iteration
			updateQualInstanceBs(iterationCount);
			st = new SimplexTuner<Evaluator>();
			int numInstances = eval.getInstances().length;
			double[][] startSimplex = new double[simNumPoints][dim];
			for (int i = 0; i < simNumPoints; i++) {
				startSimplex[i] = optim.bestPoints.get(i).getPoint();
				log.info("{} {}", i, startSimplex[i]);
			}

			numAddEval = 2;

			optim = new SimplexOptimizer(1e-16, 1e-16);
			optim.initBest(numAddEval);
			((SimplexOptimizer)optim).setSimplex(new NelderMeadSimplex(startSimplex));

			optim = st.doSimplex(optim, budget / numInstances, eval, startSimplex[0]);

			if (BsType != 1) {
				addElite();
			} else {
				Configuration lastBest = restartElites.get(restartElites.size() - 1);
				Configuration thisBest = new Configuration(params, eval.toParamValues(
						optim.bestPoints.get(0).getPoint()));
				Configuration secondBest = new Configuration(params, eval.toParamValues(
						optim.bestPoints.get(1).getPoint()));
				log.info("Best conf from last BOBYQA iteration: {}", lastBest);
				log.info("Best conf of this simplex iteration: {}", thisBest);
				log.info("Second best: {}", secondBest);
				if (lastBest.equals(thisBest)) {
					log.info("Add second best");
					addElite(secondBest);
				} else if (lastBest.equals(secondBest)) {
					log.info("Add the best");
					addElite(thisBest);
				} else {
					log.info("Second evaluation of the best two");
					addEliteWithSecondEval();
				}
			}
			minNewCan = determineMinNewCan();
		}

	}

	private void updateQualInstanceBs(int iterationCount) {
		if (BsType == 1) {
			if (iterationCount == 2 || firstSave) {
				eval.setInstances(new Instance[]{instances[instanceIndex - numEval]});
			} else {
				// alternating instance
				eval.setInstances(new Instance[]{eval.getInstances()[0]});
				if (instanceIndex < 10) {
					instanceIndex += numEval;
				}
			}
		} else {
			if (iterationCount == 2 && firstSave) {
				eval.setInstances(new Instance[]{instances[0]});
			} else {
				Instance[] iterationInstances = new Instance[2];
				iterationInstances[0] = instances[instanceIndex - numEval];
				iterationInstances[1] = eval.getInstances()[0];
				eval.setInstances(iterationInstances);
				if (instanceIndex < 10) {
					instanceIndex += numEval;
				}
			}
		}
	}

}
