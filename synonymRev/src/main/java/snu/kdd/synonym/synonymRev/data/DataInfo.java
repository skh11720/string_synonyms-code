package snu.kdd.synonym.synonymRev.data;

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
		name = getName();
	}
	
	private String getName() {
		String dataOneFileName = dataOnePath.substring( dataOnePath.lastIndexOf( "/" ) + 1 );
		String dataTwoFileName = dataTwoPath.substring( dataTwoPath.lastIndexOf( "/" ) + 1 );
		String ruleFileName = rulePath.substring( rulePath.lastIndexOf( "/" ) + 1 );

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
}
