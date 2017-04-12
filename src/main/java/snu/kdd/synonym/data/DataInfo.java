package snu.kdd.synonym.data;

import snu.kdd.synonym.tools.JSONUtil;

public class DataInfo {
	int avgRecLen;
	int nRecord;
	int nToken;

	boolean isSynthetic = false;
	double equivRatio;
	long seed;
	double zipf;
	long size = 0;

	String dataOnePath;
	String dataTwoPath;
	String rulePath;
	String name = "None";

	public DataInfo() {

	}

	public DataInfo( String dataOnePath, String dataTwoPath, String rulePath ) {
		this.dataOnePath = dataOnePath;
		this.dataTwoPath = dataTwoPath;
		this.rulePath = rulePath;

	}

	public String toJson() {
		StringBuilder bld = new StringBuilder();

		bld.append( "\"name\": \"" + name + "\"" );
		bld.append( ", \"is_synthetic\": \"" + isSynthetic + "\"" );
		bld.append( ", \"file_size\": \"" + size + "\"" );

		bld.append( ", \"synthetic_parameters\": {" );
		bld.append( "\"Data One Path\": \"" + dataOnePath + "\"" );
		bld.append( ", \"Data Two Path\": \"" + dataTwoPath + "\"" );
		bld.append( ", \"Rule Path\": \"" + rulePath + "\"" );
		bld.append( "}" );

		return bld.toString();
	}

	public DataInfo( String fileName ) {
		loadFromFile( fileName );
	}

	public void setSynthetic( int avgRecLen, int nRecord, long seed, int nToken, double zipf, double equivRatio ) {
		this.avgRecLen = avgRecLen;
		this.nRecord = nRecord;
		this.nToken = nToken;

		this.isSynthetic = true;
		this.seed = seed;
		this.zipf = zipf;
		this.equivRatio = equivRatio;
	}

	public void saveToFile( String fileName ) {
		JSONUtil json = new JSONUtil();
		json.add( "avgRecLen", this.avgRecLen );
		json.add( "nRecord", this.nRecord );
		json.add( "nToken", this.nToken );
		json.add( "isSynthetic", this.isSynthetic );

		if( isSynthetic ) {
			json.add( "seed", this.seed );
			json.add( "zipf", this.zipf );
			json.add( "equivRatio", this.equivRatio );
		}

		json.write( fileName );
	}

	public void loadFromFile( String fileName ) {
		JSONUtil json = new JSONUtil();
		json.read( fileName );

		this.avgRecLen = Integer.parseInt( (String) json.getValue( "avgRecLen" ) );
		this.nRecord = Integer.parseInt( (String) json.getValue( "nRecord" ) );
		this.nToken = Integer.parseInt( (String) json.getValue( "nToken" ) );
		this.isSynthetic = Boolean.parseBoolean( (String) json.getValue( "isSynthetic" ) );

		if( isSynthetic ) {
			this.seed = Long.parseLong( (String) json.getValue( "seed" ) );
			this.zipf = Double.parseDouble( (String) json.getValue( "zipf" ) );
			this.equivRatio = Double.parseDouble( (String) json.getValue( "equivRatio" ) );
		}
	}
}
