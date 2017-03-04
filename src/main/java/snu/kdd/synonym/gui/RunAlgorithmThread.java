package snu.kdd.synonym.gui;

import java.io.IOException;

import javax.swing.JTextArea;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.driver.Driver;

public class RunAlgorithmThread extends Thread {

	String[] args;

	boolean verbose;

	JTextArea logArea;

	public RunAlgorithmThread( String arg, String additional, boolean verbose, JTextArea logArea ) {
		String[] temp = arg.split( " " );
		this.args = new String[ temp.length + 1 ];

		for( int i = 0; i < temp.length; i++ ) {
			this.args[ i ] = temp[ i ];
		}
		this.args[ temp.length ] = additional;

		this.verbose = verbose;

		this.logArea = logArea;
	}

	@Override
	public void run() {
		try {
			long startTime = System.currentTimeMillis();
			try {
				Driver.main( args );
			}
			catch( ParseException e ) {
				e.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			logArea.append( "Finished: " + ( endTime - startTime ) + "\n" );
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

}
