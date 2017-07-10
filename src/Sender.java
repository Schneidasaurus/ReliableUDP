import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Sender builds on unreliable UDP datagram packets to create a reliable file
 * transfer protocol to be used with the included Receiver class
 *
 * Created by Andrew Schneider on 6/29/2017.
 */
public class Sender extends ReliableUDPSkeleton {

    private InetAddress receiverIp;
    private String fileName = "outgoing.txt";

    public static void main(String[] args){
        Options options = new Options();
        options.addOption("s", true, "Packet size");
        options.addOption("t", true, "Timeout length in ms");
        options.addOption("w", true, "Window size");
        options.addOption("d", true, "Percentage of packets to drop or corrupt, expressed as a decimal");

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
        sender.send();
    }

    public Sender(){}

    public void send(){
        try (DatagramSocket socket = new DatagramSocket(0)){
            socket.setSoTimeout(timeoutLength);
            CustomPacket[] frame = new CustomPacket[windowSize];
            ByteBuffer messageBuffer = ByteBuffer.wrap(Files.readAllBytes(Paths.get(fileName)));
            boolean lastPacket = false;
            int seqno = 1;
            int current = 0;
            while (true){
                for (int i = 0; i < frame.length; i++){
                    byte[] payload = new byte[packetSize > messageBuffer.remaining()? messageBuffer.remaining() : packetSize];
                    if (messageBuffer.hasRemaining())messageBuffer.get(payload, 0, payload.length);
                    else {
                        payload = new byte[0];
                        lastPacket = true;
                    }
                    frame[i] = new CustomPacket(payload, seqno, (short)0);
                    current += packetSize;
                    socket.send(frame[i].getPacket(receiverIp, PORT));
                    System.out.println(String.format("[SENDing]: %d [%d:%d] [%s]", seqno, packetSize * (seqno -1), (packetSize * seqno) -1, "SENT"));
                    seqno++;
                    if (lastPacket) break;
                }
                if (lastPacket) break;
            }
        } catch (SocketException e) {
            System.out.println("Unable to bind socket");
        } catch (IOException e) {
            System.out.println("Problem reading file");
        }
    }


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
        public void setReceiverIp(String addr){
            try {
                sender.receiverIp = InetAddress.getByName(addr);
            } catch (UnknownHostException e) {
                System.out.println("Unable to resolve host");
            }
        }

        public Sender create() { return sender; }
    }
}
