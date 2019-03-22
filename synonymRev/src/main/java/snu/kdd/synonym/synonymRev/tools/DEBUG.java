package snu.kdd.synonym.synonymRev.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DEBUG {
	public static final boolean AlgorithmON = false;

	public static final boolean JoinMHON = false;
	public static final boolean JoinMinON = false;
	public static final boolean JoinBKON = false;
	public static final boolean NaiveON = false;

	public static final boolean JoinMHIndexON = false;
	public static final boolean JoinMinIndexON = false;

	public static final boolean JoinMinNaiveON = false;
	public static final boolean JoinMHNaiveON = false;
	
	public static final boolean SIJoinON = false;
	public static final boolean JoinPkduckON = false; 

	// Print index contents and detailed information
	public static final boolean printSelfJoinON = false;
	public static final boolean PrintNaiveIndexON = false;
	public static final boolean PrintJoinMinIndexON = false;
	public static final boolean PrintJoinMinJoinON = false;
	public static final boolean PrintEstimationON = false;

	public static final boolean ValidateON = false;
	public static final boolean SampleStatON = false;

	// mostly on
	public static final boolean PrintQueryON = true;
	public static final boolean ToLowerON = true;

	public static final boolean EstTooManyWarningON = false;
	public static final boolean JoinNaiveSkipTooMany = true;
	public static final int EstTooManyThreshold = 20_000_000;
	
	// AbstractGlobalOrder
	public static final boolean bGlobalOrderWriteToFile = false;
	
	// index
	public static final boolean bIndexWriteToFile = false;
	
	public static final boolean bAlgorithmResultQualityEvaluator = false;
	
	private static PrintWriter log = null;
	static {
		try {
			log = new PrintWriter( new FileWriter("tmp/DEBUG.log"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void log(String str, String tag) {
		if ( tag == null ) log.println(str);
		else log.println(String.format("[%s] %s", tag, str));
	}
}
