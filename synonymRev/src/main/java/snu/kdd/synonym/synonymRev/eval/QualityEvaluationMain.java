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
		int deltaMax = -1;
		double thres = -1;
		String dist = null;
		try {
			deltaMax = Integer.parseInt(args[2]);
			dist = args[3];
		}
		catch (NumberFormatException e) {
			thres = Double.parseDouble(args[2]);
		}
		String resultStr = null;
		
		String[] exe_args = Arrays.copyOf(argsTemplate, argsTemplate.length);
		if (alg.equals("JoinDeltaVarBK")) {
			exe_args[1] = alg;
			exe_args[5] = String.format("\"-K 1 -qSize 2 -delta %d -dist %s -sampleB 0.01\"", deltaMax, dist);
			resultStr = String.format("%s\t%s\t%d\t:", dataName, alg+"_"+dist, deltaMax);
		}
		else if (alg.equals("JoinPkduck")) {
			exe_args[1] = "JoinPkduckOriginal";
			exe_args[5] = String.format("\"-ord FF -theta %.2f -rc false -lf true\"", thres);
			resultStr = String.format("%s\t%s\t%.2f\t:", dataName, alg, thres);
		}
		else if (alg.equals("SIJoin")) {
			exe_args[1] = "SIJoinOriginal";
			exe_args[5] = String.format("\"-theta %.2f\"", thres);
			resultStr = String.format("%s\t%s\t%.2f\t:", dataName, alg, thres);
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
			strbld.append("\tOOM");
		}
		return strbld.toString();
	}
}
