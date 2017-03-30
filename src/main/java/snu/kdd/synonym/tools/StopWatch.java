package snu.kdd.synonym.tools;

/**
 * A class providing various time measuring capabilities.
 */

public class StopWatch {
	public static StopWatch getWatchStarted( String name ) {
		final StopWatch watch = new StopWatch( name );
		watch.start();
		return watch;
	}

	private long timeEnd;
	private long timeStart;
	private long totalTime = 0;

	private String watchName = null;

	private StopWatch( String name ) {
		watchName = name;
	}

	public String getName() {
		return watchName;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public void printElapsedTime() {
		Util.printLog( watchName + " running for " + ( System.currentTimeMillis() - timeStart ) + "ms" );
	}

	public void printTotal() {
		Util.printLog( watchName + " ran for " + totalTime + "ms" );
	}

	public void reset() {
		timeStart = 0;
		timeEnd = 0;
		totalTime = 0;
	}

	public void reset( String newName ) {
		watchName = newName;
		timeStart = 0;
		timeEnd = 0;
		totalTime = 0;
	}

	public void resetAndStart( String newName ) {
		reset( newName );
		start();
	}

	public void start() {
		timeStart = System.currentTimeMillis();
	}

	public void stop() {
		if( timeStart != 0 ) {
			timeEnd = System.currentTimeMillis();
			totalTime += timeEnd - timeStart;

			Util.printLog( watchName + " ran for " + ( timeEnd - timeStart ) + "ms" + " (total: " + totalTime + "ms)" );
			timeStart = 0;
		}
	}

	public void stopAndAdd( StatContainer st ) {
		stop();
		st.add( this );
	}

	public void stopQuiet() {
		if( timeStart == 0 ) {
			Util.printLog( "[Warning]: Stop watch " + watchName + " not started" );
		}
		timeEnd = System.currentTimeMillis();
		totalTime += timeEnd - timeStart;
	}

	public void stopQuietAndAdd( StatContainer st ) {
		stopQuiet();
		st.add( this );
	}
}
