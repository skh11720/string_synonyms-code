package snu.kdd.synonym.synonymRev.data;

import java.io.File;

public class DataInfo {

	boolean isSynthetic = false;

	public final String dataOnePath;
	public final String dataTwoPath;
	public final String rulePath;
	public final String dataOneFileName;
	public final String dataTwoFileName;
	public final String ruleFileName;
	public final String datasetName;

	public DataInfo( String dataOnePath, String dataTwoPath, String rulePath ) {
		this.dataOnePath = dataOnePath;
		this.dataTwoPath = dataTwoPath;
		this.rulePath = rulePath;
		this.dataOneFileName = dataOnePath.substring( dataOnePath.lastIndexOf(File.separator) + 1 );
		this.dataTwoFileName = dataTwoPath.substring( dataTwoPath.lastIndexOf(File.separator) + 1 );
		this.ruleFileName = rulePath.substring( rulePath.lastIndexOf(File.separator) + 1 );
		this.datasetName = setName();
	}
	
	private String setName() {

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
		bld.append( "\"Name\": \"" + datasetName + "\"" );
		bld.append( ", \"Data One Path\": \"" + dataOnePath + "\"" );
		bld.append( ", \"Data Two Path\": \"" + dataTwoPath + "\"" );
		bld.append( ", \"Rule Path\": \"" + rulePath + "\"" );
		return bld.toString();
	}
	
	public String getName() {
		return datasetName;
	}
}
