package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMHNaive extends AlgorithmTemplate {

	public JoinMHNaive( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		return "joinMHNaive";
	}

	@Override
	public String getVersion() {
		return "2.0";
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		// TODO Auto-generated method stub

	}

}
