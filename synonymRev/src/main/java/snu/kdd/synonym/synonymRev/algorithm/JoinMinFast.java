package snu.kdd.synonym.synonymRev.algorithm;

import snu.kdd.synonym.synonymRev.index.JoinMinFastIndex;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;

public class JoinMinFast extends JoinMin {
	
	public final double sampleB;

	public JoinMinFast(String[] args) {
		super(args);
		checker = new TopDownOneSide();
		sampleB = param.getDoubleParam("sampleB");
	}

	@Override
	protected void reportParamsToStat() {
		super.reportParamsToStat();
		stat.add("Param_sampleB", sampleB);
	}

	@Override
	protected void buildIndex() {
		idx = new JoinMinFastIndex( indexK, qSize, stat, query, sampleB, 0, writeResultOn );
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
