/*
 * Created by Zhi Yuan
 */
package tune;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datahandler.OutputHandler;
import race.Race;
import algo.Configuration;
import algo.Instance;
import eval.AlgorithmEvaluator;

/**
 * @author yuan
 * Created on Aug 20, 2013
 *
 */
public class PostSelection {

	private final Logger log = LoggerFactory.getLogger(getClass());
	ArrayList<Configuration> elites;
	
	/**
	 * 
	 */
	public PostSelection() {
		elites = new ArrayList<Configuration>();
	}

	public PostSelection(ArrayList<Configuration> elites) {
		this.elites = elites;
	}

	public Configuration eliteSelection(int budget, Instance[] instances, boolean isFriedman, 
			int firstTest) {
		AlgorithmEvaluator eval = new AlgorithmEvaluator(
				elites.toArray(new Configuration[elites.size()]), instances);
		elites = new ArrayList<Configuration>(Arrays.asList(eval.getConfigurations()));
		int numConfig = elites.size();
		int numInstances = instances.length;
		log.info("Elite selection with {} configurations, {} budget, {} instances, first test at {}", 
				numConfig, budget, numInstances, firstTest);
		Race racer = new Race(numConfig, numInstances, eval, budget, isFriedman, firstTest);
		int bestIndex;
		if (numConfig > 1) {
			bestIndex = racer.race();
		} else {
			bestIndex = 0;
		}
		if (bestIndex < 0) {
			// race fails
			log.error("Race fails to select from {} elites with budget {}, return first elite", 
					numConfig, budget);
			bestIndex = 0;
		}
		Configuration bestConf = eval.getConfigurations()[bestIndex];
		OutputHandler.writeln(bestConf.toString());
		return bestConf;
	}
}
