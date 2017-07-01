import org.apache.commons.cli.*;

/**
 * Sender builds on unreliable UDP datagram packets to create a reliable file
 * transfer protocol to be used with the included Receiver class
 *
 * Created by Andrew Schneider on 6/29/2017.
 */
public class Sender extends ReliableUDPSkeleton {

    private String receiverIp;

    public static void main(String[] args){
        Options options = new Options();
        options.addOption("s", true, "Packet size");
        options.addOption("t", true, "Timeout length in ms");
        options.addOption("w", true, "Window size");
        options.addOption("d", true, "Percentage of packets to drop or corrupt, expressed as a percentage");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -cp commons-cli-1.4 Sender [options] receiver_ip_address receiver_port", options);
        }

        Builder builder = new Builder();
        if (cmd != null) {
            if (cmd.hasOption("s")) builder.setPacketSize(Integer.parseInt(cmd.getOptionValue("s")));
            if (cmd.hasOption("t")) builder.setTimeout(Integer.parseInt(cmd.getOptionValue("t")));
            if (cmd.hasOption("w")) builder.setWindowSize(Integer.parseInt(cmd.getOptionValue("w")));
            if (cmd.hasOption("d")) builder.setDropCorruptRate(Double.parseDouble("d"));
        } else throw new NullPointerException("CommandLine cmd is null");
        builder.setReceiverIp(args[args.length-2]);
        builder.setPort(Integer.parseInt(args[args.length-1]));

        Sender sender = builder.create();
    }

    public Sender(){}

    public static Builder Builder(){
        return new Builder();
    }

    /**
     *
     */
    static class Builder {
        private Sender sender;

        private Builder(){ sender = new Sender(); }

        public void setPacketSize(int size){ sender.packetSize = size; }
        public void setTimeout(int timeout) { sender.timeoutLength = timeout; }
        public void setWindowSize( int size) { sender.windowSize = size; }
        public void setDropCorruptRate(double rate) { sender.dropCorruptRate = rate; }
        public void setPort(int port) { sender.PORT = port; }
        public void setReceiverIp(String addr){ sender.receiverIp = addr; }

        public Sender create() { return sender; }
    }
}
