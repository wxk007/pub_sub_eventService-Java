/**
 * Created by wxk007 on 4/16/17.
 */
import com.sun.jmx.remote.internal.ArrayQueue;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//the logic of my event service: it would create 3 threads, they are used for: receive message from pub, send current
// message to subscriber and send history message to subscriber


//it would be changed after I figure out how to use zookeeper here
public class eventService {
    //this lock is used to protect the private field mHisList, which is used to store the history information
    private ReentrantLock mHisLock;
    private Condition mHisCond;

    private ReentrantLock mCurLock;
    private Condition mCurCond;

    //history information would be send when it reached five
    private volatile Queue<message> mHisList;

    private volatile Queue<message> mCurMessage;

    //this port is the one that can get the message from pub
    private String getPort;
    private Context getContext;
    private Socket getSocket;

    //topic is used to describe which topic does this es subscribe
    private String topic;

    //this port is used to send current and history message to the sub
    private String sendPort;
    private Context sendContext;
    private Socket sendSocket;

    public eventService(int getPort, int sendPort, String topic){
        this.getPort = Integer.toString(getPort);
        this.sendPort = Integer.toString(sendPort);
        this.topic = topic;
        mHisLock = new ReentrantLock();
        mHisCond = mHisLock.newCondition();
        mCurLock = new ReentrantLock();
        mCurCond = mCurLock.newCondition();
        mHisList = new LinkedList<>();
        mCurMessage = new LinkedList<>();
        getContext = ZMQ.context(1);
        getSocket = getContext.socket(ZMQ.SUB);
        sendContext = ZMQ.context(1);
        sendSocket = sendContext.socket(ZMQ.PUB);

        getSocket.connect("tcp://localhost:" + this.getPort);

        sendSocket.bind("tcp://*:" + this.sendPort);
    }

    public void receive(){
        getSocket.subscribe(topic.getBytes());

        while(!Thread.currentThread().isInterrupted()){
            String topic = getSocket.recvStr();
            String content = getSocket.recvStr();
            System.out.println("received: " + topic + " : " + content);
            //try to write the information into the CurList
            message curMessage = new message(topic,content);
            mCurLock.lock();
            //curMessage can only have one single message in it, guarentee it
            while(mCurMessage.size() == 1){
                try {
                    mCurCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mCurMessage.add(curMessage);
            //after you add it into the list, you have to signal up the waitting thread who is trying to get message from it
            mCurCond.signal();
            mCurLock.unlock();


            //we should put the
            mHisLock.lock();
            //the history list can have no more than 5 messages
            while(mHisList.size() > 4){
                try {
                    mHisCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mHisList.add(curMessage);
            //signal the sending method if we've got enough history messages
            if(mHisList.size() > 4)
                mHisCond.signal();
            mHisLock.unlock();

        }

    }

    //send method is merely used to send current message, has nothing to do with history list
    public void send(){
        while(!Thread.currentThread().isInterrupted()){
            mCurLock.lock();
            while (mCurMessage.size() == 0){
                try {
                    mCurCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            message tempMessage = mCurMessage.poll();
            System.out.println("sending: " + tempMessage.toString());
            sendSocket.sendMore(tempMessage.getTopic());
            sendSocket.send(tempMessage.getContent());
            if(mCurMessage.size() == 0)
                mCurCond.signal();
            mCurLock.unlock();
        }

    }

    //this method is used to send history list towards subscribers
    public void sendHistory(){
        while(!Thread.currentThread().isInterrupted()){
            mHisLock.lock();
            while(mHisList.size() < 5){
                try {
                    mHisCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.printf("Sending history messages to subscriber, we've got %d messages in historylist \n", mHisList.size());
            while(mHisList.size() != 0){
                message tempMessage = mHisList.poll();
                String historyTopic = "history";
                sendSocket.sendMore(historyTopic);
                sendSocket.send(tempMessage.getContent());
            }
            //after sending history message, signal the waiting thread
            if(mHisList.size() == 0)
                mHisCond.signal();
            mHisLock.unlock();
        }
    }


}

class message{
    private String topic;
    private String content;
    public message(String topic, String content){
        this.topic = topic;
        this.content = content;
    }
    public String getTopic(){
        return topic;
    }
    public String getContent(){
        return content;
    }
    public String toString(){
        return topic + " : " + content;
    }
}