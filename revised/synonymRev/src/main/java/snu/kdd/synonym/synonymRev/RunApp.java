package snu.kdd.synonym.synonymRev;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

public class RunApp {

	public static void main( String[] args ) throws IOException, ParseException {
		
		int n = 14;
		args = ("-dataOnePath D:\\ghsong\\data\\aol\\splitted\\aol_100000_data.txt " + 
				"-dataTwoPath D:\\ghsong\\data\\aol\\splitted\\aol_100000_data.txt " + 
				"-rulePath D:\\ghsong\\data\\wordnet\\rules.noun " + 
				"-outputPath output " + 
				"-algorithm JoinMHDP " + 
				"-oneSideJoin True " + 
				"-additional \"-1\"").split( " ", n );
		
		
		args[n-1] = "\"-K 1 -qSize 2 -mode dp1 -index FTK\"";
		App.main( args );
		args[n-1] = "\"-K 1 -qSize 2 -mode dp1 -index FF\"";
		App.main( args );
		args[n-1] = "\"-K 1 -qSize 3 -mode dp1 -index FTK\"";
		App.main( args );
		args[n-1] = "\"-K 1 -qSize 3 -mode dp1 -index FF\"";
		App.main( args );
		args[n-1] = "\"-K 2 -qSize 2 -mode dp1 -index FTK\"";
		App.main( args );
		args[n-1] = "\"-K 2 -qSize 2 -mode dp1 -index FF\"";
		App.main( args );
	}
}
