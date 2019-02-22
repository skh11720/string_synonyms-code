package snu.kdd.synonym.synonymRev.data;

import java.io.File;

public class DataInfo {

	boolean isSynthetic = false;

	final String dataOnePath;
	final String dataTwoPath;
	final String rulePath;
	final String name;

	public DataInfo( String dataOnePath, String dataTwoPath, String rulePath ) {
		this.dataOnePath = dataOnePath;
		this.dataTwoPath = dataTwoPath;
		this.rulePath = rulePath;
		name = setName();
	}
	
	private String setName() {
		final String dataOneFileName = dataOnePath.substring( dataOnePath.lastIndexOf(File.separator) + 1 );
		final String dataTwoFileName = dataTwoPath.substring( dataTwoPath.lastIndexOf(File.separator) + 1 );
		final String ruleFileName = rulePath.substring( rulePath.lastIndexOf(File.separator) + 1 );

		if( isSelfJoin() ) {
			return dataOneFileName + "_SelfJoin" + "_wrt_" + ruleFileName;
		}
		else {
			return dataOneFileName + "_JoinWith_" + dataTwoFileName + "_wrt_" + ruleFileName;
		}
	}
	
	private boolean isSelfJoin() {
		return dataOnePath.equals( dataTwoPath );
	}

	public String toJson() {
		StringBuilder bld = new StringBuilder();
		bld.append( "\"Name\": \"" + name + "\"" );
		bld.append( ", \"Data One Path\": \"" + dataOnePath + "\"" );
		bld.append( ", \"Data Two Path\": \"" + dataTwoPath + "\"" );
		bld.append( ", \"Rule Path\": \"" + rulePath + "\"" );
		return bld.toString();
	}
	
	public String getName() {
		return name;
	}
}
