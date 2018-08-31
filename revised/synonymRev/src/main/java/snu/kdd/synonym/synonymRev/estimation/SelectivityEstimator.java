package snu.kdd.synonym.synonymRev.estimation;

import java.util.Arrays;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class SelectivityEstimator {
	OLSMultipleLinearRegression regressor;
	final double[] param;
	
	public SelectivityEstimator( double[][] x, double[] y ) {
		regressor = new OLSMultipleLinearRegression();
		regressor.newSampleData( y, x );
		param = regressor.estimateRegressionParameters();
	}

	public double[] getParameters() {
		return param;
	}

	public double predict(double[] x) {
		double prediction = param[0];
		for ( int i=0; i<x.length; ++i ) prediction += param[i+1] * x[i];
		return prediction;
	}

//	public static void main( String[] args ) {
//		double[] x = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000,};
//		double[] y1 = {121, 193, 262, 427, 832, 1250, 2077, 3259, 5599, 8786, 17318,};
//		double[] y2 = {142364, 210920, 291105, 541433, 1983102, 2949982, 5109236, 8583779, 17123674, 25993872, 65460941};
//
//		double[][] X1 = new double[x.length][];
//		for ( int i=0; i<x.length; ++i ) X1[i] = new double[] {x[i]};
//		double[][] X2 = new double[x.length][];
//		for ( int i=0; i<x.length; ++i ) X2[i] = new double[] {x[i], x[i]*x[i]};
//
//		SelectivityEstimator estimator1 = new SelectivityEstimator( X1, y2 );
//		System.out.println( Arrays.toString( estimator1.getParameters() ) );
//	}
}
