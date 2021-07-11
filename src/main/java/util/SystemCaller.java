/**
 * 
 */
package util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import datahandler.OutputHandler;

/**
 * @author yuan
 *
 */
public class SystemCaller {

	/**
	 * 
	 */
	public SystemCaller() {
		// TODO Auto-generated constructor stub
	}
	
	public static String call(String cmd) {
		Runtime r = Runtime.getRuntime();
		StringBuffer sb = new StringBuffer(""); 
		
		try	{
			/*
			*	Here	we	are	executing	the	UNIX	command	ls	for	directory	listing.
			*	The	format	returned	is	the	long	format	which	includes	file
			*	information	and	permissions.
			*/																																				
			OutputHandler.writeln("runs " + cmd);
			//Process	p = r.exec(cmd);
			Process	p = r.exec(cmd);
			InputStream	in = p.getInputStream();
			BufferedInputStream	buf = new BufferedInputStream(in);
			InputStreamReader inread = new InputStreamReader(buf);
			BufferedReader bufferedreader = new BufferedReader(inread);

			//Read the command output
			String line;
			while ((line = bufferedreader.readLine()) != null) {
				System.out.println(line);
				sb.append(line);
				sb.append("\n");
			}
			// Check for ls failure
			try	{
				if (p.waitFor() != 0) {
					System.err.println("exit value = " + p.exitValue());
                 }
             } catch (InterruptedException e) {
                 System.err.println(e);
             } finally {
                 // Close the InputStream
                 bufferedreader.close();
                 inread.close();
                 buf.close();
                 in.close();
             }
         } catch (IOException e) {
             System.err.println(e.getMessage());
         }
         
         return sb.toString();

	}

	public static String call(String[] cmdarray) {
		Runtime r = Runtime.getRuntime();
		StringBuffer sb = new StringBuffer(""); 
		
		try	{
			/*
			*	Here	we	are	executing	the	UNIX	command	ls	for	directory	listing.
			*	The	format	returned	is	the	long	format	which	includes	file
			*	information	and	permissions.
			*/																																				
			System.out.println("runs " + Arrays.toString(cmdarray));
			//Process	p = r.exec(cmd);
			Process	p = r.exec(cmdarray);
			InputStream	in = p.getInputStream();
			BufferedInputStream	buf = new BufferedInputStream(in);
			InputStreamReader inread = new InputStreamReader(buf);
			BufferedReader bufferedreader = new BufferedReader(inread);

			//Read the command output
			String line;
			while ((line = bufferedreader.readLine()) != null) {
				System.out.println(line);
				sb.append(line);
				sb.append("\n");
			}
			// Check for ls failure
			try	{
				if (p.waitFor() != 0) {
					System.err.println("exit value = " + p.exitValue());
                 }
             } catch (InterruptedException e) {
                 System.err.println(e);
             } finally {
                 // Close the InputStream
                 bufferedreader.close();
                 inread.close();
                 buf.close();
                 in.close();
             }
         } catch (IOException e) {
             System.err.println(e.getMessage());
         }
         
         return sb.toString();

	}

}
