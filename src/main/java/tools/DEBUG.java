package tools;

public class DEBUG {
	public static final boolean AlgorithmON = false;

	public static final boolean NaiveON = true;
	public static final boolean SIJoinON = false;

	public static final boolean JoinMinON = true;
	public static final boolean JoinMinIndexON = false;
	public static final boolean JoinMinJoinON = false;
	public static final boolean JoinMinIndexCountON = false;

	public static final boolean JoinHybridON = true;
	public static final boolean JoinHybridThresON = false;

	public static final boolean JoinMHOn = true;
	public static final boolean JoinMHDetailOn = true;

	public static final boolean SampleStatOn = false;
	public static final boolean ValidateON = false;

	public static final boolean PrintJoinMinIndexON = false;
	public static final boolean PrintNaiveIndexON = false;

	public static void main( String args[] ) {
		System.out.println( "\"Hi".replaceAll( "\"", "\\\\\"" ) );
	}
}
