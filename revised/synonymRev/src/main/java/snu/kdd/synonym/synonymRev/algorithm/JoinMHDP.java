package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHDPIndex;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

@Deprecated
public class JoinMHDP extends JoinMH {

	public JoinMHDP( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void buildIndex( boolean writeResult ) {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		idx = new JoinMHDPIndex( indexK, qgramSize, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		stat.add( "checkTPQ", ((JoinMHDPIndex)idx).checkTPQ );
		stat.add( "nCandQGrams", ((JoinMHDPIndex)idx).countValue );
		stat.add( "checkTPQTime", ((JoinMHDPIndex)idx).checkTPQTime );
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "JoinMHDP";
	}
	
	@Override
	public String getVersion() {
		return "1.00";
	}
}
