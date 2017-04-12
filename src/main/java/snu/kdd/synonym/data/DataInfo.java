package snu.kdd.synonym.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import snu.kdd.synonym.tools.JSONUtil;

public class DataInfo {
	// int avgRecLen;
	int nOneRecord;
	int nTwoRecord;
	int nToken;
	int nRule;

	boolean isSynthetic = false;
	double equivRatio;
	long seed;
	double zipf;
	long size = 0;

	String dataOnePath;
	String dataTwoPath;
	String rulePath;
	String name = "None";
	String info;

	File oneInfoFile;
	File twoInfoFile;
	File ruleInfoFile;

	boolean infoFileOneExists = false;
	boolean infoFileTwoExists = false;
	boolean infoRuleExists = false;
	boolean selfJoin = false;
	boolean updated = false;

	public DataInfo() {

	}

	public void updateRuleCount( int count ) {
		if( this.nRule != count ) {
			this.nRule = count;
			updated = true;
		}
	}

	public void updateOneCount( int count ) {
		if( this.nOneRecord != count ) {
			this.nOneRecord = count;
			updated = true;
		}
	}

	public void updateTwoCount( int count ) {
		if( this.nTwoRecord != count ) {
			this.nTwoRecord = count;
			updated = true;
		}
	}

	public DataInfo( String dataOnePath, String dataTwoPath, String rulePath ) {
		this.dataOnePath = dataOnePath;
		this.dataTwoPath = dataTwoPath;
		this.rulePath = rulePath;

		String dataOne = dataOnePath.substring( dataOnePath.lastIndexOf( "/" ) );
		String oneInfoFilePath = dataOnePath.substring( 0, dataOnePath.lastIndexOf( "/" ) ) + dataOne + "_info.json";
		name = dataOne;
		System.out.println( "One info file path " + oneInfoFilePath );
		oneInfoFile = new File( oneInfoFilePath );
		infoFileOneExists = oneInfoFile.exists();
		if( infoFileOneExists ) {
			loadFromFile( oneInfoFile, 1 );
		}

		if( !dataOnePath.equals( dataTwoPath ) ) {
			String dataTwo = dataTwoPath.substring( dataTwoPath.lastIndexOf( "/" ) );
			String twoInfoFilePath = dataTwoPath.substring( 0, dataTwoPath.lastIndexOf( "/" ) ) + dataTwo + "_info.json";
			twoInfoFile = new File( twoInfoFilePath );
			infoFileTwoExists = twoInfoFile.exists();
			if( infoFileTwoExists ) {
				loadFromFile( twoInfoFile, 2 );
			}
			name += "_JoinWith_" + dataTwo;
		}
		else {
			name += "_SelfJoin";
			selfJoin = true;
		}

		String rule = rulePath.substring( rulePath.lastIndexOf( "/" ) );
		ruleInfoFile = new File( rulePath.substring( 0, rulePath.lastIndexOf( "/" ) ) + "_ruleinfo.json" );
		infoRuleExists = ruleInfoFile.exists();
		if( infoRuleExists ) {
			loadFromFile( ruleInfoFile, 0 );
		}
		name += "_WRT_" + rule;
	}

	public void writeInfo() {
		if( updated ) {
			saveToFile( oneInfoFile, true );
			if( !selfJoin ) {
				saveToFile( twoInfoFile, false );
			}
			saveRuleToFile( ruleInfoFile );
		}
	}

	public String toJson() {
		StringBuilder bld = new StringBuilder();

		bld.append( "\"name\": \"" + name + "\"" );
		bld.append( ", \"is_synthetic\": \"" + isSynthetic + "\"" );
		bld.append( ", \"file_size\": \"" + size + "\"" );

		bld.append( ", \"data_info\": {" );
		bld.append( "\"Data One Path\": \"" + dataOnePath + "\"" );
		bld.append( ", \"Data Two Path\": \"" + dataTwoPath + "\"" );
		bld.append( ", \"Rule Path\": \"" + rulePath + "\"" );
		bld.append( ", \"Info\": \"" + info + "\"" );
		bld.append( "}" );

		return bld.toString();
	}

	public DataInfo( String fileName ) {

	}

	public void setSynthetic( int avgRecLen, int nOneRecord, long seed, int nToken, double zipf, double equivRatio ) {
		// this.avgRecLen = avgRecLen;
		this.nOneRecord = nOneRecord;
		this.nToken = nToken;

		this.isSynthetic = true;
		this.seed = seed;
		this.zipf = zipf;
		this.equivRatio = equivRatio;
	}

	public void saveToFile( File file, boolean isFirst ) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter( new FileWriter( file ) );

			bw.write( "\"nRecord\": " );
			if( isFirst ) {
				bw.write( "" + nOneRecord );
			}
			else {
				bw.write( "" + nTwoRecord );
			}
			bw.write( "\"" );

			bw.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	public void saveRuleToFile( File file ) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter( new FileWriter( file ) );

			bw.write( "\"nRule\": " );
			bw.write( "\"" + nRule );
			bw.write( "\"" );

			bw.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	public void saveToFile( String fileName ) {
		JSONUtil json = new JSONUtil();
		// json.add( "avgRecLen", this.avgRecLen );
		json.add( "nOneRecord", this.nOneRecord );
		json.add( "nToken", this.nToken );
		json.add( "isSynthetic", this.isSynthetic );

		if( isSynthetic ) {
			json.add( "seed", this.seed );
			json.add( "zipf", this.zipf );
			json.add( "equivRatio", this.equivRatio );
		}

		json.write( fileName );
	}

	public void loadFromFile( File file, int type ) {
		BufferedReader br;
		String line = "";
		try {
			br = new BufferedReader( new FileReader( file ) );
			line = br.readLine();

			int count = Integer.parseInt( line.replaceAll( "\"", "" ).split( " " )[ 1 ] );

			if( type == 0 ) {
				nRule = count;
			}
			else if( type == 1 ) {
				nOneRecord = count;
			}
			else {
				nTwoRecord = count;
			}

			br.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}

		// this.avgRecLen = Integer.parseInt( (String) json.getValue( "avgRecLen" ) );
		// this.nRecord = Integer.parseInt( (String) json.getValue( "nRecord" ) );
		// this.nToken = Integer.parseInt( (String) json.getValue( "nToken" ) );
		// this.isSynthetic = Boolean.parseBoolean( (String) json.getValue( "isSynthetic" ) );
		//
		// if( isSynthetic ) {
		// this.seed = Long.parseLong( (String) json.getValue( "seed" ) );
		// this.zipf = Double.parseDouble( (String) json.getValue( "zipf" ) );
		// this.equivRatio = Double.parseDouble( (String) json.getValue( "equivRatio" ) );
		// }
	}
}
