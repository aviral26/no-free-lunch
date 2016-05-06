import dsblog.Client;
import dsblog.Config;
import utils.CommonUtils;

import java.util.Random;
import java.util.UUID;

public class Tester {

    public static void main(String[] args) throws Exception {

        Runnable flow = () -> {
            // Choose server randomly.
            // Flow --> post, post, lookup, sync, lookup.
            Client client = new Client();

            try {

                int server_id = Math.abs(new Random().nextInt()) % Config.NUMBER_OF_SERVERS;

                String message_1 = UUID.randomUUID() + " " + Thread.currentThread().getName();
                client.doPost(server_id, message_1);
                String message_2 = UUID.randomUUID() + " " + Thread.currentThread().getName();
                client.doPost(server_id, message_2);

                String[] response = client.doLookup(server_id);
                System.out.print("thread-" + Thread.currentThread().getName() + " Response to first lookup: ");
                boolean flag = false;
                for(String s : response){
                    System.out.println(s);
                    if(s.equals(message_1))
                        flag = true;
                    if(s.equals(message_2) && !flag)
                        throw new Exception();
                }

                client.doSync(server_id, Math.abs(new Random().nextInt()) % Config.NUMBER_OF_SERVERS);

                response = client.doLookup(server_id);
                flag = false;
                for(String s : response){
                    if(s.equals(message_1))
                        flag = true;
                    if(s.equals(message_2) && !flag)
                        throw new Exception();
                }

            }
            catch (Exception e){
                System.out.println("thread-" + Thread.currentThread().getName() + " ERROR caught.");
                e.printStackTrace();
            }
            System.out.println("thread-" + Thread.currentThread().getName() + " done.");
        };
        Config.init();
        int threads = Integer.parseInt(args[0]);
        System.out.println("Starting " + threads + " connections to cluster.");
        for(int i = 0; i < threads; i++)
            CommonUtils.startThreadWithName(flow, "thread-" + i);

        System.out.println("Main tester thread finished.");
    }
}
