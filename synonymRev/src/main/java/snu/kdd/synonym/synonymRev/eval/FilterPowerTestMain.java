package snu.kdd.synonym.synonymRev.eval;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import snu.kdd.synonym.synonymRev.App;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmFactory;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmStatInterface;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.AlgorithmResultQualityEvaluator;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;

public class FilterPowerTestMain {

	static final String[] argsTemplate = {"-algorithm", "", "-oneSideJoin", "True", "-additional", ""};

	private static PrintWriter pw;

	public static void main(String[] args) throws IOException, ParseException {
		PrintWriter pw = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/FilterPowerTestMain.txt", true ) ) );
		
		String dataName = args[0];
		int size = Integer.parseInt(args[1]);
		String algName = args[2];
		int indexK = Integer.parseInt(args[3]);
		int qSize = Integer.parseInt(args[4]);
		int deltaMax = Integer.parseInt(args[5]);
		String dist = args[6];
		boolean useLF = Boolean.parseBoolean(args[7]);
		boolean usePQF = Boolean.parseBoolean(args[8]);
		boolean useSTPQ = Boolean.parseBoolean(args[9]);

		StringBuilder strbld = new StringBuilder();
		for ( String str : args ) strbld.append("\t"+str);

		Query query = Util.getTestQuery(dataName, size);
		String[] exe_args = Arrays.copyOf(argsTemplate, argsTemplate.length);
		exe_args[1] = algName;
		if ( algName.equals("JoinDeltaVar") ) {
			exe_args[5] = String.format("\"-K %d -qSize %d -delta %d -dist %s -useLF %s -usePQF %s -useSTPQ %s\"", indexK, qSize, deltaMax, dist, useLF, usePQF, useSTPQ);
		}
		else if ( algName.equals("JoinDeltaVarBK") ) {
			exe_args[5] = String.format("\"-K %d -qSize %d -delta %d -dist %s -sampleB 0.01 -useLF %s -usePQF %s -useSTPQ %s\"", indexK, qSize, deltaMax, dist, useLF, usePQF, useSTPQ);
		}

		CommandLine cmd = App.parseInput( exe_args );
		AlgorithmInterface alg = AlgorithmFactory.getAlgorithmInstance(cmd, query.selfJoin );
		try {
			alg.run(query);
			alg.getStat().printResult();
			StatContainer stat = alg.getStat();
			for ( String key : new String[] {AlgorithmStatInterface.TOTAL_RUNNING_TIME, AlgorithmStatInterface.FINAL_RESULT_SIZE, Stat.CAND_PQGRAM_COUNT, Stat.NUM_VERIFY} ) {
				strbld.append("\t"+stat.getString(key));
			}
		}
		catch ( OutOfMemoryError e ) {
			strbld.append("\tOOM");
		}

		pw.println(strbld.toString().trim());
		pw.flush(); 
		pw.close();
	}
}
