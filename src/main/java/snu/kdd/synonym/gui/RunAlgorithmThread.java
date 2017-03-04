package snu.kdd.synonym.gui;

import java.io.IOException;

import javax.swing.JTextArea;

import snu.kdd.synonym.driver.Driver;

public class RunAlgorithmThread extends Thread {

	String[] args;

	boolean verbose;

	JTextArea logArea;

	public RunAlgorithmThread( String arg, boolean verbose, JTextArea logArea ) {
		this.args = arg.split( " " );
		this.verbose = verbose;

		this.logArea = logArea;
	}

	@Override
	public void run() {
		try {
			long startTime = System.currentTimeMillis();
			Driver.main( args );
			long endTime = System.currentTimeMillis();
			logArea.append( "Finished: " + ( endTime - startTime ) + "\n" );
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

}
