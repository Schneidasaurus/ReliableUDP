package com.andrewkschneider.ReliableUDP;

import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.*;
import java.util.Random;

/**
 * Receiver receives data from the included Sender class via unreliable
 * UDP datagram packets
 *
 * Created by Andrew Schneider on 6/29/2017.
 */
public class Receiver {

    private InetAddress address;
    private int packetSize = 500;
    private int timeoutLength = 2000;
    private int windowSize = 5;
    private double dropCorruptRate = 0;
    private int PORT = 13;

    private static final String cmdLineSyntax = "java -cp \"commons-cli-1.4.jar;.\" com.andrewkschneider.ReliableUDP.Receiver [options] receiver_ip_address receiver_port";

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
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(0);
        }

        Builder builder = new Builder();
        if (cmd != null){
            if (cmd.hasOption("w")) builder.setWindowSize(Integer.parseInt(cmd.getOptionValue("w")));
            if (cmd.hasOption("d")) builder.setDropCorruptRate(Double.parseDouble(cmd.getOptionValue("d")));
        }else throw new NullPointerException("CommandLind cmd is null");
        if (cmd.getArgs().length < 2){
            System.out.println("IP and port are required");
            formatter.printHelp(cmdLineSyntax, options);
            System.exit(0);
        } else {
            builder.setAddress(cmd.getArgs()[0]);
            builder.setPort(Integer.parseInt(cmd.getArgs()[1]));
        }

        Receiver receiver = builder.create();
        receiver.receive();
    }

    public void receive(){
        try (DatagramSocket socket = new DatagramSocket(PORT); Writer writer = new BufferedWriter(new FileWriter("output.txt"));){


            DatagramPacket response = new DatagramPacket(new byte[2048], 2048);
            CustomPacket responsePacket = null;
            CustomPacket ackPacket;
            int ackno = 1;
            CustomPacket[] window = new CustomPacket[windowSize];
            String packetCond;
            boolean dup;
            boolean finalPacket = false;

            Random rng = new Random();
            String ackCond;
            boolean sendAck;

            while (true){

                for (int i = 0; i < window.length; i++) window[i]= null;

                // receive packets until window is full
                for (int i = 0; i < window.length; i++) {
                    while (window[i] == null) {

                        dup = false;

                        socket.receive(response);
                        responsePacket = new CustomPacket(response.getData());

                        if (responsePacket.getCksum() == 1) packetCond = "CRPT";
                        else if (responsePacket.getSeqno() != ackno) packetCond = "!Seq";
                        else packetCond = "RECV";
                        if (responsePacket.getSeqno() < ackno ) {
                            dup = true;
                            if (responsePacket.getSeqno() < ackno - 5) {
                                ackPacket = new CustomPacket(ackno, (short)0);
                                ackCond = "SENT";
                                sendAck = true;
                                if (rng.nextDouble() < dropCorruptRate){
                                    if (rng.nextInt() %2 == 1) {
                                        ackPacket.setCksum((short) 1);
                                        ackCond = "ERRR";
                                    }
                                    else {
                                        sendAck = false;
                                        ackCond = "DROP";
                                    }
                                }

                                if (sendAck)socket.send(ackPacket.getPacket(response.getAddress(), response.getPort()));
                                System.out.println(String.format("[ACK] %3d [%s]", ackno, ackCond));
                            }
                        }



                        printMessage(dup, responsePacket.getSeqno(), packetCond);

                        if (responsePacket.getSeqno() == ackno && responsePacket.getCksum() != 1) {
                            window[i] = responsePacket;
                            ackno++;
                            if (responsePacket.getData().length == 0){
                                finalPacket = true;
                                break;
                            }
                        }
                    }

                    if (finalPacket) break;
                }

                // write contents of window to file
                for (CustomPacket packet: window){
                    if (packet != null)writer.write(new String(packet.getData(), "US-ASCII"));
                }

                // send ack to sender to move window
                ackPacket = new CustomPacket(ackno, (short)0);
                ackCond = "SENT";
                sendAck = true;
                if (rng.nextDouble() < dropCorruptRate){
                    if (rng.nextInt() % 2 == 1){
                        ackPacket.setCksum((short)1);
                        ackCond = "ERRR";
                    }
                    else {
                        sendAck = false;
                        ackCond = "DROP";
                    }
                }

                if (sendAck) socket.send(ackPacket.getPacket(response.getAddress(), response.getPort()));
                System.out.println(String.format("[ACK.]: %3d [%s]", ackno, ackCond));

                if (finalPacket) {
                    boolean finalAckFail = true;
                    socket.setSoTimeout(3000);
                    while (finalAckFail) {
                        try {
                            socket.receive(response);

                            ackPacket = new CustomPacket(ackno, (short)0);
                            ackCond = "SENT";
                            sendAck = true;
                            if (rng.nextDouble() < dropCorruptRate){
                                if (rng.nextInt() % 2 == 1){
                                    ackPacket.setCksum((short)1);
                                    ackCond = "ERRR";
                                }
                                else {
                                    sendAck = false;
                                    ackCond = "DROP";
                                }
                            }

                            if (sendAck) socket.send(ackPacket.getPacket(response.getAddress(), response.getPort()));
                            System.out.println(String.format("[ACK.]: %3d [%s]", ackno, ackCond));

                        } catch (SocketTimeoutException e) {
                            finalAckFail = false;
                        }
                    }
                    break;
                }
            }


        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void printMessage(boolean dup, int seqno, String packetCond){
        System.out.println(String.format("[%s]: %3d [%s]", dup ? "DUPL" : "RECV", seqno, packetCond));
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
                System.exit(0);
            }
        }

        public Receiver create(){ return receiver; }
    }
}

