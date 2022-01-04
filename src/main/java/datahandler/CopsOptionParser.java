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
public class CopsOptionParser extends CmdLineParser {


	public static final Option SCENARIO = 
		new CmdLineParser.Option.StringOption('s', "scenario");

	public static final Option VALIDATE_ONLY = 
			new CmdLineParser.Option.BooleanOption('v', "validate-only");

	public static final Option CONF = 
			new CmdLineParser.Option.StringOption('c', "conf");

	public static final Option HELP = 
		new CmdLineParser.Option.BooleanOption('h', "help");

	public static final Option WARM_START =
			new CmdLineParser.Option.BooleanOption('w', "warm-start");

	List optionHelpStrings = new ArrayList();

	/**
	 * Create a new DBCmdLineParser.java object.
	 */
	public CopsOptionParser() {
		super();
		addOption(HELP);
		addOption(SCENARIO);
		addOption(VALIDATE_ONLY);
		addOption(CONF);
		addOption(WARM_START);

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
    	addHelp(CopsOptionParser.SCENARIO, "the scenario file");
    	addHelp(CopsOptionParser.VALIDATE_ONLY, "whether only perform validation without tuning");
		addHelp(CopsOptionParser.CONF, "the configuration to be validate");
		addHelp(CopsOptionParser.WARM_START, "whether use warm start with a stored evaluation archive");
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
