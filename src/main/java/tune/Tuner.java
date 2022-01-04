/*
 * Created by Zhi Yuan
 */
package tune;


import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

import com.google.gson.Gson;
import datahandler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import algo.Archive;
import algo.CategoricalParameter;
import algo.Configuration;
import algo.Instance;
import algo.NumericalParameter;
import algo.Parameter;
import eval.AlgorithmEvaluator;
import race.Race;
import util.Randomizer;
import util.SystemProperty;

/**
 * @author Zhi Yuan
 * Created on Apr 16, 2013
 * 
 * Currently only U/F-Race, i.e. uniform random sampled configurations selected by F-Race,
 * is used. To-do tasks and known problems include: 
 * 1, should also handle the categorical and conditional parameters; 
 * 2, the Wilcoxon test implemented by apache is not exactly correct; 
 * 3, check repeated configurations and resample; 
 * 4, achive previous evaluations in {@link Race};
 * 5, the evaluation result is currently read from the last line of the screen output 
 * when running the target algorithm. Should also allow other possibilities;
 */
public class Tuner {
	
	static CmdLineHandler chandler = new CmdLineHandler();
	public static CopsOptionParser parser = new CopsOptionParser();
	//private final static Logger log = Logger.getLogger(Tuner.class.getName());
	private static Logger log = LoggerFactory.getLogger(Tuner.class);

	public static String tuner;
	/**
	 * q for solution quality; t for computation time; a for anytime performance. 
	 */
	public static char goal;
	//public static String tuner = "utrace";
	public static int signifDigit;
	public static Archive arch = new Archive();
	public static Archive warmStartArchive = new Archive();
	public static double cutoffTime;
	public static HashMap<String, Double> opt;
	public static boolean useFRace;
	public static boolean hasCategorical;
	public static boolean hasConditional;
	public static boolean hasNumerical;
	public static Configuration defaultConfig;
	public static boolean hasDefault;
	private static long randomSeed;
	public static boolean warmStart;
	private static String randomSeedFile;
	public static String archiveFile;
	public static int initialArchiveSize = 0;

	/**
	 * 
	 */
	public Tuner() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Logger log = Logger.getLogger("");
		//log.setLevel(Level.WARNING);
		parser = chandler.readCmdLineArgs(args);
		randomSeedFile = SystemProperty.get(SystemProperty.RANDOM_SEED_FILE);
		archiveFile = SystemProperty.get(SystemProperty.ARCHIVE_FILE);
		warmStart = (Boolean) parser.getOptionValue(CopsOptionParser.WARM_START, false);
		if (warmStart) {
			if (randomSeedFile == null || archiveFile == null) {
				log.error("Warm start failed, because either random seed file or archive file is not defined. Will proceed without warm start");
				warmStart = false;
			} else {
				File f = new File(archiveFile);
				if (! f.exists() || f.isDirectory()) {
					log.error("Warm start failed, because no valid archive file is found. Will proceed without warm start");
					warmStart = false;
				} else {
					readArchiveFromFile();
					if (warmStartArchive.getSize() <= 0) {
						log.error("Warm start failed, because archive file is empty. Will proceed without warm start");
						warmStart = false;
					} else {
						f = new File(randomSeedFile);
						if (! f.exists() || f.isDirectory()) {
							log.error("Warm start failed, because no valid random seed file is found. Will proceed without warm start");
							warmStart = false;
						} else {
							readRandomSeedFromFile();
						}
					}

				}
			}
		}
		init();
		String paramFile = SystemProperty.get(SystemProperty.PARAM_FILE);
		Parameter[] params = InputHandler.readParams(paramFile);
		hasNumerical = hasNumericalParameter(params);
		log.info("Numerical parameters exists? {}", hasNumerical);
		hasCategorical = hasCategoricalParameter(params);
		log.info("Categorical parameters exists? {}", hasCategorical);
		hasConditional = hasConditionalParameter(params);
		log.info("Conditional parameters exists? {}", hasConditional);
		checkDefaultConfiguration(params);
		int budget = Integer.valueOf(SystemProperty.get(SystemProperty.MAXEXP));
		String insFile = SystemProperty.get(SystemProperty.INS_SEED_FILE);
		
		Instance[] instances = InputHandler.readInstanceSeed(insFile);
		log.info("Max number of evaluations: {}", budget);
		Configuration bestConf = null;

		String outputConfigJsonFile = SystemProperty.get(SystemProperty.OUTPUT_CONFIGURATION_JSON_FILE);

