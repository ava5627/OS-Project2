import com.sun.nio.sctp.SctpChannel;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

public class ChannelThread extends Thread {
    SctpChannel sc;
    Mutex mutex;
    int connected_id;

    public ChannelThread(SctpChannel sc, Mutex mutex, int connected_id) {
        this.sc = sc;
        this.mutex = mutex;
        this.connected_id = connected_id;
    }

    public void run() {
        try {

            ByteBuffer buf = ByteBuffer.allocateDirect(Message.MAX_MSG_SIZE); // Messages are received over SCTP using ByteBuffer
            sc.configureBlocking(true); // Ensures that the channel will block until a message is received
            while (true) {
                // listen for msg
                Message message = Message.receiveMessage(sc);
                mutex.updateClock(message.clock);
                if (MessageType.request == message.msgType) {
                    Request req = new Request(message.sender, message.clock);
                    mutex.pq.put(req);
                    // Send reply
                    Message reply = new Message(mutex.nodeID, MessageType.reply, "REPLY", mutex.logClock.get());
                    reply.send(sc);
                }
                else if (MessageType.release == message.msgType) {
                    Request req = new Request(message.sender, message.clock);
                    mutex.pq.remove(req);
                }

                if (mutex.requestTime.get() < message.clock) {
                    mutex.higherTimestamp.add(message.sender);
                }


            }
        } catch (ClosedChannelException e){
            System.out.println("Received all messages");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}