package analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class ResultStats {
  public static void main(String[] args) throws IOException {
    String filename = "ss.txt";
    BufferedReader br = new BufferedReader(new FileReader(filename));
    String line;
    HashMap<String, Integer> freq = new HashMap<String, Integer>();
    while ((line = br.readLine()) != null) {
      String[] pline = line.split("\t");
      String[] left = pline[0].trim().split(" ");
      String[] right = pline[2].trim().split(" ");
      int i = Math.min(left.length, 5);
      int j = Math.min(right.length, 5);
      String key = String.format("%d, %d", i, j);
      if (freq.containsKey(key))
        freq.put(key, freq.get(key) + 1);
      else
        freq.put(key, 1);
    }
    br.close();
    for (Entry<String, Integer> e : freq.entrySet())
      System.out.println(e.getKey() + ": " + e.getValue());
  }
}
