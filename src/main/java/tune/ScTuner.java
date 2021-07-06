/**
 * 
 */
package tune;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuan
 *
 */
public class ScTuner extends UbcTuner {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 
	 */
	public ScTuner() {
		// TODO Auto-generated constructor stub
	}

	protected void oneQualification(int budget, int iterationCount) {
		int budgetSim = determineSimplexBudget(budget);
		double[] bestPoint = doSimplex(budgetSim, iterationCount);
	
		if (restEval < minBud) {
			return;
		}
		
		doCma(bestPoint);
	}

}
