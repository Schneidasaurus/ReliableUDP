package com.andrewkschneider.ReliableUDP;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * Sender builds on unreliable UDP datagram packets to create a reliable file
 * transfer protocol to be used with the included Receiver class
 *
 * Created by Andrew Schneider on 6/29/2017.
 */
public class Sender {

    private static String cmdLineSyntax = "java -cp \"commons-cli-1.4.jar;.\" com.andrewkschneider.ReliableUDP.Sender [options] receiver_ip_address receiver_port";
    private InetAddress receiverIp;
    private int packetSize = 500;
    private int timeoutLength = 2000;
    private int windowSize = 5;
    private double dropCorruptRate = 0;
    private int PORT = 13;
    private String fileName = "outgoing.txt";
    private int sleepTime = 500;

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
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(0);
        }

        Builder builder = new Builder();
        if (cmd != null) {
            if (cmd.hasOption("s")) builder.setPacketSize(Integer.parseInt(cmd.getOptionValue("s")));
            if (cmd.hasOption("t")) builder.setTimeout(Integer.parseInt(cmd.getOptionValue("t")));
            if (cmd.hasOption("w")) builder.setWindowSize(Integer.parseInt(cmd.getOptionValue("w")));
            if (cmd.hasOption("d")) builder.setDropCorruptRate(Double.parseDouble(cmd.getOptionValue("d")));
        } else throw new NullPointerException("CommandLine cmd is null");
        if (cmd.getArgs().length < 2){
            System.out.println("Receiver IP and port are required");
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(0);
        }else {
            builder.setReceiverIp(cmd.getArgs()[0]);
            builder.setPort(Integer.parseInt(cmd.getArgs()[1]));
        }

        Sender sender = builder.create();
        sender.send();
    }

    public Sender(){}

    public void send(){
        try (DatagramSocket socket = new DatagramSocket(0)){

            DatagramPacket response = new DatagramPacket(new byte[2048], 2048);
            CustomPacket responsePacket = null;
            socket.setSoTimeout(timeoutLength);
            CustomPacket[] window = new CustomPacket[windowSize];
            ByteBuffer messageBuffer = ByteBuffer.wrap(Files.readAllBytes(Paths.get(fileName)));
            boolean lastPacket = false;
            int seqno = 1;
            int lastAck = 0;
            String ackCond;

            Random rng = new Random();
            String sendCondition;
            short cksum;
            boolean sendPacket = true;

            Instant start = Instant.now();

            while (true){
                // reset window data
                for (int i = 0; i < window.length; i++) window[i] = null;

                // read in from file data and create packets to populate window
                for (int i = 0; i < window.length; i++){

                    cksum = (short)0;
                    sendCondition = "SENT";

                    byte[] payload = new byte[packetSize > messageBuffer.remaining()? messageBuffer.remaining() : packetSize];

                    if (messageBuffer.hasRemaining())messageBuffer.get(payload, 0, payload.length);
                    else {
                        payload = new byte[0];
                        lastPacket = true;
                    }


                    if (rng.nextDouble() < dropCorruptRate){
                        if (rng.nextInt() % 2 == 1) {
                            cksum = (short)1;
                            sendCondition = "ERRR";
                        }
                        else {
                            sendPacket = false;
                            sendCondition = "DROP";
                        }
                    }

                    window[i] = new CustomPacket(payload, seqno, cksum);
                    if (sendPacket) socket.send(window[i].getPacket(receiverIp, PORT));

                    printMessage("SENDing", seqno, sendCondition, start);

                    seqno++;
                    if (lastPacket) break;
                }

                // Wait for ack and process
                while (responsePacket == null || seqno != responsePacket.getAckno()){
                    try {
                        ackCond = "";
                        socket.receive(response);
                        responsePacket = new CustomPacket(response.getData());
                        if (responsePacket.getCksum() == 1) ackCond = "[ErrAck.]";
                        else if (lastAck == responsePacket.getAckno()) ackCond = "[DuplAck]";
                        else if (seqno == responsePacket.getAckno()) {
                            ackCond = "[MoveWnd]";
                            lastAck = responsePacket.getAckno();
                        }
                        System.out.println(String.format("[AckRcvd]: %3d %s", responsePacket.getAckno(), ackCond));
                    } catch (SocketTimeoutException e){
                        System.out.println(String.format("[TimeOut]: %3d", seqno));
                    }

                    if (responsePacket == null || seqno != responsePacket.getAckno()){
                        for (CustomPacket packet: window) {
                            if (packet == null) break;

                            sendCondition = "SENT";
                            sendPacket = true;
                            packet.setCksum((short)0);

                            int tempSeqno = packet.getSeqno();
                            if (rng.nextDouble() < dropCorruptRate){
                                if (rng.nextInt() %2 == 1){
                                    packet.setCksum((short)1);
                                    sendCondition = "ERRR";
                                }
                                else {
                                    sendPacket = false;
                                    sendCondition = "DROP";
                                }
                            }

                            if (sendPacket)socket.send(packet.getPacket(receiverIp, PORT));
                            printMessage("ReSend.", tempSeqno, sendCondition, start);
                        }

                    }
                }

                if (lastPacket) break;
            }

        } catch (SocketException e) {
            System.out.println("Unable to bind socket");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Problem reading file");
            System.exit(0);
        }
    }

    private void printMessage(String sendType, int seqno, String sendCondition, Instant start) {
        try { Thread.sleep(sleepTime); }
        catch (InterruptedException e) { e.printStackTrace(); }
        System.out.println(String.format("[%s]: %3d [%10d:%10d] %10dms [%s]",sendType, seqno, packetSize * (seqno -1), (packetSize * seqno) -1, Duration.between(start, Instant.now()).toMillis(), sendCondition));
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
