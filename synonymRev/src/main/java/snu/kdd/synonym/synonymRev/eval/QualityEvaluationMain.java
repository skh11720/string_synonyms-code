package snu.kdd.synonym.synonymRev.eval;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.App;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmFactory;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmStatInterface;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.AlgorithmResultQualityEvaluator;
import snu.kdd.synonym.synonymRev.tools.Util;

public class QualityEvaluationMain {

	static final String[] argsTemplate = {"-algorithm", "", "-oneSideJoin", "True", "-additional", ""};

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

	private static PrintWriter pw;

	public static void main(String[] args) throws IOException, ParseException {
		pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/QualityEvaluationMain.txt", true)));
		
		String dataName = args[0];
		String alg = args[1];
		String resultStr = null;
		
		resultStr = String.format("%s\t%s\t:", dataName, alg);
		String[] exe_args = Arrays.copyOf(argsTemplate, argsTemplate.length);
		if (alg.equals("JoinDeltaVarBK")) {
			exe_args[1] = alg;
			exe_args[5] = String.format("\"-K 1 -qSize 2 -delta 0 -dist lcs -sampleB 0.01\"");
		}
		else if (alg.equals("PassJoin")) {
			exe_args[1] = "PassJoin";
			exe_args[5] = String.format("\"-delta 0 -dist edit\"");
		}
		else if (alg.equals("JoinPkduck")) {
			exe_args[1] = "JoinPkduckOriginal";
			exe_args[5] = String.format("\"-ord FF -theta 1.0 -rc false -lf true\"");
		}
		else if (alg.equals("SIJoin")) {
			exe_args[1] = "SIJoinOriginal";
			exe_args[5] = String.format("\"-theta 1.0\"");
		}
		else if (alg.equals("JoinBKPSet_DP")) {
			exe_args[1] = "JoinBKPSet";
			exe_args[5] = String.format("\"-K 2 -verify DP\"");
		}
		else if (alg.equals("JoinBKPSet_GR")) {
			exe_args[1] = "JoinBKPSet";
			exe_args[5] = String.format("\"-K 2 -verify GR1\"");
		}
		else if (alg.equals("JoinSetNaive")) {
			exe_args[1] = "JoinSetNaive";
			exe_args[5] = String.format("\"\"");
		}

		resultStr += runAndGetResultString(dataName, exe_args);
		pw.println(resultStr);
		pw.flush();
		pw.close();

	}

	private static String runAndGetResultString( String dataName, String[] args ) throws ParseException, IOException {
		CommandLine cmd = App.parseInput( args );
		Query query = Util.getTestQuery(dataName, 0);
		String groundPath = Util.getGroundTruthPath(dataName);
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
			strbld.append("\t-");
		}
		return strbld.toString();
	}
}
