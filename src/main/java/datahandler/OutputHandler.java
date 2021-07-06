package datahandler;
/*
 * Author: Eric Yuan
 * Created on 23.08.2005
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import util.SystemProperty;

/**
 * Enter type description here.
 * @author Eric Yuan
 * <p>
 * Created on 23.08.2005
 */
public class OutputHandler {
	
	//public static boolean debug = SystemProperty.get(SystemProperty.debug).equalsIgnoreCase("true");
	public static boolean debug = 
		SystemProperty.get(SystemProperty.DEBUG).equalsIgnoreCase("true");
	
	public static StringBuffer out = new StringBuffer("");
	//static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final int MATRIX_WIDTH = 20;
	public static final int MAX_LINE_WIDTH = 80;
	public static final int MAX_STREAM_LENGTH = 3000000;

	/**
	 * Construct a new OutputHandler object. 
	 */
	public OutputHandler() {
		super();
	}

	public static void writeOut() {
		System.out.println(out.toString());
		out = new StringBuffer();
	}

	public static void appendTimeValue(double time, double value) {
		out.append(time);
		out.append(" ");
		out.append(value);
		out.append("\n");
	}
	
	public static void writeln(String s){
	    if(debug){
	    	System.out.println(s);
	    }
	}
	
	public static void writeln(double s){
	    if(debug){
	    	System.out.println(s);
	    }
	}

	public static void writeln(int s){
	    if(debug){
	    	System.out.println(s);
	    }
	}

	public static void writeln(){
	    if(debug){
	        System.out.println();
	    }
	}
	
	public static void write(String s){
	    if(debug){
	        System.out.print(s);
	    }
	}

	public static void writeMatrix2HTML(int[][] a) {
		if (debug) {
			StringBuffer sb = new StringBuffer("<table>\n");
			for (int i=0 ; i<a.length ; i++) {
				sb.append("<tr>\n");
				for (int j=0 ; j<a[i].length ; j++) {
					sb.append("<td>");
					sb.append(a[i][j]);
					sb.append("</td>");
				}
				sb.append("\n</tr>");
			}
			sb.append("</table>\n");
			System.out.print(sb.toString());
		}
	}
	
	public static void writeMatrix2Txt(double[][] a) {
		if (debug) {
			System.out.print(returnMatrix2Txt(a));
		}
	}

