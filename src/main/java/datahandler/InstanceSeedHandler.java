/**
 * 
 */
package datahandler;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Randomizer;
import algo.Instance;

/**
 * This class sets up the training and testing instances for tuning experiment 
 * from a given training instance set and a testing instance set.  
 * The task is to assign randomly generated random seed to each instance for evaluation, 
 * and duplicate instances if the instance set is not large enough. 
 * @author yuan
 *
 */
public class InstanceSeedHandler {
	private static final Logger log = LoggerFactory.getLogger(InstanceSeedHandler.class);
	
	static String prob = "tsqap";
	//static String prob = "cplex_vfp";
	//static String prob = "cplex_lotsize";

	static String inputInstanceFolder = prob + "/Instanceu";
	static String inputTestInstanceFolder = prob + "/TestInstanceu";
	static String filePat = ".dat";
	static int trainDuplicate = 10;
	static int testDuplicate = 10;
	static int numTrainTrials = 25;
	//static int numTest = 25;
	
	//static String outputInstanceFolder = "cplex/Instances";
	//static String outputTestInstanceFolder = "cplex/TestInstances";
	//static String outputSeedInsFolder = "cplex_lotsize/seed-settings";
	static String outputSeedInsFolder = prob + "/seed-settingu";
	

	/**
	 * 
	 */
	public InstanceSeedHandler() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		setupInstances();

	}

	private static void setupInstances() {
		Randomizer.init();
		File folder = new File(inputInstanceFolder);
		File[] listOfFolders = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().indexOf(filePat) >= 0;
			}
		});
		int numInstances = listOfFolders.length;
		int totalInstances = trainDuplicate * numInstances;
		int[] orders;
		Instance[] instances = new Instance[totalInstances];
		int index = 0;
		int seed;
		StringBuffer sb;
		String outFile;
		log.info("Totally {} instances", numInstances);
		for (int i = 0; i < numInstances; i++) {
			log.info("{}", listOfFolders[i]);
		}
		
		log.info("Number of training trials: {}", numTrainTrials);
		for (int k = 0; k < numTrainTrials; k++) {
			sb = new StringBuffer();
			for (int i = 0; i < trainDuplicate; i++) {
				orders = Randomizer.generateRandomPermutation(numInstances);
				//System.out.println(Arrays.toString(orders));
				
				for (int j = 0; j < numInstances; j++) {
					seed = Randomizer.nextInt(100000000, 999999999);
					sb.append(seed).append(" ").append(listOfFolders[orders[j]].getName()).append("\n");
					//instances[index++] = new Instance(seed, listOfFolders[orders[j]].getName());
			    	//System.out.println(i + " " + j + " " + instances[index-1]);
			    	
			    }
				
			}
			
			//File outDir = new File(outputInstanceFolder);
			//outDir.mkdir();
			outFile = new StringBuffer(outputSeedInsFolder).append(File.separator)
					.append("train").append(k+1).append(".txt").toString();
			//System.out.println(outFile);
			OutputHandler.print2File(outFile, sb.toString());
						
		}
		
		sb = new StringBuffer();
		folder = new File(inputTestInstanceFolder);
		listOfFolders = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().indexOf(filePat) >= 0;
			}
		});
		numInstances = listOfFolders.length;
		log.info("{} Testing instances with {} seeds each", numInstances, testDuplicate);
		
		for (int i = 0; i < testDuplicate; i++) {
			orders = Randomizer.generateRandomPermutation(numInstances);
			//System.out.println(Arrays.toString(orders));
			
			for (int j = 0; j < numInstances; j++) {
				seed = Randomizer.nextInt(100000000, 999999999);
				sb.append(seed).append(" ").append(listOfFolders[orders[j]].getName()).append("\n");
				//instances[index++] = new Instance(seed, listOfFolders[orders[j]].getName());
		    	//System.out.println(i + " " + j + " " + instances[index-1]);
		    	
		    }
		}
		
		outFile = new StringBuffer(outputSeedInsFolder).append(File.separator)
				.append("test.txt").toString();
		//System.out.println(outFile);
		OutputHandler.print2File(outFile, sb.toString());
		

	}

}
