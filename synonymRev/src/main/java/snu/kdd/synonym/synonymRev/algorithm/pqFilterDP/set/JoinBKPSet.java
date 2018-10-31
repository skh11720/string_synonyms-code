package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;

public class JoinBKPSet extends JoinFKPSet {

	public JoinBKPSet( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected void buildIndex( Dataset indexedSet ) {
		WYK_HashMap<Integer, List<Record>> idx = null;
		Object2IntOpenHashMap<Record> idxCount = null;
		Dataset searchedSet = null;

		if ( indexedSet == query.indexedSet ) {
			idx = idxT;
			idxCount = idxCountT;
			searchedSet = query.searchedSet;
		}
		else if ( indexedSet == query.searchedSet ) {
			idx = idxS;
			idxCount = idxCountS;
			searchedSet = query.indexedSet;
		}
		idxCount.defaultReturnValue( 0 );
		
		Int2IntOpenHashMap count = new Int2IntOpenHashMap();
		count.defaultReturnValue(0);
		for ( Record recS : searchedSet.recordList ) {
			IntOpenHashSet candidateTokens = new IntOpenHashSet();
			for ( int pos=0; pos<recS.size(); pos++ ) {
				for ( Rule rule : recS.getSuffixApplicableRules( pos )) {
					for (int token : rule.getRight() ) candidateTokens.add( token );
				}
			}
			for ( int token : candidateTokens ) count.addTo( token, 1 );
		}

		for ( Record recT : indexedSet.recordList ) {
			int[] tokens = recT.getTokensArray();
			int j_max = Math.min( indexK, tokens.length );
			IntOpenHashSet smallestK = new IntOpenHashSet(indexK);
			for ( int j=0; j<j_max; j++ ) {
				int smallest = tokens[0];
				int smallest_count = count.get( smallest );
				for ( int i=1; i<tokens.length; i++ ) {
					if ( smallestK.contains( tokens[i] )) continue;
					if ( count.get( tokens[i] ) < smallest_count ) {
						smallest = tokens[i];
						smallest_count = count.get( smallest );
					}
				}
				smallestK.add( smallest );
			}
			idxCount.put( recT, smallestK.size() );
			for ( int token : smallestK ) {
				if ( idx.get( token ) == null ) idx.put( token, new ObjectArrayList<Record>() );
				idx.get( token ).add( recT );
			}
//			System.out.println( indexK+"\t"+tokens.length+"\t"+j_max+"\t"+idxCount.getInt( recT ) );
		}
		
		if (DEBUG.bIndexWriteToFile) {
			try {
				String name = "";
				if ( idx == idxS ) name = "idxS";
				else if ( idx == idxT ) name = "idxT";
				BufferedWriter bw = new BufferedWriter( new FileWriter( "./tmp/PQFilterDPSetIndex_"+name+".txt" ) );
				for ( int key : idx.keySet() ) {
					bw.write( "token: "+query.tokenIndex.getToken( key )+" ("+key+")\n" );
					for ( Record rec : idx.get( key ) ) bw.write( ""+rec.getID()+", " );
					bw.write( "\n" );
				}
			}
			catch( IOException e ) {
				e.printStackTrace();
				System.exit( 1 );
			}
		}
	}

	@Override
	public String getName() {
		return "JoinBKPSet";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 */
		return "1.00";
	}
}
