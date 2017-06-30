/**
 * Created by aksch_000 on 6/29/2017.
 */

import  org.apache.commons.cli.*;

public abstract class ReliableUDPSkeleton {

    protected static int packetSize = 500;
    protected static int timeoutLength = 3000;
    protected static short windowSize = 5;
    protected static double dropPercent = 0;
    protected static final int PORT = 13;
    protected static Options options;


    static {
        options = new Options();
        options.addOption("s", true, "Packet size");
        options.addOption("t", true, "Timeout length in ms");
        options.addOption("w", true, "Window size");
        options.addOption("d", true, "Percentage of packets to drop or corrupt. Enter as a decimal.");

    }
}