	public static String returnMatrix2Txt(double[][] a) {
		StringBuffer sb = new StringBuffer("");
		for (int i=0 ; i<a.length ; i++) {
			for (int j=0 ; j<a[i].length ; j++) {
				sb.append(a[i][j]);
				if (j < a[i].length - 1) {
					sb.append(" ");
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public static void writeMatrix2HTMLPart(int[][] a) {
		if (debug) {
			StringBuffer sb = new StringBuffer("<table>\n");
			for (int i=0 ; i<a.length ; i++) {
				sb.append("<tr>\n");
				for (int j=0 ; j<a[i].length ; j++) {
					sb.append("<td>");
					sb.append(a[i][j]);
					sb.append("</td>");
				}
				sb.append("\n</tr>");
				System.out.print(sb.toString());
				sb = new StringBuffer("");
			}
			sb.append("</table>\n");
			System.out.print(sb.toString());
		}
	}

	
	public static String returnMatrix2OPL(int[][] a) {
		StringBuffer sb = new StringBuffer("[\n");
		for (int i=0 ; i<a.length ; i++) {
			sb.append("\t[");
			for (int j=0 ; j<a[i].length ; j++) {
				if (j != 0) {
					sb.append(", ");
				}
				if ( j % MATRIX_WIDTH == 0) {
					sb.append("\n\t ");
				}
				sb.append(a[i][j]);
			}
			sb.append(" ]\n");
		}
		sb.append("]\n");
		return sb.toString();
	}

	public static void writeMatrix2OPL(int[][] a) {
		if (debug) {
			StringBuffer sb = new StringBuffer("[\n");
			for (int i=0 ; i<a.length ; i++) {
				sb.append("\t[");
				for (int j=0 ; j<a[i].length ; j++) {
					if (j != 0) {
						sb.append(", ");
					}
					if ( j % MATRIX_WIDTH == 0) {
						sb.append("\n\t ");
					}
					sb.append(a[i][j]);
				}
				sb.append(" ]\n");
				System.out.println(sb.toString());
				sb = new StringBuffer("");
			}
			sb.append("]\n");
			System.out.println(sb.toString());
		}
	}

	public static String returnMatrix2OPL(long[][] a) {
		StringBuffer sb = new StringBuffer("[\n");
		for (int i=0 ; i<a.length ; i++) {
			sb.append("\t[");
			for (int j=0 ; j<a[i].length ; j++) {
				if (j != 0) {
					sb.append(", ");
					if ( j % MATRIX_WIDTH == 0) {
						sb.append("\n\t ");
					}
				}
				sb.append(a[i][j]);
			}
			sb.append(" ]\n");
		}
		sb.append("]\n");
		return sb.toString();
	}

	public static String returnMatrix2OPL(String[][] a) {
		StringBuffer sb = new StringBuffer("[\n");
		for (int i=0 ; i<a.length ; i++) {
			sb.append("\t[");
			for (int j=0 ; j<a[i].length ; j++) {
				if (j != 0) {
					sb.append(", ");
					if ( j % (MATRIX_WIDTH / 4) == 0) {
						sb.append("\n\t ");
					}
				}
				sb.append(a[i][j]);
			}
			sb.append(" ]\n");
		}
		sb.append("]\n");
		return sb.toString();
	}

	public static <T> void writeArray(T[] a) {
		if (debug) {
			System.out.println(returnArray(a));
		}
	}

	public static void writeArray(double[] a) {
		if (debug) {
			System.out.println(returnArray(a));
		}
	}

	public static void writeArray(int[] a) {
		if (debug) {
			System.out.println(returnArray(a));
		}
	}

	public static <T> String returnArray(T[] a) {
		return returnArray(a, " ");
	}

	public static <T> String returnArray(T[] a, String sep) {
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < a.length; i++) {
			sb.append(a[i]);
			sb.append(sep);
		}
		
		return sb.toString();
	}

	public static String returnArray(double[] a) {
		return returnArray(a, " ");
	}

	public static String returnArray(int[] a) {
		return returnArray(a, " ");
	}

	public static String returnArray(double[] a, String sep) {
		if (a != null) {
			StringBuffer sb = new StringBuffer();
			
			for (int i = 0; i < a.length; i++) {
				sb.append(a[i]);
				sb.append(sep);
			}
			
			return sb.toString();
		} else {
			return null;
		}
	}

	public static String returnArray(int[] a, String sep) {
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < a.length; i++) {
			sb.append(a[i]);
			sb.append(sep);
		}
		
		return sb.toString();
	}

	public static boolean print2File(String filename, String msg) {
		FileOutputStream out;
		mkdir(filename);
		
		try {
			out = new FileOutputStream(filename);
			PrintStream p = new PrintStream( out );
			p.print(msg);
			p.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

	}
	
	/**Method's description here.
	 * @param filename
	 */
	private static void mkdir(String filename) {
		int index = filename.lastIndexOf("\\");
		if (index < 0) {
			index = filename.lastIndexOf("/");
		}
		if (index >= 0) {
			String path = filename.substring(0, index);
			//System.out.println(path);
			File dir = new File(path);
			
			if (! dir.exists()) {
				dir.mkdirs();
			}
		}
	}

	public static boolean print2File(String path, String name, String msg) {
		return print2File(path + "/" + name, msg);
	}
		
	public static void printMatrix2File(String path, String name, 
										String[][] matrix) {
		StringBuffer sb = new StringBuffer("");
		for (int i=0 ; i<matrix.length ; i++) {
			for (int j=0 ; j<matrix[i].length ; j++) {
				sb.append(matrix[i][j]);
				if (j != matrix[i].length - 1) {
					sb.append("\t");
				}
			}
			sb.append("\n");
		}
		print2File(path + "/" + name, sb.toString());
		
	}
	

	public static String returnArray2OPL(long [] a) {
		StringBuffer sb = new StringBuffer("[\n\t");
		for (int i = 0; i < a.length; i++) {
			if (i != 0) {
				sb.append(", ");
				if (i % MATRIX_WIDTH == 0) {
					sb.append("\n\t ");
				}
			}
			sb.append(a[i]);
		}
		sb.append("]\n");
		return sb.toString();
	}
	
	public static String returnArray2OPL(String [] s) {
		StringBuffer sb = new StringBuffer("[\n\t");
		for (int i = 0; i < s.length; i++) {
			if (i != 0) {
				sb.append(", ");
				if (i % (MATRIX_WIDTH / 4) == 0) {
					sb.append("\n\t ");
				}
			}
			sb.append(s[i]);
		}
		sb.append("]\n");
		return sb.toString();
	}
	
	public static String formatTime(int time){
		int minutes = time%60;
		if (minutes<10) {
			return time/60 + ":0" + minutes;			
		}
		else {
			return time/60 + ":" + minutes;
		}
	}

	public static String formatTime(long time){
		long minutes = time%60;
		if (minutes<10) {
			return time/60 + ":0" + minutes;			
		}
		else {
			return time/60 + ":" + minutes;
		}
	}

	public static String formatTime(double time){
		return formatTime(Math.round(time));
	}
	
	public static String getDay2String(int day) {
		switch (day) {
			case 0: return "Mon";
			case 1: return "Tue";
			case 2: return "Wed";
			case 3: return "Thur";
			case 4: return "Fri";
			default: return "ERROR";
		}
	}

	

	public static void printParam(double[] param) {
		if (debug) {
			System.out.println(returnParam(param));
		}		
	}

	public static String returnParam(double[] param) {
		StringBuffer sb = new StringBuffer("");
		for (int i=0 ; i<param.length ; i++) {
			sb.append(param[i]);
			sb.append("\t");
		}
		return sb.toString();
	}
	

	public static void writeRelationStatistics(int t, int b, int w, int g) {
		if (debug) {
			System.out.println(returnRelationStatistics(t, b, w, g));
		}		
	}

	private static String returnRelationStatistics(int t, int b, int w,
			int g) {
		StringBuffer sb = new StringBuffer("In total ");
		sb.append(t);
		sb.append(" relations, \n");
		sb.append(b);
		sb.append(" are black i.e. naturally bad, \n");
		sb.append(w);
		sb.append(" are white i.e. naturally good, \n");
		sb.append(g);
		sb.append(" are gray i.e. can be either good or bad.");
		
		return sb.toString();
	}

	public static void writeOverlap(int [][] overlap) {
		if (debug) {
			System.out.println(returnOverlap(overlap));
		}		
	}

	private static String returnOverlap(int [][] overlap) {
		StringBuffer sb = new StringBuffer("[");
		sb.append(overlap[0][0]);
		sb.append("-");
		sb.append(overlap[0][1]);
		sb.append("] U [");
		sb.append(overlap[1][0]);
		sb.append("-");
		sb.append(overlap[1][1]);
		sb.append("]");
		
		return sb.toString();
	}
	
	public static String returnArrayList(ArrayList<double[]> arrays) {
		StringBuffer sb = new StringBuffer();
		double[] item;
		
		for(int i = 0; i < arrays.size(); i++) {
			item = (double[]) arrays.get(i);
			for (int j = 0; j < item.length - 1; j++) {
				sb.append(item[j]);
				sb.append(" ");
			}
			sb.append(item[item.length - 1]);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public static void printArrayList(ArrayList<double[]> arrays) {
		if (debug) {
			System.out.println(returnArrayList(arrays));
		}
	}

	public static boolean printArrayList2File(String filename, ArrayList<double[]> arrays) {
		
		return print2File(filename, returnArrayList(arrays));

	}


}
