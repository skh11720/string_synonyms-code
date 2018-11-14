package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMinFastIndex;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;

public class JoinMinFast extends JoinMin {
	
	protected double sampleRatio;

	public JoinMinFast(Query query, StatContainer stat, String[] args) throws IOException, ParseException {
		super(query, stat, args);
		param = new Param(args);
		checker = new TopDownOneSide();
		qSize = param.getIntParam("qSize");
		indexK = param.getIntParam("indexK");
		sampleRatio = param.getDoubleParam("sampleB");
		useLF = param.getBooleanParam("useLF");
		usePQF = param.getBooleanParam("usePQF");
		useSTPQ = param.getBooleanParam("useSTPQ");
	}

	@Override
	protected void buildIndex( boolean writeResult ) {
		idx = new JoinMinFastIndex( indexK, qSize, stat, query, sampleRatio, 0, writeResult );
		JoinMinFastIndex.useLF = useLF;
		JoinMinFastIndex.usePQF = usePQF;
		JoinMinFastIndex.useSTPQ = useSTPQ;
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: test for filtering power test
		 */
		return "1.01";
	}

	@Override
	public String getName() {
		return "JoinMinFast";
	}
}
