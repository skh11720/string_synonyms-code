package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmFactory;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmStatInterface;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.AlgorithmResultQualityEvaluator;

public class QualityComparisonVaryingThresholdTest {

	static final String[] dataNameArray = new String[] {"NAMES", "UNIV_1_2", "CONF"};
	static final String[] distArray = new String[] {"lcs"};
	static final int[] deltaArray = new int[] {0, 1, 2};
	static final double[] thresArray = new double[] {1.0, 0.8, 0.6, 0.4};
	static final String[] argsTemplate = {"-algorithm", "", "-oneSideJoin", "True", "-additional", ""};
//	static final String groundPath = "D:\\ghsong\\data\\synonyms\\Names\\ver_4\\Names_groundtruth.txt";
	static final String[] measureLongArray = {
			AlgorithmStatInterface.EVAL_TP,
			AlgorithmStatInterface.EVAL_FP,
			AlgorithmStatInterface.EVAL_FN,
	};
	static final String[] measureDoubleArray = {
			AlgorithmStatInterface.EVAL_PRECISION,
			AlgorithmStatInterface.EVAL_RECALL,
			AlgorithmStatInterface.EVAL_F1SCORE,
	};
	
	private PrintWriter pw;

	@Test
	public void test() throws IOException, ParseException {
		initPrintWriter();
		runAlgorithms();
		pw.close();
	}
	
	private void initPrintWriter() {
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/QualityComparisonVaryingThresholdTest.txt")));
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
	}
	
	private void runAlgorithms() throws IOException, ParseException {
		for ( String dataName : dataNameArray ) {
			runOurs(dataName);
			runPrevs(dataName, "JoinPkduckOriginal");
			runPrevs(dataName, "SIJoinOriginal");
		}
	}
	
	private void runOurs( String dataName ) throws IOException, ParseException {
		String[] args = Arrays.copyOf(argsTemplate, argsTemplate.length);
		args[1] = "JoinDeltaVarBK";
		for ( String dist : distArray ) {
			for ( int delta : deltaArray ) {
				args[5] = String.format("\"-K 1 -qSize 2 -delta %d -dist %s -sampleB 0.01\"", delta, dist);
				String resultStr = String.format("%s\t%s\t%d\t:", dataName, args[1], delta);
				resultStr += runAndGetResultString(dataName, args);
				pw.println(resultStr);
				pw.flush();
			}
		}
	}
	
	private void runPrevs(String dataName, String algName) throws ParseException, IOException {
		String[] args = Arrays.copyOf(argsTemplate, argsTemplate.length);
		args[1] = algName;
		for ( double thres : thresArray ) {
			if (algName.equals("JoinPkduckOriginal"))
				args[5] = String.format("\"-ord FF -theta %.2f -rc false -lf true\"", thres);
			else if (algName.equals("SIJoinOriginal"))
				args[5] = String.format("\"-theta %.2f\"", thres);
			String resultStr = String.format("%s\t%s\t%.2f\t:", dataName, args[1], thres);
			resultStr += runAndGetResultString(dataName, args);
			pw.println(resultStr);
			pw.flush();
		}
	}
	
	private String runAndGetResultString( String dataName, String[] args ) throws ParseException, IOException {
		CommandLine cmd = App.parseInput( args );
		Query query = TestUtils.getTestQuery(dataName, 0);
		String groundPath = TestUtils.getGroundTruthPath(dataName);
		AlgorithmInterface alg = AlgorithmFactory.getAlgorithmInstance(cmd, query.selfJoin);
		StringBuilder strbld = new StringBuilder();
		try {
			alg.run(query);
			AlgorithmResultQualityEvaluator.evaluate(alg, query, groundPath);
			alg.getStat().printResult();

			for (String measure : measureLongArray ) {
				strbld.append("\t"+alg.getStat().getLong(measure));
			}
			for (String measure : measureDoubleArray ) {
				strbld.append("\t"+alg.getStat().getDouble(measure));
			}
		}
		catch ( OutOfMemoryError e ) {
			strbld.append("\tOOM");
		}
		return strbld.toString();
	}
}