		String tuningGoal = SystemProperty.get(SystemProperty.TUNING_GOAL).toLowerCase();
		if (tuningGoal.startsWith("so") || tuningGoal.startsWith("qu")) {
			goal = 'q';
		} else if (tuningGoal.startsWith("co") || tuningGoal.startsWith("ti")) {
			goal = 't';
			String optFile = SystemProperty.get(SystemProperty.OPT_FILE);
			log.info("optimum file? {}", optFile);
			if (optFile != null) {
				opt = InputHandler.readOpt(optFile);
			}
		} else if (tuningGoal.startsWith("an")) {
			goal = 'a';
		} else {
			log.error("The tuning goal {} in the scenario file is not recognizable. Please specify qual, time, or anytime", 
					tuningGoal);
			System.exit(1);
		}
	
		try {
			cutoffTime = Double.valueOf(SystemProperty.get(SystemProperty.CUTOFF_TIME));
		} catch (Exception e) {
			if (goal == 't') {
				log.error("Please specify the cutoff time in scenario file");
				System.exit(1);
			} else {
				cutoffTime = Double.MIN_VALUE;
			}
			
		}
		
		boolean validateOnly = (Boolean) parser.getOptionValue(CopsOptionParser.VALIDATE_ONLY, false);
		if (validateOnly) {
			log.info("Validate only.");
			log.info("Input argument: {}", Arrays.toString(args));
			String confString = (String) parser.getOptionValue(CopsOptionParser.CONF, null);
			if (confString == null) {
				log.warn("No configuration is given for validation only, take the default configuration.");
				if (defaultConfig == null) {
					log.error("No default configuration exists, stop");
					System.exit(2);
				} else {
					log.info("Validating default configuration: {}", defaultConfig);
					validate(defaultConfig);
				}
			} else {
				Configuration conf = new Configuration(params, confString);
				log.info("Validating input configuration: {}", conf);
				validate(conf);
				
			}
			System.exit(0);
		}

		if (tuner.toLowerCase().startsWith("ur")) {
			bestConf = urace(params, budget, instances, useFRace);
		} else if (tuner.toLowerCase().startsWith("bo")) {
			
			if (hasCategorical) {
				log.warn("{} cannot be used to tune categorical parameters, use I/Race instead.", tuner);
				bestConf = irace(params, budget, instances);
			} else {
				bestConf = bobyqa(params, budget, instances);
			}
		} else if (tuner.toLowerCase().startsWith("i")) {
			bestConf = irace(params, budget, instances);
		} else if (tuner.toLowerCase().startsWith("c")) {
			if (hasCategorical) {
				log.warn("{} cannot be used to tune categorical parameters, use I/Race instead.", tuner);
				bestConf = irace(params, budget, instances);
			} else {
				bestConf = cmaes(params, budget, instances);
			}
		} else if (tuner.toLowerCase().startsWith("bc")) {
			
			if (hasCategorical) {
				log.warn("{} cannot be used to tune categorical parameters, use I/Race instead.", tuner);
				bestConf = irace(params, budget, instances);
			} else {
				bestConf = bocma(params, budget, instances);
			}
		} else if (tuner.toLowerCase().startsWith("ubc")) {
			
			if (hasCategorical) {
				log.warn("{} cannot be used to tune categorical parameters, use I/Race instead.", tuner);
				bestConf = irace(params, budget, instances);
			} else {
				bestConf = ubc(params, budget, instances);
			}
		} else if (tuner.toLowerCase().startsWith("sbc")) {
			
			if (hasCategorical) {
				log.warn("{} cannot be used to tune categorical parameters, use I/Race instead.", tuner);
				bestConf = irace(params, budget, instances);
			} else {
				bestConf = sbc(params, budget, instances);
			}
		} else if (tuner.toLowerCase().startsWith("sc")) {
			
			if (hasCategorical) {
				log.warn("{} cannot be used to tune categorical parameters, use I/Race instead.", tuner);
				bestConf = irace(params, budget, instances);
			} else {
				bestConf = sc(params, budget, instances);
			}
		} else if (tuner.toLowerCase().equals("bs")) {
			
			if (hasCategorical) {
				log.warn("{} cannot be used to tune categorical parameters, use I/Race instead.", tuner);
				bestConf = irace(params, budget, instances);
			} else {
				bestConf = bs(params, budget, instances);
			}
		} else if (tuner.toLowerCase().startsWith("si")) {
			if (hasCategorical) {
				log.warn("{} cannot be used to tune categorical parameters, use I/Race instead.", tuner);
				bestConf = irace(params, budget, instances);
			} else {
				bestConf = simplex(params, budget, instances);
			}
		} else {
			log.error("Tuner {} is not recognizable. Please check.", tuner);
		}
		
