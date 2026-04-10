package net.sf.freecol.server.ai.colony;

public class Utils {

    public static String compressIdForLogging(String s) {
        return s.replaceAll("model\\.[^.]*\\.", "");
    }
}
