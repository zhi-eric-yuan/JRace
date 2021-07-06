/*
 * Created by Eric Yuan on Mar 25, 2008
 */
package datahandler;

import jargs.gnu.CmdLineParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author yuan
 * Created on Mar 25, 2008
 */
public class DBOptionParser extends CmdLineParser {

	public static final Option MMAS = 
		new CmdLineParser.Option.BooleanOption("mmas");

	public static final Option ACS = 
		new CmdLineParser.Option.BooleanOption("acs");

	public static final Option ITANTS = 
		new CmdLineParser.Option.BooleanOption("itants");

	public static final Option GREEDY = 
		new CmdLineParser.Option.BooleanOption("greedy");

	public static final Option IG = 
		new CmdLineParser.Option.BooleanOption("ig");

	public static final Option HELP = 
		new CmdLineParser.Option.BooleanOption('h', "help");

	public static final Option ALPHA = 
		new CmdLineParser.Option.DoubleOption('a', "alpha");

	public static final Option BETA = 
		new CmdLineParser.Option.DoubleOption('b', "beta");

	public static final Option RHO = 
		new CmdLineParser.Option.DoubleOption('e', "rho");

	public static final Option Q_0 = 
		new CmdLineParser.Option.DoubleOption('q', "q0");

	public static final Option GAMMA = 
		new CmdLineParser.Option.DoubleOption('g', "gamma");

	public static final Option DESTRUCT = 
		new CmdLineParser.Option.DoubleOption('d', "destruct");

	public static final Option NOICE = 
		new CmdLineParser.Option.DoubleOption('n', "noice");

	public static final Option TIME = 
		new CmdLineParser.Option.DoubleOption('t', "time");

	public static final Option TEMP_CTRL = 
		new CmdLineParser.Option.DoubleOption('c', "tempctrl");

	public static final Option ANTS = 
		new CmdLineParser.Option.IntegerOption('m', "ants");

	public static final Option RECONSTRUCT = 
		new CmdLineParser.Option.IntegerOption('r', "reconstruct");
	
	public static final Option INSTANCE = 
		new CmdLineParser.Option.StringOption('i', "instace");
	
	public static final Option TIME_WINDOW = 
		new CmdLineParser.Option.IntegerOption('w', "timewindow");
	
	public static final Option RPS = 
		new CmdLineParser.Option.DoubleOption('p', "rps");	

	
	List optionHelpStrings = new ArrayList();

	/**
	 * Create a new DBCmdLineParser.java object.
	 */
	public DBOptionParser() {
		super();
		addOption(MMAS);
		addOption(ACS);
        addOption(ITANTS);
        addOption(IG);
		addOption(ALPHA);
		addOption(BETA);
		addOption(RHO);
		addOption(Q_0);
		addOption(GAMMA);
		addOption(DESTRUCT);
		addOption(ANTS);
		addOption(RECONSTRUCT);
		addOption(TIME);
		addOption(HELP);
		addOption(INSTANCE);
		addOption(TIME_WINDOW);
		addOption(GREEDY);
		addOption(NOICE);
		addOption(RPS);
		addOption(TEMP_CTRL);
		
	}

	public void printUsage() {
	    System.err.println("usage: prog [options]");
	    if (optionHelpStrings == null || optionHelpStrings.isEmpty()) {
	    	addHelpOptions();
	    }
	    for (Iterator i = optionHelpStrings.iterator(); i.hasNext(); ) {
	    	System.err.println(i.next());
	    }
	}
	protected void addHelpOptions() {
    	addHelp(DBOptionParser.TIME, "the maximum execution time");
    	addHelp(DBOptionParser.INSTANCE, "the name of the instance");
    	addHelp(DBOptionParser.TIME_WINDOW, "the time window size of the instance");
    	addHelp(DBOptionParser.ITANTS, "apply Iterated Ants with Max-Min Ant System");
    	addHelp(DBOptionParser.IG, "apply Iterated Greedy");
    	addHelp(DBOptionParser.MMAS, "apply Max-Min Ant System");
    	addHelp(DBOptionParser.ACS, "apply Ant Colony System");
    	addHelp(DBOptionParser.GREEDY, "apply Randomized Greedy Search");
    	addHelp(DBOptionParser.NOICE, "# (0.0000001 - 1.0) degree of random noice in Randomized Greedy Search");
    	addHelp(DBOptionParser.ALPHA, "# alpha (influence of pheromone trails)");
    	addHelp(DBOptionParser.BETA, "# beta (influence of heuristic information)");
    	addHelp(DBOptionParser.RHO, "# rho: pheromone trail evaporation");
    	addHelp(DBOptionParser.Q_0, "# q_0: prob. of best choice in tour construction");
    	addHelp(DBOptionParser.GAMMA, "# Factor specifying the gap between the maximum and minimum pheromone trail in MMAS");
    	addHelp(DBOptionParser.TEMP_CTRL, "# 0.0000001 - 0.02, the metropolis temperature control factor for Iterated Greedy");
    	addHelp(DBOptionParser.ANTS, "# number of ants");
    	addHelp(DBOptionParser.DESTRUCT, "# the percentage of the solution to be destructed");
    	addHelp(DBOptionParser.RECONSTRUCT, "# number of reconstruction runs after each destruction");
    	addHelp(DBOptionParser.RPS, "# (-1, 1) whether to select the starting vehicle probabilisitically");
    	addHelp(DBOptionParser.HELP, "display this help text and exit");
	}

	public Option addHelp(Option option, String helpString) {
		if (option.shortForm() != null) {
			optionHelpStrings.add(" -" + option.shortForm() + " / --" + option.longForm() + ":\t\t" + helpString);
		} else {
			optionHelpStrings.add("    / --" + option.longForm() + ":\t\t" + helpString);
		}
		return option;
	}

}
