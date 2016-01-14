package sanitizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import tools.Stemmer;

public class Sanitizer {
  RuleSanitizer rs = new RuleSanitizer();
  DataSanitizer ds = new DataSanitizer();

  public static void main(String[] args) {
    // extractText();
    // sanitizeData();
    // sanitizeRule();
    removeDuplicates();
  }

  @SuppressWarnings("unused")
  private static void extractText() {
    String aolin = "/Users/wooyekim/Desktop/AOL-user-ct-collection/";
    String aolout = "/users/wooyekim/Documents/workspace/SynonymOptimized/aol/whole.txt";
    extractText(aolin, aolout);
  }

  // IMPORTANT: it does not remove duplicated strings!!!
  @SuppressWarnings("unused")
  private static void sanitizeData() {
    File in = new File("aol/whole.txt");
    File out = new File("aol/whole.sanitized.txt");
    Sanitizer san = new Sanitizer();
    san.sanitizeData(in, out);
  }

  @SuppressWarnings("unused")
  private static void sanitizeRule() {
    File in = new File("wordnet/rules.noun");
    File out = new File("wordnet/rules.sanitized.noun");
    Sanitizer san = new Sanitizer();
    san.sanitizeRule(in, out);
  }

  private static void removeDuplicates() {
    File in = new File("aol/whole.sanitized.txt");
    File out = new File("aol/whole.sanitized.uniq.txt");
    try {
      HashSet<String> dict = new HashSet<String>();
      BufferedReader br = new BufferedReader(new FileReader(in));
      BufferedWriter bw = new BufferedWriter(new FileWriter(out));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.isEmpty()) continue;
        dict.add(line);
      }
      br.close();
      for (String str : dict)
        bw.write(str + "\n");
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Extract queries only from given AOL dataset directory
   */
  private static void extractText(String indir, String out) {
    File lastfile = null;
    String laststr = null;
    try {
      File folder = new File(indir);
      BufferedWriter bw = new BufferedWriter(new FileWriter(out));
      // Process every file in the given directory
      for (File file : folder.listFiles()) {
        BufferedReader br = new BufferedReader(new FileReader(file));
        lastfile = file;
        // Skip the first line since it represents the schema
        String line = br.readLine();
        while ((line = br.readLine()) != null) {
          laststr = line;
          String[] pstr = line.split("\t");
          bw.write(pstr[1] + "\n");
        }
        br.close();
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (java.lang.ArrayIndexOutOfBoundsException e) {
      System.out.println("Filename: " + lastfile.toString());
      System.out.println("String: " + laststr);
      throw e;
    }
  }

  private void sanitizeData(File in, File out) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(in));
      BufferedWriter bw = new BufferedWriter(new FileWriter(out));
      String line;
      while ((line = br.readLine()) != null) {
        String sanitized = ds.sanitize(line);
        if (sanitized == null) continue;
        bw.write(sanitized + "\n");
      }
      br.close();
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void sanitizeRule(File in, File out) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(in));
      BufferedWriter bw = new BufferedWriter(new FileWriter(out));
      String line;
      while ((line = br.readLine()) != null) {
        String sanitized = rs.sanitize(line);
        if (sanitized == null) continue;
        bw.write(sanitized + "\n");
      }
      br.close();
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

class RuleSanitizer {
  Stemmer s = new Stemmer();

  String sanitize(String text) {
    // Make given string lower case
    text = text.toLowerCase();
    // Parse rule into two parts (lhs, rhs)
    String[] tokens = text.split(", ");
    // Apply stemmer
    assert (tokens.length == 2);
    StringBuilder builder = new StringBuilder();
    // Process LHS
    String[] lhs = tokens[0].split(" ");
    for (int i = 0; i < lhs.length; ++i) {
      lhs[i] = stem(lhs[i]);
      if (i != 0) builder.append(" ");
      builder.append(lhs[i]);
    }
    builder.append(", ");
    // Process RHS
    String[] rhs = tokens[1].split(" ");
    for (int i = 0; i < rhs.length; ++i) {
      rhs[i] = stem(rhs[i]);
      if (i != 0) builder.append(" ");
      builder.append(rhs[i]);
    }
    return builder.toString();
  }

  private String stem(String text) {
    char[] carray = text.toCharArray();
    s.add(carray, carray.length);
    s.stem();
    return s.toString();
  }
}

class DataSanitizer {
  Stemmer s = new Stemmer();

  /**
   * Sanitize the given string.</br>
   * If the string is navigational query or simple query, it returns null.
   */
  String sanitize(String text) {
    // Make given string lower case
    text = text.toLowerCase();
    // Remove navigational query
    if (checkNavigational(text)) return null;
    // Remove punctuations
    String step1 = removeStopCharacters(text);
    String[] pstr = step1.split(" ");
    // Remove simple queries
    if (pstr.length == 1) return null;
    // Apply stemmer
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < pstr.length; ++i) {
      pstr[i] = stem(pstr[i]);
      if (i != 0) builder.append(" ");
      builder.append(pstr[i]);
    }
    return builder.toString();
  }

  private String stem(String text) {
    char[] carray = text.toCharArray();
    s.add(carray, carray.length);
    s.stem();
    return s.toString();
  }

  private String removeStopCharacters(String text) {
    return text.replaceAll("\\.|,|'", "");
  }

  private boolean checkNavigational(String text) {
    return text.contains("www") || text.contains(".co")
        || text.contains(".com");
  }
}
