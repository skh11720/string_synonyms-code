package snu.kdd.synonym.synonymRev.tools;

import java.util.Map;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public abstract class AbstractParam {

	protected Map<String, Integer> mapParamI = new Object2IntArrayMap<>();
	protected Map<String, Double> mapParamD = new Object2DoubleArrayMap<>();
	protected Map<String, Boolean> mapParamB = new Object2BooleanArrayMap<>();
	protected Map<String, String> mapParamS = new Object2ObjectArrayMap<>();

	public final int getIntParam( String key ) { return mapParamI.get(key); }

	public final double getDoubleParam( String key ) { return mapParamD.get(key); }

	public final boolean getBooleanParam( String key ) { return mapParamB.get(key); }

	public final String getStringParam( String key ) { return mapParamS.get(key); }
	
	public final String getJSONString() { 
		StringBuilder bld = new StringBuilder("{");
		for ( Map.Entry<String, Integer> entry : mapParamI.entrySet() ) bld.append("\""+entry.getKey()+"\":\""+entry.getValue()+"\", ");
		for ( Map.Entry<String, Double> entry : mapParamD.entrySet() ) bld.append("\""+entry.getKey()+"\":\""+entry.getValue()+"\", ");
		for ( Map.Entry<String, Boolean> entry : mapParamB.entrySet() ) bld.append("\""+entry.getKey()+"\":\""+entry.getValue()+"\", ");
		for ( Map.Entry<String, String> entry : mapParamS.entrySet() ) bld.append("\""+entry.getKey()+"\":\""+entry.getValue()+"\", ");
		int l = bld.length();
		bld.delete(l-2, l);
		bld.append("}");
		return bld.toString();
	}
}
