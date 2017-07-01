/**
 * Created by aksch_000 on 6/29/2017.
 */


public abstract class ReliableUDPSkeleton {

    protected int packetSize = 500;
    protected int timeoutLength = 2000;
    protected int windowSize = 5;
    protected double dropCorruptRate = 0;
    protected int PORT = 13;


}