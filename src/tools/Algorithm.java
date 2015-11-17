package tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Algorithm {
	protected HashMap<String, Integer> str2int;
	protected ArrayList<String> strlist;

	protected Algorithm(String rulefile, String Rfile, String Sfile)
			throws IOException {
		str2int = new HashMap<String, Integer>();
		strlist = new ArrayList<String>();
		// Add empty string first
		str2int.put("", 0);
		strlist.add("");

		BufferedReader br = new BufferedReader(new FileReader(rulefile));
		String line;
		while ((line = br.readLine()) != null) {
			String[] pstr = line.split("(,| |\t)+");
			for (String str : pstr)
				getID(str);
		}
		br.close();

		br = new BufferedReader(new FileReader(Rfile));
		while ((line = br.readLine()) != null) {
			String[] pstr = line.split("( |\t)+");
			for (String str : pstr)
				getID(str);
		}
		br.close();

		br = new BufferedReader(new FileReader(Sfile));
		while ((line = br.readLine()) != null) {
			String[] pstr = line.split("( |\t)+");
			for (String str : pstr)
				getID(str);
		}
		br.close();
	}

	private int getID(String str) {
		if (!str2int.containsKey(str)) {
			str2int.put(str, strlist.size());
			strlist.add(str);
		}
		return str2int.get(str);
	}
}
