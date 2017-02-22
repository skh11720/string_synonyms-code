package snu.kdd.synonym.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class JSONUtil {
	ObjectArrayList<String> nameList;
	ObjectArrayList<Object> valueList;

	public JSONUtil() {
		nameList = new ObjectArrayList<String>();
		valueList = new ObjectArrayList<Object>();
	}

	public void add( String name, Object value ) {
		this.nameList.add( name );
		this.valueList.add( value );
	}

	public void write( String fileName ) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter( new FileWriter( fileName ) );
		}
		catch( IOException e ) {
			e.printStackTrace();
			return;
		}

		try {
			bw.write( "{\n" );

			for( int i = 0; i < nameList.size(); i++ ) {
				if( i != 0 ) {
					bw.write( ",\n" );
				}
				bw.write( "\"" );
				bw.write( nameList.get( i ) );
				bw.write( "\": \"" );
				bw.write( valueList.get( i ).toString() );
				bw.write( "\"" );
			}
			bw.write( "\n}" );
		}
		catch(

		IOException e ) {
			e.printStackTrace();
		}

		try {
			bw.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	public void read( String fileName ) {
		BufferedReader br = null;

		try {
			br = new BufferedReader( new FileReader( fileName ) );
		}
		catch( FileNotFoundException e ) {
			e.printStackTrace();
			return;
		}

		String line = null;

		try {
			while( ( line = br.readLine() ) != null ) {
				if( ( line.equals( "{" ) || line.equals( "}" ) ) ) {
					continue;
				}
				else {
					String[] temp = line.replaceAll( ",", "" ).split( ":" );
					String key = temp[ 0 ].replaceAll( "\"", "" ).trim();
					String value = temp[ 1 ].replaceAll( "\"", "" ).trim();

					add( key, value );
				}
			}
		}
		catch( IOException e ) {
			e.printStackTrace();
		}

		try {
			br.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	public Object getValue( String key ) {
		for( int i = 0; i < nameList.size(); i++ ) {
			if( nameList.get( i ).equals( key ) ) {
				return valueList.get( i );
			}
		}
		return null;
	}
}
