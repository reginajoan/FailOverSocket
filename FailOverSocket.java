import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
public class FailOverSocket {
    private static boolean flag = false;

    public static void main(String[] args) throws Exception {
        /*Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(9000);
                    //do {
                        System.out.println("Waiting Transaction ..");
                        Socket clientSocket = ss.accept();
                        clientSocket.setKeepAlive(true);
                        while (clientSocket.getInputStream().available() == 0) {
                            Thread.sleep(100L);
                        }
                        byte[] data;
                        int bytes;
                        data = new byte[clientSocket.getInputStream().available()];
                        bytes = clientSocket.getInputStream().read(data,0,data.length);
                        String dataDB = new String(data, 0, bytes, "ASCII");
                        System.out.println(dataDB);
                        String dataFromHobis = getFromServer(dataDB);
                        System.out.println("data from hobis " + dataFromHobis);
                        if(dataFromHobis != null){
                            clientSocket.getOutputStream().write(dataFromHobis.getBytes("ASCII"));
                        }
                    //} while (true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
         */
        try {
            ServerSocket ss = new ServerSocket(9000);
            //do {
            System.out.println("Waiting Transaction ..");
            Socket clientSocket = ss.accept();
            clientSocket.setKeepAlive(true);
            while (clientSocket.getInputStream().available() == 0) {
                Thread.sleep(100L);
            }
            byte[] data;
            int bytes;
            data = new byte[clientSocket.getInputStream().available()];
            bytes = clientSocket.getInputStream().read(data,0,data.length);
            String dataDB = new String(data, 0, bytes, "ASCII");
            System.out.println(dataDB);
            String dataFromHobis = getFromServer(dataDB);
            System.out.println("data from hobis " + dataFromHobis);
            if(dataFromHobis != null){
                clientSocket.getOutputStream().write(dataFromHobis.getBytes("ASCII"));
            }
            //} while (true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFromServer(String dataDB) throws Exception {
        final int timeout = 10;
        String data = "";

        //while (true){
        data = RunningProgram(dataDB, timeout);
        if(flag){
            flag = false;
            return data;
        }else {
            flag = true;
            return RunningProgram1(dataDB, timeout);
        }
    }
    public static String RunningProgram(String dataDB, int timeout) throws Exception{
        String getFromHli = "";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Task1(dataDB));

        try {
            System.out.println("Server 1 running");
            System.out.println("Started..");
            getFromHli = future.get(timeout, TimeUnit.SECONDS);
            System.out.println("Finished!");
            flag = true;
            executor.shutdownNow();
            return getFromHli;
        } catch (Exception e) {
            future.cancel(true);
            System.out.println("Terminated!");
            flag = false;
            executor.shutdownNow();
            return e.getMessage();
        }
    }
    public static String RunningProgram1(String dataDB, int timeout) throws Exception{
        String getFromHli = "";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        //Executors.
        Future<String> future = executor.submit(new Task1(dataDB));
        try {
            System.out.println("Server 2 running");
            System.out.println("Started..");
            //System.out.println(future.get(5, TimeUnit.SECONDS));
            getFromHli = future.get(timeout, TimeUnit.SECONDS);
            System.out.println("Finished!");
            flag = true;
            executor.shutdownNow();
            return getFromHli;
        } catch (Exception e) {
            future.cancel(true);
            System.out.println("Terminated!");
            flag = false;
            executor.shutdownNow();
            return e.getMessage();
        }
    }
}

class Task1 implements Callable<String> {
    private static String host = "192.168.88.98";
    private static int port = 1212;
    private static String dataDB;
    private static Socket clientSocket = null;
    public Task1(String dataDB){
        this.dataDB = dataDB;
    }
    static String server = null;
    static boolean connected = false;
    static DataInputStream in = null;
    static DataOutputStream out = null;
    static String data = null;

    @Override
    public String call() {
        try {
            return connect(host,port);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
    static String Refresing(){
        try{
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            /*
            Thread thread = new Thread(){
                String msg = "";
                public void run() {
                    while(connected){
                        try {
                            msg = in.readUTF();
                            System.out.println(msg);
                            data = msg;
                        }catch(IOException e){
                            connected = false;
                            System.out.println("Reconnecting...");
                            while(!connected)
                                connect(server, port);
                        }
                    }
                }
            };
            thread.start();
             */
            String msg = "";
            while(connected){
                try {
                    msg = in.readUTF();
                    System.out.println(msg);
                    data = msg;
                }catch(IOException e){
                    connected = false;
                    System.out.println("Reconnecting...");
                    while(!connected)
                        connect(server, port);
                }
            }
        }catch (Exception e){
            connected = false;
        }
        return data;
    }

    static String connect(String hostt, int portt){
        try{
            clientSocket = new Socket(hostt, portt);
            server = hostt;
            port = portt;
            connected=true;
            data = Refresing();
            System.out.println("Connected!");
            SendAndGetFromHLI(dataDB);
        }catch(Exception e){
            System.out.println("Can't connect !");
            connected = false;
        }
        return data;
    }
    public static String SendAndGetFromHLI(String dataDB){
        String print = "";
        System.out.println("Send data to server : "+dataDB);
        try {
            clientSocket = new Socket(host,port);
            clientSocket.getOutputStream().write(dataDB.getBytes("ASCII"));
            while (clientSocket.getInputStream().available() == 0) {
                Thread.sleep(100L);
            }
            byte[] data = new byte[clientSocket.getInputStream().available()];
            int bytes = clientSocket.getInputStream().read(data, 0, data.length);
            print = new String(data, 0, bytes, "ASCII");//.substring(4,bytes);
            System.out.println("from server : "+print);
            return print;
        } catch (IOException ex) {
            return ex.getMessage();
        } catch (InterruptedException ie) {
            return ie.getMessage();
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}