/*
 * Created by Eric Yuan on Mar 25, 2008
 */
package datahandler;

import java.util.ArrayList;
import java.util.List;

import jargs.examples.gnu.AutoHelpParser;
import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.UnknownOptionException;

/**
 * @author yuan
 * Created on Mar 25, 2008
 */
public class CmdLineHandler {
	
	public static CmdLineParser parser;
	//public static MathOptionParser mParser;
	List optionHelpStrings = new ArrayList();
	AutoHelpParser help = new AutoHelpParser();


	/**
	 * Create a new CmdLineHandler.java object.
	 */
	public CmdLineHandler() {
		// TODO Auto-generated constructor stub
	}

	public CopsOptionParser readCmdLineArgs (String[] args) {
		return (CopsOptionParser) readCmdLineArgs(args, true);
	}

	/**
	 * 
	 * @param args command line arguments
	 * @param useCops whether to use the CopsOptionParser or MathOptionParser
	 * @return
	 */
	public CmdLineParser readCmdLineArgs (String[] args, boolean useCops) {
		initParser(useCops);
		try {
			parser.parse(args);
		} catch (IllegalOptionValueException e) {
            System.err.println(e.getMessage());
            parser.printUsage();
            System.exit(2);
		} catch (UnknownOptionException e) {
            System.err.println(e.getMessage());
            parser.printUsage();
            System.exit(2);
		}
		
        if (Boolean.TRUE.equals(parser.getOptionValue(CopsOptionParser.HELP))) {
            parser.printUsage();
            System.exit(0);
        }
               
        return parser;
	}

	protected void initParser() {
		initParser(true);
	}
	protected void initParser(boolean useCops) {
		if (useCops) {
			parser = new CopsOptionParser();
		} else {
			parser = new MathOptionParser();
		}
	}	


}
