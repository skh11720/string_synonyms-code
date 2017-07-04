package snu.kdd.synonym.synonymRev.data;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TokenIndex {
	Object2IntOpenHashMap<String> token2IntMap;
	ObjectArrayList<String> int2TokenList;

	int nextNewId = 0;

	public TokenIndex() {
		token2IntMap = new Object2IntOpenHashMap<String>();
		token2IntMap.defaultReturnValue( -1 );

		// add an empty string with id 0
		getID( "" );
	}

	public int getID( String token ) {
		// Get id of token, if a new token is given add it to token2IntMap and int2TokenList
		int id = token2IntMap.getInt( token );

		if( id < -1 ) {
			id = nextNewId++;
			token2IntMap.put( token, id );
			int2TokenList.add( token );
		}

		return id;
	}
	
	public String getToken( int index ) {
		return int2TokenList.get(index);
	}
}
