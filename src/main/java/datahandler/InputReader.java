/*
 * Author: Eric Yuan
 * Created on Aug 23, 2005
 */
package datahandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Enter type description here.
 * @author Eric Yuan
 * <p>
 * Created on Aug 23, 2005
 */
public class InputReader {
	
	//protected String path;
	//protected String fileName;

	/** buffered character input stream */
	protected BufferedReader in;

	/** the tokenizer is instatiated for each line read by readLine( ). */
	protected StringTokenizer t;

	protected String line;

	public InputReader(String fileName){
		this(".", fileName);
	}

	/**
	 * Create a new InputReader object.
	 * @param path
	 * @param name
	 */
	public InputReader(String path, String name){
		super();
		try{
			//this.path = path;
			//this.fileName = name;
			File file = new File(path, name);
			FileReader reader = new FileReader(file);
			in = new BufferedReader(reader);
		}catch (FileNotFoundException fe){
			System.err.println("file not found in InputReader constructor: "+fe.getMessage());
		}
	}
	
	/** reads the next line of input and creates the associated StringTokenizer. */
	public boolean readLine( ) {
		try {
			while(true){
				line = in.readLine();
				if (line==null) return false;
				line = line.trim();
				if (line.length()!=0 && !line.startsWith("#")) break;
			}
			//System.out.println(in.readLine());
			//System.out.println(in.readLine());
			t = new StringTokenizer( line );
			return true;
		} catch ( IOException e2 ) {
			System.err.println( "*** IO Exception ***" );
			System.exit( 1 );
			return false;
		}
	}

	/** reads the next line of input and creates the associated StringTokenizer. */
	public boolean readLine_db( ) {
		try {
			while(true){
				line = in.readLine();
				if (line==null) return false;
				line = line.trim();
				if (line.length()!=0 && !line.startsWith("#")) break;
			}
			//System.out.println(in.readLine());
			//System.out.println(in.readLine());
			t = new StringTokenizer( line , ";" );
			return true;
		} catch ( IOException e2 ) {
			System.err.println( "*** IO Exception ***" );
			System.exit( 1 );
			return false;
		}
	}

	/** reads the next line of input and creates the associated StringTokenizer. */
	public String[] readLine2Parts() {
		String [] parts;
		
		try {
			while(true){
				line = in.readLine();
				if (line==null) {
					return null;
				}
				line = line.trim();
				if (line.length()!=0 && !line.startsWith("#")) {
					break;
				}
			}
			parts = line.split("\t");
			return parts;
		} catch ( IOException e2 ) {
			System.err.println( "*** IO Exception ***" );
			System.exit( 1 );
			return null;
		}
	}

	public int 		tokenCount( ) 	{ return t.countTokens( ); }
	public boolean 	hasMore( ) 		{ return t.hasMoreTokens( ); }

	///////////////////////////////////////////////   Integer types:

	/** consumes the next input token and converts into a short value
		@return		short value represented by the next token
	 */
	public short nextShort( ) {
		try { return Short.parseShort( t.nextToken( ) );
		} catch( NoSuchElementException e0 ) {
			System.err.println( "No more input available" );
		} catch( NumberFormatException e1 ) {
			System.err.println( e1.getMessage( ) );
			System.err.println( "ist keine g�ltige short" );
		}
		return 0;
	 } // nextShort



	/** consumes the next input token and converts into a int value
		@return		int value represented by the next token
	 */
	public int nextInt( ) {
		try { return Integer.parseInt( t.nextToken( ) );
		} catch( NoSuchElementException e0 ) {
			System.err.println( "No more input available" );
		} catch( NumberFormatException e1 ) {
			System.err.println( e1.getMessage( ) );
			System.err.println( "ist keine g�ltige int" );
		}
		return 0;
	 } // nextInt



	/** consumes the next input token and converts into a long value
		@return		long value represented by the next token
	 */
	public long nextLong( ) {
		try { return Long.parseLong( t.nextToken( ) );
		} catch( NoSuchElementException e0 ) {
			System.err.println( "No more input available" );
		} catch( NumberFormatException e1 ) {
			System.err.println( e1.getMessage( ) );
			System.err.println( "ist keine g�ltige long" );
		}
		return 0;
	 } // nextLong



	///////////////////////////////////////////////   Character:

	/** consumes the next input token and converts into a character.
		Attention: single characters must be separated by
		white space characters.
		@return		charcter represented by the next token
	 */
	public char nextChar( ) {
		return t.nextToken( ).charAt( 0 );
	 } // nextchar


	///////////////////////////////////////////////   Boolean:

	/** consumes the next input token and converts into a boolean value
		@return		boolean value represented by the next token
	 */
	public boolean nextBoolean( ) {
		return new Boolean( t.nextToken( ) ).booleanValue( );
	 } // nextBoolean



	///////////////////////////////////////////////   Float/Double:

	/** consumes the next input token and converts into a float value
		@return		float value represented by the next token
	 */
	public float nextFloat( ) {
		try { return new Float( t.nextToken( ) ).floatValue( );
		} catch( NoSuchElementException e0 ) {
			System.err.println( "No more input available" );
		} catch( NumberFormatException e1 ) {
			System.err.println( e1.getMessage( ) );
			System.err.println( "ist keine g�ltige float" );
		}
		return Float.NaN;
	 } // nextFloat


	/** consumes the next input token and converts into a double value
		@return		double value represented by the next token
	 */
	public double nextDouble( ) {
		try { return new Double( t.nextToken( ) ).doubleValue( );
		} catch( NoSuchElementException e0 ) {
			System.err.println( "No more input available" );
		} catch( NumberFormatException e1 ) {
			System.err.println( e1.getMessage( ) );
			System.err.println( "ist keine g�ltige double" );
		}
		return Double.NaN;
	 } // nextDouble


	///////////////////////////////////////////////   String:

	public String nextString( ) {
		try { return t.nextToken( );
		} catch( NoSuchElementException e0 ) {
			System.err.println( "No more input available" );
		}
		return "";
	 } // nextString

	/**
	 * @return Returns the line.
	 */
	public String getLine() {
		return line;
	}
	
	/**
	 * returns all content in the file as a String
	 * @return all content in the file as a String
	 */
	public String readAll() {
		StringBuffer sb = new StringBuffer("");
		try {
			while (true) {
					line = in.readLine();
				if (line == null) {
					break;
				}
				sb.append(line);
				sb.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	public void copyFileTo(String pathNew, String nameNew) {
		File dir = new File(pathNew);
		
		if (! dir.exists()) {
			dir.mkdirs();
		}
		
		OutputHandler.print2File(pathNew, nameNew, readAll());
	}
}
