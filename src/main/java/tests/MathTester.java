/*
 * Created by @user 
 */
package tests;

import java.io.*;
import java.util.*;

import com.google.gson.Gson;
import datahandler.InputReader;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.MultivariateFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.direct.BOBYQAOptimizer;
import org.apache.commons.math.optimization.direct.CMAESOptimizer;
import org.apache.commons.math.optimization.direct.NelderMeadSimplex;
import org.apache.commons.math.optimization.direct.SimplexOptimizer;
import org.apache.commons.math.optimization.direct.UniformRandomOptimizer;
import org.apache.commons.math.stat.inference.TestUtils;
import org.apache.commons.math.stat.inference.WilcoxonSignedRankTest;
import org.apache.commons.math.stat.inference.WilcoxonSignedRankTestImpl;
import org.apache.commons.math.stat.ranking.NaturalRanking;
import org.apache.commons.math.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import algo.Archive;
import algo.Configuration;
import algo.Instance;
import algo.Parameter;
import datahandler.CopsOptionParser;
import datahandler.OutputHandler;
import stat.inference.FriedmanTest;
import stat.inference.FriedmanTestImpl;
import tests.BenchmarkFunction.Rosen;
import tune.Tuner;
import util.CollectionHandler;
import util.MathHelp;
import util.SystemCaller;

/**
 * @author yuan
 * Created on Nov 30, 2011
 *
 */
public class MathTester {

	private static Logger log = LoggerFactory.getLogger(MathTester.class);

