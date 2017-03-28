package org.folio.okapi.util;

import java.util.LinkedList;
import java.util.List;

public class SplitProcessArgs {

  public static List<String> split(String cmd) {
    boolean q = false;
    List<String> l = new LinkedList<>();
    int j = 0;
    int i = 0;
    while (i < cmd.length()) {
      if (cmd.charAt(i) == '\"') {
        if (i > j) {
          l.add(cmd.substring(j, i));
        }
        j = i + 1;
        q = !q;
      } else if (!q && cmd.charAt(i) == ' ') {
        if (i > j) {
          l.add(cmd.substring(j, i));
        }
        j = i + 1;
      }
      i++;
    }
    if (i > j) {
      l.add(cmd.substring(j, i));
    }
    return l;
  }
}
