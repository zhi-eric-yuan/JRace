/*
 * Author: Eric Yuan
 * Created on 06.10.2005
 */
package util;

import java.io.File;
import java.util.HashMap;

import tune.Tuner;
import datahandler.CopsOptionParser;
import datahandler.InputHandler;

/**
 * Enter type description here.
 * @author Eric Yuan
 * <p>
 * Created on 06.10.2005
 */
public class SystemProperty {
	
	protected static HashMap<String, String> properties;
	//public static final String PROPERTY_FILE_NAME = "System.properties";
	public static final String PROPERTY_FILE_NAME = 
		(String) Tuner.parser.getOptionValue(CopsOptionParser.SCENARIO, "scenario.txt");
	public static final String WORK_DIR = determineWorkDir();
	public static final String DEBUG = "debug";
	public static final String INTERACTIVE = "interactive";
	public static final String MAXEXP = "max_exp";
	public static final String EXEC = "exec";
	public static final String INSINIT = "ins_init";
	public static final String SEEDINIT = "seed_init";
	public static final String INS_SEED_FILE = "ins_seed_file";
	public static final String TEST_INS_SEED_FILE = "test_ins_seed_file";
	public static final String PARAM_FILE = "param_file";
	public static final String INS_DIR = "ins_dir";
	public static final String TEST_INS_DIR = "test_ins_dir";
	public static final String CMD_END = "cmd_end";
	public static final String TUNER = "tuner";
	public static final String VALIDATION = "validation";
	public static final String TEST_RESULT_FILE = "test_result_file";
	public static final String RACE_TYPE = "race_type";
	public static final String TUNING_GOAL = "tuning_goal";
	public static final String CUTOFF_TIME = "cutoff_time";
	public static final String SIGNIF_DIGIT = "signif_digit";
	public static final String OPT_FILE = "opt_file";
	public static final String TIME_INIT = "time_init";
	public static final String OPT_INIT = "opt_init";
	public static final String START_RECORD_TIME = "start_record_time";
	public static final String LOG_TIME = "log_time";
	public static final String LOG_QUAL = "log_qual";
	public static final String A = "a";
	public static final String B = "b";
	public static final String C = "c";
	public static final String D = "d";
	public static final String E = "e";
	public static final String F = "f";
	public static final String G = "g";
	public static final String MU_INC = "mu_inc";
	public static final String C_NUM_ADD_EVAL = "c_num_add_eval";
	public static final String EVAL_HIST_BEST = "eval_hist_best";
	public static final String POP_LEVEL = "pop_level";
	public static final String S_NUM_ADD_EVAL = "s_num_add_eval";
	public static final String MULTI_ELITES = "multi_elites";
	public static final String BOBYQA_RESTART_TYPE = "bobyqa_restart_type";
	public static final String FIRST_SAVE = "first_save";
	public static final String BS_TYPE = "bs_type";
	public static final String SIMPLEX_EARLY = "simplex_early";
	public static final String S_MAX = "s_max";
	public static final String RANDOM_SEED = "random_seed";
	public static final String NUM_UNIF_FAC = "num_unif_fac";
	public static final String NUM_EVAL = "num_eval";
	public static final String CRACE = "crace";
	public static final String PLUS_NUM_CAND = "plus_num_cand";
	public static final String QUAL_FIRST_TEST = "qual_first_test";
	public static final String ADAPT_MU = "adapt_mu";
	
	/**
	 * Create a new SystemProperty object.
	 */
	public SystemProperty() {
		super();
		// TODO Auto-generated constructor stub
	}

	private static String determineWorkDir() {
		int index = PROPERTY_FILE_NAME.lastIndexOf(System.getProperty("file.separator"));
		String userDir = ".";
		if (index <= 0) {
			return userDir;
		} else {
			return combineDir(userDir, PROPERTY_FILE_NAME.substring(0, index));
		}
	}

	public static String get(String key) {
		if (properties == null || properties.size() == 0) {
			properties = InputHandler.readSystemProperties(PROPERTY_FILE_NAME);
		}
		return properties.get(key);
	}
	
	public static void addProperty(String key, String value) {
		if (properties == null || properties.size() == 0) {
			properties = InputHandler.readSystemProperties(PROPERTY_FILE_NAME);
		}
		
		properties.put(key, value);
	}

	public static String addWorkDir(String s) {
		return combineDir(WORK_DIR, s);
	}

	private static String combineDir(String d1, String d2) {
		StringBuffer sb = new StringBuffer(d1);
		sb.append(File.separator);
		sb.append(d2);
		return sb.toString();
	}

	public static boolean getBoolean(String key, boolean defaultValue) {
		String value = get(key);
		if (value != null) {
			return value.toLowerCase().startsWith("t");
		} else {
			return defaultValue;
		}
	}

	public static int getInteger(String key, int defaultValue) {
		String value = get(key);
		if (value != null) {
			return Integer.valueOf(value);
		} else {
			return defaultValue;
		}
	}

	public static double getDouble(String key, double defaultValue) {
		String value = get(key);
		if (value != null) {
			return Double.valueOf(value);
		} else {
			return defaultValue;
		}
	}

}
