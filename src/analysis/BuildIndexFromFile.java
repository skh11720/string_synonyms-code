package analysis;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import tools.WYK_HashMap;

public class BuildIndexFromFile {
  public static void main(String[] args) throws IOException {
    List<Triple> list = new ArrayList<Triple>();
    BufferedInputStream bis = new BufferedInputStream(
        new FileInputStream(args[0]));
    while (true) {
      Triple t = Triple.read(bis);
      if (t == null) break;
      list.add(t);
    }
    bis.close();
    long starttime = System.currentTimeMillis();
    WYK_HashMap<Integer, WYK_HashMap<Long, List<Integer>>> idx = new WYK_HashMap<Integer, WYK_HashMap<Long, List<Integer>>>(
        20);
    for (int c = 0; c < list.size(); ++c) {
      Triple t = list.get(c);
      WYK_HashMap<Long, List<Integer>> localidx = idx.get(t.idx);
      if (localidx == null) {
        localidx = new WYK_HashMap<Long, List<Integer>>();
        idx.put(t.idx, localidx);
      }
      List<Integer> locallist = localidx.get(t.twogram);
      if (locallist == null) {
        locallist = new ArrayList<Integer>(100);
        localidx.put(t.twogram, locallist);
      }
      locallist.add(t.id);
    }
    long duration = System.currentTimeMillis() - starttime;
    System.out.println(duration);
    for(WYK_HashMap<Long, List<Integer>> localidx : idx.values())
      localidx.printStat();

    // int sum = 0;
    // int ones = 0;
    // long count = 0;
    // ///// Statistics
    // for (Map<Long, List<Integer>> curridx : idx.values())
    // for (List<Integer> locallist : curridx.values()) {
    // if (locallist.size() == 1) {
    // ++ones;
    // continue;
    // }
    // sum++;
    // count += locallist.size();
    // }
    // System.out.println("key-value pairs(all) : " + (sum + ones));
    // System.out.println("key-value pairs(w/o 1) : " + sum);
    // System.out.println("iIdx size(w/o 1) : " + count);
    // System.out.println("Rec per idx(w/o 1) : " + ((double) count) / sum);
    // System.out.println("2Gram retrieval: " + Record.exectime);
  }

  static byte[]     intbuffer  = new byte[4];
  static byte[]     longbuffer = new byte[8];
  static ByteBuffer intwrap    = ByteBuffer.wrap(intbuffer);
  static ByteBuffer longwrap   = ByteBuffer.wrap(longbuffer);

  private static class Triple {
    int  idx;
    long twogram;
    int  id;

    static Triple read(BufferedInputStream bis) throws IOException {
      Triple inst = new Triple();
      intwrap.clear();
      int code = bis.read(intbuffer, 0, 4);
      if (code == -1) return null;
      inst.idx = intwrap.getInt();
      longwrap.clear();
      bis.read(longbuffer, 0, 8);
      inst.twogram = longwrap.getLong();
      intwrap.clear();
      bis.read(intbuffer, 0, 4);
      inst.id = intwrap.getInt();
      return inst;
    }
  }
}
