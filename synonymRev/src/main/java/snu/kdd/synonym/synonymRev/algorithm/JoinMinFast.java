package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMinFastIndex;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMinFast extends JoinMin {
	
	protected double sampleRatio;

	public JoinMinFast( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}
	
	@Override
	protected void setup( Param params ) {
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;
		sampleRatio = params.sampleRatio;
		if ( sampleRatio <= 0 ) throw new RuntimeException("sampleRatio should be larger than 0.");
		useLF = params.useLF;
		usePQF = params.usePQF;
		useSTPQ = params.useSTPQ;
	}

	@Override
	protected void buildIndex( boolean writeResult ) throws IOException {
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
