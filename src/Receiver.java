import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.*;

/**
 * Receiver receives data from the included Sender class via unreliable
 * UDP datagram packets
 *
 * Created by Andrew Schneider on 6/29/2017.
 */
public class Receiver extends ReliableUDPSkeleton {

    private InetAddress address;

    public static void main(String[] args){
        Options options = new Options();
        options.addOption("w", true, "Window size");
        options.addOption("d", true, "Percentage of packets to drop or corrupt, expressed as a decimal");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e){
            System.out.println(e.getMessage());
            formatter.printHelp("java -cp commons-cli-1.4 Receiver [options] receiver_ip_address receiver_port", options);
        }

        Builder builder = new Builder();
        if (cmd != null){
            if (cmd.hasOption("w")) builder.setWindowSize(Integer.parseInt(cmd.getOptionValue("w")));
            if (cmd.hasOption("d")) builder.setDropCorruptRate(Double.parseDouble(cmd.getOptionValue("d")));
        }else throw new NullPointerException("CommandLind cmd is null");
        builder.setAddress(args[args.length-2]);
        builder.setPort(Integer.parseInt(args[args.length-1]));

        Receiver receiver = builder.create();
        receiver.receive();
    }

    public void receive(){
        try (DatagramSocket socket = new DatagramSocket(PORT)){
            StringBuilder builder = new StringBuilder();
            DatagramPacket response = new DatagramPacket(new byte[2048], 2048);
            while (true){
                socket.receive(response);
                CustomPacket receivedPacket = new CustomPacket(response.getData());
                builder.append(new String(receivedPacket.getData(), 0, receivedPacket.getData().length, "US-ASCII"));
                if (receivedPacket.getData().length ==0) break;
            }
            System.out.println(builder);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Builder {
        private Receiver receiver;

        public Builder(){ receiver = new Receiver(); }

        public void setWindowSize(int size) {receiver.windowSize = size;}
        public void setDropCorruptRate(double rate){ receiver.dropCorruptRate = rate; }
        public void setPort(int port) {receiver.PORT = port; }
        public void setAddress(String addr){
            try {
                receiver.address = InetAddress.getByName(addr);
            } catch (UnknownHostException e){
                System.out.println("Unable to resolve host");
            }
        }

        public Receiver create(){ return receiver; }
    }
}

