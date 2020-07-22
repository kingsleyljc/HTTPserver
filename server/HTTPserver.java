import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class HTTPserver {
    public static PrintWriter writer;
    static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static void main(String[] args) throws Exception{
//        System.out.println("???");
        int port = 8888;
        new HTTPserver(port);   // 在端口8888启动服务器
    }
    ExecutorService pool = Executors.newCachedThreadPool();//处理http请求的线程的线程池
    int port;
    HTTPserver(int port) throws Exception{
        this.port = port;
        ServerSocket serverSocket = new ServerSocket(port);
        while (true){
            Socket socket = serverSocket.accept();
            ServerProcess tmp_process = new ServerProcess(socket);
            pool.submit(tmp_process);
        }
    }
    synchronized static void write_log(String msg) throws IOException {
        Date d = new Date();
        String s = sdf.format(d);
        System.out.println(s);
        File log_file = new File("./Log/log.txt");
        if (!log_file.exists())
            log_file.createNewFile();
        writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(log_file,true)),true);
        writer.println("访问时间"+s);
        writer.print(msg);
        writer.println();
        writer.close();
    }
    private class Logger implements Runnable{
        Logger(String tmp){
            msg = tmp;
        }
        String msg;
        public void run() {
            try {
                write_log(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class ServerProcess implements Runnable{
        Socket socket;
        OutputStream outputStream;
        InputStream inputStream;
        BufferedReader reader;
        String msg = "";
        ServerProcess(Socket socket) throws IOException {
            this.socket = socket;

            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));
        }
        public void run() {
            try{
                System.out.println("getRemoteSocketAddress:"+socket.getRemoteSocketAddress());
                msg += "请求来源:"+socket.getRemoteSocketAddress()+'\n';
                String line;
                boolean first = true;
                String cookie_username = null;
                String cookie_phoneNumber= null;
                String cookie_count= null;
                String reqPath = "";
                int method = 1; //1是get，2是post，3是head
                while((line = reader.readLine())!=null){
                    System.out.println(line);
                    if (line.startsWith("Cookie")){
                        msg+=line+'\n';
                        String cookie = line.substring(line.indexOf(':')+2);
                        String cookie_content[] = cookie.split("; ");
                        for(String t:cookie_content){
                            if(t.startsWith("Username"))
                                cookie_username = t.substring(t.indexOf("=")+1);
                            else if (t.startsWith("PhoneNumber"))
                                cookie_phoneNumber = t.substring(t.indexOf("=")+1);
                            else if (t.startsWith("Count"))
                                cookie_count = t.substring(t.indexOf("=")+1);
                        }
                    }
                    if(first){//第一行   GET  /xxx/xx.HTML  HTTP1.1   /
                        String[] infos = line.split(" ");
                        first = false;
                        if(infos!=null || infos.length>2){
                            reqPath = infos[1];//请求路径
                            if (infos[0].startsWith("GET"))
                                method = 1;
                            else if (infos[0].startsWith("POST"))
                                method = 2;
                            else if (infos[0].startsWith("HEAD"))
                                method = 3;
                            else
                                throw new RuntimeException("请求行解析失败:"+line);
                            msg += "请求方式：" + infos[0]+'\n';
                            msg += "请求路径：" + infos[1]+'\n';
                        }else{
                            throw new RuntimeException("请求行解析失败:"+line);
                        }
                    }
                    if(line.equals(""))//请求头读取到空行就结束
                        break;
                }
                if (method==2){
                    if ((!reqPath.equals("/"))&&!reqPath.equals("/login.html")){
                        response404(outputStream);
                    }
                    int a;
                    String tmptmp = "";
                    while((a = reader.read())!=-1){
                        tmptmp+=(char)a;
                        if (tmptmp.endsWith("&signUp="))break;
                    }
                    System.out.println(tmptmp);
                    responseLogin(outputStream,tmptmp,0);
                }
                if(method!=2&&!reqPath.equals("")) {
                    System.out.println("处理请求:http://localhost" + reqPath);
                    if(reqPath.equals("/")){
                        String ext = "html";
                        if (cookie_username!=null){
                            String tmptmp ="Username="+cookie_username+
                                    "&PhoneNumber="+cookie_phoneNumber+"&Signup=";
                            responseLogin(outputStream,tmptmp,Integer.parseInt(cookie_count));
                        }
                        else {
                            File file = new File("./KingsleySolar/login.html");
                            resposne200(outputStream, file.getAbsolutePath(), ext, method);
                        }
                    }else{
                        String ext = reqPath.substring(reqPath.lastIndexOf(".")+1);
                        File file = new File("./KingsleySolar"+reqPath);

                        if(file.exists() && file.isFile()){
                            resposne200(outputStream, file.getAbsolutePath(), ext, method);
                        }else{
                            response404(outputStream);
                        }
                    }
                }
                pool.submit(new Logger(msg));
            }

            catch (Exception e){
                e.printStackTrace();

            }finally {
                try{
                    if(inputStream!=null)
                        inputStream.close();
                    if(reader!=null)
                        reader.close();
                    if(outputStream!=null)
                        outputStream.close();
                }catch (IOException ex){
                    ex.printStackTrace();
                }
            }
        }
        private void response404(OutputStream out){
            System.out.println("返回404");
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(out);
                pw.println("HTTP/1.1 404");//输出响应行
                pw.println("Content-Type: text/html;charset=utf-8");
                pw.println();//表示响应头结束，开始响应内容
                pw.write("<h2>欢迎访问kingServer~</h2>");
                pw.write("<h2>你输入的路径有误哦~</h2>");
                pw.write("<h2>重新输入路径吧~</h2>");
                pw.flush();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try{
                    if(pw!=null)
                        pw.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
        private void responseLogin(OutputStream out,String data,int count){
            PrintWriter pw = null;
            try {
                Map<String,String> mapmap = new HashMap<>();
                String[] items = data.split("&");
                for(int i=0;i<items.length-1;i++){
                    System.out.println(items[i]);
                    String[] after = items[i].split("=");
                    System.out.println("after:");
                    System.out.println(after[0]);
                    System.out.println(after[1]);
                    mapmap.put(after[0],after[1]);
                }
                count++;
                pw = new PrintWriter(out);
                pw.println("HTTP/1.1 200 OK");//输出响应行
                pw.println("Content-Type: text/html;charset=utf-8");
                pw.println("Set-Cookie: Username="+mapmap.get("Username"));
                pw.println("Set-Cookie: PhoneNumber="+mapmap.get("PhoneNumber"));
                pw.println("Set-Cookie: Count="+count);
                pw.println();//表示响应头结束，开始响应内容
                pw.write("<h2> Hello "+mapmap.get("Username")+"</h2>");
                pw.write("<h2> Your phone number is "+mapmap.get("PhoneNumber")+"</h2>");
                pw.write("<h2> You have visited for "+count+" times~</h2>");
                pw.write("<h2> click here for Kingsley's Solay System</h2>");
                pw.write("<a href=\"./SolarSystemKing.html\"> click here for Kingsley's Solay System</a>");
                pw.flush();
                System.out.println("响应欢迎页面！");
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try{
                    if(pw!=null)
                        pw.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
        private void resposne200(OutputStream out,String filePath,String ext,int method){
            System.out.println("返回 200，method:"+method);
            System.out.println("-------------------------------");
            PrintWriter pw = new PrintWriter(out);
            InputStream in;
            try {
                if(ext.equals("jpg") || ext.equals("png")||ext.equals("gif")){
                    out.write("HTTP/1.1 200 OK\r\n".getBytes());//输出响应行
                    if(ext.equals("jpg"))
                        out.write("Content-Type: image/jpg\r\n".getBytes());
                    else  if(ext.equals("png"))
                        out.write("Content-Type: image/png\r\n".getBytes());
                    else if(ext.equals("gif"))
                        out.write("Content-Type: image/gif\r\n".getBytes());
                    out.write("\r\n".getBytes());//输出空行，表示响应头结束
                    if (method !=1)return;
                    System.out.println(filePath);
                    in = new FileInputStream(filePath);
                    int len;
                    byte [] buff = new byte[1024];
                    while((len = in.read(buff))!=-1){
                        out.write(buff,0,len);
                    }
                    out.flush();
                }else if(ext.equals("html") || ext.equals("js") || ext.equals("css") || ext.equals("json")){
                    pw.println("HTTP/1.1 200 OK");//输出响应行
                    if(ext.equals("html"))
                        pw.println("Content-Type: text/html;charset=utf-8");
                    else  if(ext.equals("js"))
                        pw.println("Content-Type: application/x-javascript");
                    else if(ext.equals("css"))
                        pw.println("Content-Type: text/css");
                    else if(ext.equals("json"))
                        pw.println("Content-Type: application/json;charset=utf-8");
                    //输出空行表示响应头结束
                    pw.println();
                    pw.flush();
                    if (method !=1)return;
                    BufferedReader fileReader = new BufferedReader( new InputStreamReader(new FileInputStream(filePath)));
                    //写出数据
                    String line;
                    while((line = fileReader.readLine())!=null){
                        pw.println(line);
                        pw.flush();
                    }
                }else{
                    response404(out);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


}

