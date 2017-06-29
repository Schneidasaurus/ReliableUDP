import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;

/**
 * Created by aksch_000 on 6/29/2017.
 */
public class CustomPacket {
    private DatagramPacket packet;

    short cksum;
    short len;
    int ackno;
    int seqno;
    byte[] data;

    public DatagramPacket getPacket() { return packet; }

    private CustomPacket(){}

    public CustomPacket(byte[] payload){

    }

    public static void main(String[] args){
        CustomPacket packet = new CustomPacket();
    }
}
