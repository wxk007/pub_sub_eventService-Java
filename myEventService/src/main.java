import java.util.Scanner;

/**
 * Created by wxk007 on 4/16/17.
 */
public class main {
    public static void main(String args[]){
        Scanner mScanner = new Scanner(System.in);
        int recPort, sendPort;
        System.out.println("Please input the port of publisher");
        while(true){
            try{
                recPort = mScanner.nextInt();
                break;
            }catch (Exception e){
                System.out.println("Please input an integer");
            }
        }

        System.out.println("Please input the port of subscriber");
        while(true){
            try{
                sendPort = mScanner.nextInt();
                break;
            }catch (Exception e){
                System.out.println("Please input an integer");
            }
        }
        System.out.println("Please input the topic");
        String topic = mScanner.next();

        eventService mES = new eventService(recPort, sendPort,topic);

        Thread recCurThread = new Thread(()->{
           mES.receive();
        });

        Thread sendCurThread = new Thread(() ->{
            mES.send();
        });

        Thread sendHisThread = new Thread(() -> {
            mES.sendHistory();
        });


        recCurThread.start();
        sendCurThread.start();
        sendHisThread.start();
    }
}
