package snu.kdd.synonym.data;

import snu.kdd.synonym.tools.JSONUtil;

public class RuleInfo {
	int lhsMax;
	int rhsMax;
	int nRule;

	boolean isSynthetic;
	long seed;

	public RuleInfo() {

	}

	public RuleInfo( String fileName ) {
		loadFromFile( fileName );
	}

	public void setSynthetic( int lhsMax, int rhsMax, int nRule, long seed ) {
		this.lhsMax = lhsMax;
		this.rhsMax = rhsMax;
		this.nRule = nRule;

		this.isSynthetic = true;
		this.seed = seed;
	}

	public void saveToFile( String fileName ) {
		JSONUtil json = new JSONUtil();
		json.add( "lhsMax", this.lhsMax );
		json.add( "rhsMax", this.rhsMax );
		json.add( "nRule", this.nRule );
		json.add( "isSynthetic", this.isSynthetic );

		if( isSynthetic ) {
			json.add( "seed", this.seed );
		}

		json.write( fileName );
	}

	public void loadFromFile( String fileName ) {
		JSONUtil json = new JSONUtil();
		json.read( fileName );

		this.lhsMax = Integer.parseInt( (String) json.getValue( "lhsMax" ) );
		this.rhsMax = Integer.parseInt( (String) json.getValue( "rhsMax" ) );
		this.nRule = Integer.parseInt( (String) json.getValue( "nRule" ) );
		this.isSynthetic = Boolean.parseBoolean( (String) json.getValue( "isSynthetic" ) );

		if( isSynthetic ) {
			this.seed = Long.parseLong( (String) json.getValue( (String) json.getValue( "seed" ) ) );
		}
	}
}