	/**
	 * 
	 */
	public MathTester() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Tuner.parser = new CopsOptionParser();
		//testLog();
		//testArray();
		//testStringTokenizer();
		//testHashMap();
		testGson();
		//testHash();
		//testMath();
		//testStatTests();
		//testWilcoxon();
		//testFriedman();
		//testFRace();
		//testRankArray();
		//testCmaes();
		//testUniform();
		//testBobyqa();
		//testSimplex();
		//testSmac();
	}

	private static void testGson() {
		HashMap<String, Double> inMap = new HashMap<>();
		inMap.put("A", 1.0);
		inMap.put("B", 2.0);
		HashMap<String, HashMap<String, Double>> map = new HashMap<>();
		map.put("a", inMap);

		System.out.println(map);

		Gson gson = new Gson();
		//gson.toJson(map);
		gson.toJson(1);            // ==> 1
		gson.toJson("abcd");       // ==> "abcd"
		System.out.println(gson.toJson(map));
		gson.toJson(new Long(10)); // ==> 10
		int[] values = { 1 };
		gson.toJson(values);       // ==> [1]

// Deserialization
		int one = gson.fromJson("1", int.class);
		//Integer one = gson.fromJson("1", Integer.class);
		//Long one = gson.fromJson("1", Long.class);
		//Boolean false = gson.fromJson("false", Boolean.class);
		String str = gson.fromJson("\"abc\"", String.class);
		String[] anotherStr = gson.fromJson("[\"abc\"]", String[].class);
		InputReader ir = new InputReader("archive.json");
		String jsonText = ir.readAll();
		System.out.println(jsonText);
		Archive arch = gson.fromJson(jsonText, Archive.class);
		System.out.println(arch);
	}

	private static void testHashMap() {
		HashMap<String, Double> inMap = new HashMap<>();
		inMap.put("A", 1.0);
		inMap.put("B", 2.0);
 		HashMap<String, HashMap<String, Double>> map = new HashMap<>();
 		map.put("a", inMap);

		System.out.println(map);
		//Properties properties = new Properties();
		//properties.putAll(map);
		//properties.store(new FileOutputStream("foo.properties"), null);
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream("data.ser"));
			out.writeObject(map);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		HashMap<String, HashMap<String, Double>> map2 = null;

		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream("data.ser"));
			map2 = (HashMap<String, HashMap<String, Double>>) in.readObject();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(map2);
	}

	private static void testStringTokenizer() {
		System.out.println((Double.MAX_VALUE + Double.MAX_VALUE)/2);
		String command = "bash jsprit_latam_hard/run.sh -i ~/Projects/logistics-optimization/solutions-comparator/data/1000_hard_latam_20210628-0704/37473688.json -s 870316129 -c \"late_dt=10.0 ,dist=10.0 \"";
		StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        System.out.println(st.countTokens());
        for (int i = 0; st.hasMoreTokens(); i++)
            cmdarray[i] = st.nextToken();
        System.out.println(Arrays.toString(cmdarray));
        System.out.println(command.substring(command.length()).indexOf(' '));
        String s = "\"hello world \" eaf wsd";
        st = new StringTokenizer(s, "\"");
        while (st.hasMoreTokens()) {
        	System.out.println(st.nextToken());
        }
        //SystemCaller.call("python hello.py");
        /*SystemCaller.call(new String[] {"python", "/Users/temporaryadmin/Projects/logistics-optimization/solutions-comparator/run_algo.py",
        		"--binary", "/Users/temporaryadmin/Projects/logistics-assignment/logistics-tools/target/universal/logistics-tools-11.0.1-64-gaf0a9f0-SNAPSHOT/bin/logistics-tools", 
        		"--metric", "StackingOverLateDeliveries()", 
        		//"--modifier", "SoftConstraintsLinear(late_dt=10.0, dist=10.0)", 
        		"--instance", 
        		"/Users/temporaryadmin/Projects/logistics-optimization/solutions-comparator/data/1000_hard_latam_20210628-0704/37473688.json",
        		"--seed", "870316129", 
        		"--modifier", "SoftConstraintsLinear(late_dt=0.49 ,dist=9.1 ,bag_t =0.4 ,late_pu=7.4 )"
        		});
        */
        SystemCaller.call(new String[] {"python", "/Users/temporaryadmin/Projects/logistics-optimization/solutions-comparator/run_algo.py",
        		"--binary", "/Users/temporaryadmin/Projects/logistics-assignment/logistics-tools/target/universal/logistics-tools-11.0.1-64-gaf0a9f0-SNAPSHOT/bin/logistics-tools", 
        		"--metric", "StackingOverLateDeliveries()", 
        		//"--modifier", "SoftConstraintsLinear(late_dt=10.0, dist=10.0)", 
        		"--instance", 
        		"/Users/temporaryadmin/Projects/logistics-optimization/solutions-comparator/data/1000_hard_latam_20210628-0704/37473688.json",
        		"--seed", "870316129", 
        		"--modifier", "SoftConstraintsLinear(late_dt=0.49 ,dist=9.1 ,bag_t =0.4 ,late_pu=7.4 )"
        		});
        //SystemCaller.call("python /Users/temporaryadmin/Projects/logistics-optimization/solutions-comparator/run_algo.py --binary /Users/temporaryadmin/Projects/logistics-assignment/logistics-tools/target/universal/logistics-tools-11.0.1-64-gaf0a9f0-SNAPSHOT/bin/logistics-tools --metric StackingOverLateDeliveries() --instance /Users/temporaryadmin/Projects/logistics-optimization/solutions-comparator/data/1000_hard_latam_20210628-0704/37473688.json --seed 870316129");
	}

	private static void testUniform() {
		double[] startPoint = {0.1, 0.1, 0.1, 0.1, 0.1};
		//double[] startPoint = null;
		double[][] boundaries = {{0, 0, 0, 0, 0}, {1, 1, 1, 1, 1}};
		int dim = boundaries[0].length;
		MultivariateFunction func = new Rosen();
		UniformRandomOptimizer optim = new UniformRandomOptimizer(dim, boundaries);
        RealPointValuePair result = optim.optimize(100000, func, GoalType.MINIMIZE, startPoint);
        System.out.println(result.getValue());
	}

	private static void testLog() {
		log.info("info {}", "hello");
		log.debug("debug");
		log.error("error");
		log.warn("warn");
		log.trace("trace{}", 1);
		System.out.println(log.isDebugEnabled());
		ArrayList<Configuration> part = new ArrayList<Configuration>(0);

	}

	private static void testArray() {
		Integer[] a = {1,2,3,4,5,-1};
		Integer[] b = {4,5,6};
		double[] e = {1,2,3,-1,5};
		System.out.println(Arrays.toString(CollectionHandler.rank(e)));
		Arrays.sort(a);
		System.out.println(Arrays.toString(a));

		ArrayList<Integer> c = new ArrayList<Integer>(Arrays.asList(b));
		c.toArray(a);
		OutputHandler.writeArray(a);
		System.out.println(Math.log(0));
		
		ArrayList<Integer> d = new ArrayList<Integer>();
		d.add(1);
		System.out.println(d.contains(1));
	}

	private static void testHash() {
		Archive arch = new Archive();
		arch.get(null, null);
		List<Integer> list1 = Arrays.asList(new Integer[] { 1, 2, 3 });
		List<Integer> list2 = Arrays.asList(new Integer[] { 1, 2, 3 });
		System.out.println(list1.equals(list2));
		Configuration conf = new Configuration(3);
		conf.addParam(new Parameter("a", "b", 'r'), 1);
		conf.addParam(new Parameter("c", "b", 'r'), 2);
		conf.addParam(new Parameter("d", "b", 'r'), 3);
		Configuration conf2 = new Configuration(4);
		conf2.addParam(new Parameter("a", "b", 'r'), 1);
		conf2.addParam(new Parameter("e", "b", 'r'), 2);
		conf2.addParam(new Parameter("f", "b", 'r'), 3);
		System.out.println(conf.equals(conf2));
		Instance ins = new Instance(1, "a", "b", "c", "d");
		Instance ins2 = new Instance(1, "a", "e", "f", "g");
		System.out.println(ins.equals(ins2));
		arch.put(conf, ins, 300.0);
		System.out.println(arch.put(conf, ins2, 400.0));
		System.out.println(arch.put(conf2, ins, 500.0));
		System.out.println(arch.get(conf2, ins2));
	}

	private static void testMath() {
		double a = 1.00001;
		long b = 1;
		System.out.println(a == b);
		System.out.println(MathHelp.power(2, 8));
		System.out.println(-1.0 * 2 / 2);
		System.out.println(FastMath.pow(4, -1.0 * 2 / 2));
		System.out.println(MathHelp.power(4, -1.0 * 2 / 2));
		String s = "hello";
		System.out.println(s.equals(null));
		
		double[] scores = {0.1};
		System.out.println(CollectionHandler.rank(scores)[0]);
		
		Object x = new Integer(1);
		System.out.println(Double.valueOf(x.toString()));

	}

	private static void testSimplex() {
		int dim = 4;
        SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        optimizer.setSimplex(new NelderMeadSimplex(4));
		MultivariateFunction func = new Rosen();

        RealPointValuePair optimum = null;
        try {
        	optimum = optimizer.optimize(100, func, GoalType.MINIMIZE, new double[] { -3, -2, -1, 0 });
        } catch (TooManyEvaluationsException e) {
        	e.printStackTrace();
        }
        System.out.println(optimum.getValue());
        double [] point = optimum.getPoint();
        for (int i = 0; i < point.length; i++) {
        	System.out.println(point[i]);
        }
	}

	private static void testBobyqa() {
		//int dim = 10;
		double[] startPoint = {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1};
		MultivariateFunction func = new Rosen();
		int dim = startPoint.length;
		int m = 2 * dim + 1;
		BOBYQAOptimizer optim =
            new BOBYQAOptimizer(m, 0.5, 1e-16);
        //System.out.println(optim.getLowerBound());
		RealPointValuePair result = optim.optimize(1000, func, GoalType.MINIMIZE, startPoint);
        //System.out.println(result.getValue());
/*        List<Double> hist = optim.getStatisticsFitnessHistory();
        for (int i = 0; i < hist.size(); i++) {
        	System.out.println(hist.get(i));
        }*/
	}

	private static void testCmaes() {
		//int dim = 10;
		double[] startPoint = {0.1, 0.1, 0.1, 0.1, 0.1};
		MultivariateFunction func = new Rosen();
		CMAESOptimizer optim =
            new CMAESOptimizer();
        RealPointValuePair result = optim.optimize(1000, func, GoalType.MINIMIZE, startPoint);
        System.out.println(result.getValue());

        List<Double> hist = optim.getStatisticsFitnessHistory();
        for (int i = 0; i < hist.size(); i++) {
        	System.out.println(hist.get(i));
        }
	}

	private static void testRankArray() {
		final Integer[] idx = { 0, 1, 2, 3 };
		final float[] data = { 1.7f, -0.3f,  2.1f,  0.5f };

		Arrays.sort(idx, new Comparator<Integer>() {
		    @Override public int compare(final Integer o1, final Integer o2) {
		        return Float.compare(data[o1], data[o2]);
		    }
		});
		
		final double [] a = {1.7, -0.3, 2.1, 0.5};
		OutputHandler.writeArray(CollectionHandler.rank((double[]) a));

		final double [] b = {1.7, -0.3, 2.1, 0.5, 0, 0, 0};
		final double [] c = {1, 1};
		NaturalRanking nr = new NaturalRanking();
		nr.rank(c);

		OutputHandler.writeArray(nr.rank(c));
	}

	private static void testFriedman() {
		FriedmanTest friedman = new FriedmanTestImpl(true);
		
		double [] sample1 = {1, 2, 3, 4, 5, 6, 7, 8, 9};
		double [] sample2 = {1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8, 9.9};
		double [] sample3 = {0.1, 1.2, 2.3, 3.4, 5.5, 6.6, 7.7, 8.8, 9.9};
		double [] sample4 = {0, 0, 0, 0, 0, 0, 0, 0, 1};
		double [] sample5 = {9, 8, 7, 6, 5, 4, 3, 2, 1};
		
		double [][] data1 = {sample1, sample2, sample5};
		double [][] data2 = {sample1, sample2, sample3};
		double [][] data = {sample1, sample2, sample4};
		System.out.println(data1.length);
		
		try {
			System.out.println(friedman.friedmanTest(data1));
			System.out.println(friedman.friedmanTest(data2));
			System.out.println(friedman.friedmanTest(data));
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MathException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void testWilcoxon() {
		WilcoxonSignedRankTest wilcoxon = new WilcoxonSignedRankTestImpl();
		
		double [] sample1 = {1, 2, 3, 4, 5, 6, 7, 8, 9};
		double [] sample2 = {1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8, 9.9};
		double [] sample3 = {0.1, 1.2, 2.3, 3.4, 5.5, 6.6, 7.7, 8.8, 9.9};
		
		double [] a = {2, 3.6, 2.6, 2.6, 7.3, 3.4, 14.9, 6.6, 2.3, 2, 6.8, 8.5};
		double [] b = {3.5, 5.7, 2.9, 2.4, 9.9, 3.3, 16.7, 6.0, 3.8, 4, 9.1, 20.9};
		
		try {
			System.out.println(wilcoxon.wilcoxonSignedRankTest(sample1, sample2, false));
			System.out.println(wilcoxon.wilcoxonSignedRankTest(sample1, sample3, false));
			System.out.println(wilcoxon.wilcoxonSignedRankTest(a, b, false));
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MathException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	private static void testStatTests() {
		double [] sample1 = {1, 2, 3, 4, 5, 6, 7, 8, 9};
		double [] sample2 = {1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8, 9.9};
		double [] sample3 = {0.1, 1.2, 2.3, 3.4, 5.5, 6.6, 7.7, 8.8, 9.9};
		double [] sample4 = {1, 2, 3, 4, 5, 6, 7, 8, 10};

		try {
			System.out.println(TestUtils.pairedT(sample1, sample2));
			System.out.println(TestUtils.pairedTTest(sample1, sample2));
			System.out.println(TestUtils.pairedTTest(sample1, sample3));
			System.out.println(TestUtils.pairedTTest(sample1, sample4));
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MathException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
