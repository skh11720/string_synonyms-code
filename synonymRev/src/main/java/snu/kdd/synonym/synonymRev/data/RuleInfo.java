package snu.kdd.synonym.synonymRev.data;

import snu.kdd.synonym.synonymRev.tools.JSONUtil;

public class RuleInfo {
	int lhsMax;
	int rhsMax;
	int nRule;
	int nToken;

	boolean isSynthetic;
	long seed;
	double zipf;

	public RuleInfo() {
	}

	public RuleInfo( String fileName ) {
		loadFromFile( fileName );
	}

	public void setSynthetic( int lhsMax, int rhsMax, int nRule, long seed, int nToken, double zipf ) {
		this.lhsMax = lhsMax;
		this.rhsMax = rhsMax;
		this.nRule = nRule;
		this.nToken = nToken;

		this.isSynthetic = true;
		this.seed = seed;
		this.zipf = zipf;
	}

	public void saveToFile( String fileName ) {
		JSONUtil json = new JSONUtil();
		json.add( "lhsMax", this.lhsMax );
		json.add( "rhsMax", this.rhsMax );
		json.add( "nRule", this.nRule );
		json.add( "nToken", this.nToken );
		json.add( "isSynthetic", this.isSynthetic );

		if( isSynthetic ) {
			json.add( "seed", this.seed );
			json.add( "zipf", this.zipf );
		}

		json.write( fileName );
	}

	public void loadFromFile( String fileName ) {
		JSONUtil json = new JSONUtil();
		json.read( fileName );

		this.lhsMax = Integer.parseInt( (String) json.getValue( "lhsMax" ) );
		this.rhsMax = Integer.parseInt( (String) json.getValue( "rhsMax" ) );
		this.nRule = Integer.parseInt( (String) json.getValue( "nRule" ) );
		this.nToken = Integer.parseInt( (String) json.getValue( "nToken" ) );
		this.isSynthetic = Boolean.parseBoolean( (String) json.getValue( "isSynthetic" ) );

		if( isSynthetic ) {
			this.seed = Long.parseLong( (String) json.getValue( "seed" ) );
			this.zipf = Double.parseDouble( (String) json.getValue( "zipf" ) );
		}
	}
}