		boolean validation = 
			SystemProperty.get(SystemProperty.VALIDATION).equalsIgnoreCase("true");
		if (validation) {
			validate(bestConf);
		}

		System.out.println(TuningStatus.listAll());
		if (bestConf == null) {
			if (TuningStatus.isEmpty()) {
				System.err.println("Failed");
				System.exit(3);
			} else {
				writeBestConfigToFile(outputConfigJsonFile, TuningStatus.lastBestConfiguration());
				System.err.println(new StringBuilder("Interrupted ").append(TuningStatus.listLast()));
				System.exit(4);
			}
		} else {
			writeBestConfigToFile(outputConfigJsonFile, bestConf);
			System.err.println(new StringBuilder("Success ").append(bestConf.toString()).toString());
		}

	}

	private static void readArchiveFromFile() {
		Gson gson = new Gson();
		InputReader ir = new InputReader(archiveFile);
		String jsonText = ir.readAll();
		warmStartArchive = gson.fromJson(jsonText, Archive.class);
		initialArchiveSize = warmStartArchive.getSize();
		log.info("Warm start read archive from file {} with {} entries", archiveFile, initialArchiveSize);
	}

	private static void readRandomSeedFromFile() {
		InputReader ir = new InputReader(randomSeedFile);
		randomSeed = Long.parseLong(ir.readAll());
		log.info("Read random seed from file {}: {}", randomSeedFile, randomSeed);
	}

	private static void writeBestConfigToFile(String outputConfigJsonFile, Configuration configuration) {
		if (outputConfigJsonFile != null && ! outputConfigJsonFile.isEmpty()) {
			 OutputHandler.print2File(outputConfigJsonFile, configuration.toJsonString());
			 log.info("Best configuration {} written to json file: {}", configuration, outputConfigJsonFile);
		}
	}

	/**
	 * @param conf
	 */
	protected static void validate(Configuration conf) {
		log.info("Starting validate the best configuration: {}", conf);
		String testInsFile = SystemProperty.get(SystemProperty.TEST_INS_SEED_FILE);
		//String testInsDir = SystemProperty.addWorkDir(SystemProperty.get(SystemProperty.TEST_INS_DIR));
		String testInsDir = SystemProperty.get(SystemProperty.TEST_INS_DIR);
		Instance[] testInstances = InputHandler.readInstanceSeed(testInsFile, testInsDir);
		String testResultFile = SystemProperty.get(SystemProperty.TEST_RESULT_FILE);
		//cutoffTime = 10;
		repeatedEvaluate(conf, testInstances, testResultFile);
	}

	private static Configuration sc(Parameter[] params, int budget, Instance[] instances) {
		log.info("Starting post-selection Simplex-CMAES tuner");
		PostSelectionTuner sc = new ScTuner();
		Configuration bestConf = sc.tune(params, budget, instances);

		return bestConf;
	}

	private static Configuration bs(Parameter[] params, int budget,
			Instance[] instances) {
		log.info("Starting post-selection BOBYQA-Simplex tuner");
		PostSelectionTuner bs = new BsTuner();
		Configuration bestConf = bs.tune(params, budget, instances);

		return bestConf;
	}

	private static Configuration ubc(Parameter[] params, int budget,
			Instance[] instances) {
		log.info("Starting post-selection UBC tuner");
		PostSelectionTuner ubc = new UbcTuner();
		Configuration bestConf = ubc.tune(params, budget, instances);

		return bestConf;
	}

	private static Configuration sbc(Parameter[] params, int budget,
			Instance[] instances) {
		log.info("Starting post-selection SBC tuner");
		PostSelectionTuner ubc = new UbcTuner(true);
		Configuration bestConf = ubc.tune(params, budget, instances);

		return bestConf;
	}

	private static Configuration simplex(Parameter[] params, int budget,
			Instance[] instances) {
		log.info("Starting post-selection Simplex tuner");
		PostSelectionTuner bt = new SimplexTuner();
		Configuration bestConf = bt.tune(params, budget, instances);

		return bestConf;
	}

	private static Configuration bocma(Parameter[] params, int budget,
			Instance[] instances) {
		log.info("Starting post-selection BOBYQA/CMA-ES tuner");
		PostSelectionTuner bct = new BocmaTuner();
		Configuration bestConf = bct.tune(params, budget, instances);

		return bestConf;
	}

	private static Configuration cmaes(Parameter[] params, int budget,
			Instance[] instances) {
		log.info("Starting post-selection CMA-ES tuner");
		PostSelectionTuner ct = new CmaesTuner();
		Configuration bestConf = ct.tune(params, budget, instances);

		return bestConf;
	}

	private static boolean hasNumericalParameter(Parameter[] params) {
		for (int i = 0; i < params.length; i++) {
			if (params[i].isNumerical()) {
				return true;
			}
		}
		return false;
	}

	private static Configuration irace(Parameter[] params, int budget,
			Instance[] instances) {
		log.info("Starting iterated racing tuner");
		IRaceTuner ir = new IRaceTuner();
		Configuration bestConf = ir.tune(params, budget, instances);

		return bestConf;
	}

	private static boolean hasCategoricalParameter(Parameter[] params) {
		for (int i = 0; i < params.length; i++) {
			if (params[i].isCategorical()) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasConditionalParameter(Parameter[] params) {
		for (int i = 0; i < params.length; i++) {
			if (params[i].isConditional()) {
				return true;
			}
		}
		return false;
	}

	private static void init() {
		if (! warmStart) {
			randomSeed = (long) SystemProperty.getDouble(SystemProperty.RANDOM_SEED, System.nanoTime());
			if (randomSeedFile != null) {
				OutputHandler.print2File(randomSeedFile, String.valueOf(randomSeed));
			}
		}
		Randomizer.init(randomSeed);
		log.info("JRace starts with random seed {}", randomSeed);
		tuner = SystemProperty.get(SystemProperty.TUNER);
		signifDigit = Integer.valueOf(SystemProperty.get(SystemProperty.SIGNIF_DIGIT));
		useFRace = SystemProperty.get(SystemProperty.RACE_TYPE).toLowerCase().startsWith("f");
	}

	private static Configuration bobyqa(Parameter[] params, int budget, Instance[] instances) {
		log.info("Starting post-selection BOBYQA tuner");
		PostSelectionTuner bt = new BobyqaTuner();
		Configuration bestConf = bt.tune(params, budget, instances);

		return bestConf;
		/*AlgorithmEvaluator eval = new AlgorithmEvaluator(instances);
		// TODO should read number of instances from file
		int numInstances = instances.length;
		Race racer = new Race(numConfig, numInstances, eval, budget, isFRace);
		int bestIndex = racer.race();
		System.out.println("Best parameter configuration found: ");
		System.out.println(configs[bestIndex].toString());*/

	}

	private static void repeatedEvaluate(Configuration bestConf,
			Instance[] testInstances, String testResultFile) {
		AlgorithmEvaluator eval = new AlgorithmEvaluator(new Configuration[]{bestConf}, 
				testInstances);
		String s = eval.validateConf(0);
		OutputHandler.print2File(testResultFile, s);
	}

	private static void bobyqa_bk(Parameter[] params, int budget, Instance[] instances) {
		log.info("Starting BOBYQA tuner");
		PostSelectionTuner bt = new BobyqaTuner();
		bt.tune(params, budget, instances);
		
		/*AlgorithmEvaluator eval = new AlgorithmEvaluator(instances);
		// TODO should read number of instances from file
		int numInstances = instances.length;
		Race racer = new Race(numConfig, numInstances, eval, budget, isFRace);
		int bestIndex = racer.race();
		System.out.println("Best parameter configuration found: ");
		System.out.println(configs[bestIndex].toString());*/

	}

	private static Configuration urace(Parameter[] params, int budget, Instance[] instances) {
		return urace(params, budget, instances, true);
	}
	
	private static Configuration urace(Parameter[] params, int budget, Instance[] instances, 
			boolean isFRace) {
		log.info("Starting uniform random sampling racing tuner");
		URaceTuner ur = new URaceTuner();
		Configuration bestConf = ur.tune(params, budget, instances);

		return bestConf;
		
	}

	public static void checkDefaultConfiguration(Parameter[] params) {
		Parameter param;
		int dim = params.length;
		Configuration config = new Configuration(dim);
		double value;
		String catValue;
		for (int i = 0; i < dim; i++) {
			param = params[i];
			if (param.isNumerical()) {
				value = ((NumericalParameter) param).getDefaultValue();
				if (value == Double.MIN_VALUE) {
					log.info("No default parameter configuration given");
					return ;
				} else {
					config.addParam(param, value);
				}
			} else if (param.isCategorical()) {
				catValue = ((CategoricalParameter) param).getDefaultValue();
				if (catValue == null) {
					log.info("No default parameter configuration given");
					return ;
				} else {
					config.addParam(param, catValue);
				}
			} else {
				log.error("Unrecognized parameter {}", param.toString());
			}
		}
		log.info("Default parameter configuration given: {}", config);
		hasDefault = true;
		defaultConfig = config;
	}

	
}
