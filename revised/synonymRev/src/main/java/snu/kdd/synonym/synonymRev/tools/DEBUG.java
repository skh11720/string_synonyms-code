package snu.kdd.synonym.synonymRev.tools;

public class DEBUG {
	public static final boolean AlgorithmON = false;

	public static final boolean JoinMHON = false;
	public static final boolean JoinMinON = false;
	public static final boolean JoinBKON = false;
	public static final boolean NaiveON = true;

	public static final boolean JoinMHIndexON = false;
	public static final boolean JoinMinIndexON = false;
	
	public static final boolean JoinMinNaiveON = false;
	public static final boolean JoinMHNaiveON = false;

	// Print index contents and detailed information
	public static final boolean PrintNaiveIndexON = false;
	public static final boolean PrintJoinMinIndexON = false;
	public static final boolean PrintJoinMinJoinON = false;
	public static final boolean PrintQueryON = true;

	public static final boolean ValidateON = false;
	public static final boolean SampleStatON = true;

	public static final boolean printSelfJoinON = false;

	public static final boolean ToLowerON = true;

	public static final boolean EstTooManyWarningON = true;
	public static final boolean JoinNaiveSkipTooMany = true;
	public static final int EstTooManyThreshold = 20_000_000;
}
