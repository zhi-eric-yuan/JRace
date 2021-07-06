/*
 * Created by Eric Yuan on Mar 25, 2008
 */
package datahandler;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author yuan
 * Created on Mar 25, 2008
 */
public class MathOptionParser extends CmdLineParser {

	public static final Option FUNCTION = 
		new CmdLineParser.Option.StringOption('f', "fun");

	public static final Option DIM = 
		new CmdLineParser.Option.IntegerOption('d', "dim");

	public static final Option INSTANCE = 
		new CmdLineParser.Option.StringOption('i', "instance");

	public static final Option SCENARIO = 
		new CmdLineParser.Option.IntegerOption('n', "scenario");

	public static final Option START = 
		new CmdLineParser.Option.StringOption('s', "start");

	public static final Option POINT = 
		new CmdLineParser.Option.StringOption('p', "point");

	public static final Option OUT = 
		new CmdLineParser.Option.StringOption('o', "outdir");

	public static final Option HELP = 
		new CmdLineParser.Option.BooleanOption('h', "help");

	public static final Option EVA = 
		new CmdLineParser.Option.BooleanOption('e', "eva");

	public static final Option DEBUG = 
		new CmdLineParser.Option.BooleanOption('v', "debug");

	public static final Option ALGO = new CmdLineParser.Option.StringOption('a', "algo");

	List optionHelpStrings = new ArrayList();

	/**
	 * Create a new DBCmdLineParser.java object.
	 */
	public MathOptionParser() {
		super();
		addOption(FUNCTION);
		addOption(DIM);		
		addOption(HELP);
		addOption(INSTANCE);		
		addOption(START);
		addOption(SCENARIO);
		addOption(POINT);
		addOption(EVA);
		addOption(DEBUG);
		addOption(ALGO);

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
    	addHelp(MathOptionParser.FUNCTION, "the function to be solved");
    	addHelp(MathOptionParser.DIM, "dimension of the test function");
    	addHelp(MathOptionParser.HELP, "display this help text and exit");
    	addHelp(MathOptionParser.INSTANCE, "the file of instance");
    	addHelp(MathOptionParser.SCENARIO, "the index of the scenario to be tested");
    	addHelp(MathOptionParser.START, "the file of starting points");
    	addHelp(MathOptionParser.POINT, "the point to be evaluated. " +
    			"Should be same dimension as in the instance file");
    	addHelp(MathOptionParser.OUT, "the output file");
    	addHelp(MathOptionParser.EVA, "to evaluate a point on a given function");
    	addHelp(MathOptionParser.DEBUG, "display all debug messages");
    	addHelp(MathOptionParser.ALGO, "select an algorithm to be evaluated, bobyqa, " +
    			"cmaes or simplex");
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
