/**
 * Created by wxk007 on 4/16/17.
 */
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

//the sending content should be split by /n
public class my_pub {
    private Context mContext;

    private Socket mPublisher;

    private String port;

    public my_pub(int port){
        this.port = Integer.toString(port);
        mContext = ZMQ.context(1);
        mPublisher = mContext.socket(ZMQ.PUB);
        mPublisher.bind("tcp://*:" + this.port);
    }
    //use the envelop to specify the topic
    public void send (String topic, String content) throws Exception{
        try{
            mPublisher.sendMore(topic);
            mPublisher.send(content);
        }catch (Exception e){
            throw e;
        }
    }
    public void shutDown(){
        mPublisher.close ();
        mContext.term ();
    }

}
