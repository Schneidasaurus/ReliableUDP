import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * CustomPacket contains information needed this protocol,
 * constructors for both data packets and ack packets,
 * and a method for updating the checksum
 *
 * Created by Andrew Schneider on 6/29/2017.
 */
public class CustomPacket {

    // Data needed for the protocol
    private short cksum;
    private short len;
    private int ackno;
    private int seqno;
    private byte[] data;

    // public accessor methods
    public short getCksum() {return cksum; }
    public int getAckno() { return ackno; }
    public int getSeqno() {return seqno; }
    public byte[] getData() { return data; }

    // public mutator
    public void setCksum(short cksum){ this.cksum = cksum; }

    // Don't allow this method to be instantiated without values to populate fields
    private CustomPacket(){}

    /**
     * Constructor called to create data-carrying packets
     * @param payload Data to be transmitted
     * @param packetno Sequence number of data
     * @param cksum Checksum of data (in this case 0=good, 1=bad)
     */
    public CustomPacket(byte[] payload, int packetno, short cksum){

        this.cksum = cksum;
        len = (short)(12 + payload.length);
        ackno = packetno;
        seqno = packetno;
        data = payload;
    }

    /**
     * Constructor called to create ack packets
     * @param ackno Sequence number being acked
     * @param cksum Checksum of data (in this case 0=good, 1=bad)
     */
    public CustomPacket(int ackno, short cksum){
        this.cksum = cksum;
        this.ackno = ackno;
        len = 8;
    }

    /**
     * Constructor called to parse received information
     * @param packetIn Data to be parsed
     */

    public CustomPacket(byte[] packetIn){
        ByteBuffer buffer = ByteBuffer.wrap(packetIn);
        cksum = buffer.getShort();
        len = buffer.getShort();
        ackno = buffer.getInt();
        if (len != 8){
            seqno = buffer.getInt();
            data = new byte[len - 12];
            buffer.get(data, 0, data.length);
        }
    }

    /**
     * Takes data stored in object and bundles it into a DatagramPacket
     * @return DatagramPacket containing info stored in this object
     */
    public DatagramPacket getPacket(InetAddress recieverAddr, int port){

        ByteBuffer buffer = ByteBuffer.allocate(len);
        buffer.putShort(cksum);
        buffer.putShort(len);
        buffer.putInt(ackno);
        if (len != 8) {
            buffer.putInt(seqno);
            buffer.put(data);
        }

        return new DatagramPacket(buffer.array(), len, recieverAddr, port);
    }

    public static CustomPacket parsePacket(byte[] packetIn){ return new CustomPacket(packetIn);  }

    public static void main(String[] args){
        CustomPacket packet = new CustomPacket();
    }
}
