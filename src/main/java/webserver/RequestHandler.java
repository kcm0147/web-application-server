package webserver;

import ch.qos.logback.core.joran.util.StringToObjectConverter;
import db.DataBase;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.

            DataOutputStream dos = new DataOutputStream(out);
            byte[] body;
            body="Hello world".getBytes();

            InputStreamReader isr = new InputStreamReader(in, "UTF-8");
            BufferedReader br = new BufferedReader(isr);

            String input=br.readLine();
            String[] tokens = input.split(" ");
            if(input==null) return;

            String requestUrl=tokens[1];

            if(tokens[0].equals("POST")){
                while(!input.contains("Content-Length")){
                    input=br.readLine();
                }
                String[] tempTokens=input.split(": ");
                int length = Integer.parseInt(tempTokens[1]);
                log.debug("length : {}",length);

                while(!input.equals("")){
                    input=br.readLine();
                }

                if(requestUrl.startsWith("/user/create")) {
                    String query= IOUtils.readData(br,length);
                    Map<String, String> stringMap = HttpRequestUtils.parseQueryString(query);

                    User user = new User(stringMap.get("userId"), stringMap.get("password"),
                        stringMap.get("name"), stringMap.get("email"));

                    log.debug("user : {}",user);
                    DataBase.addUser(user);

                    response302Header(dos,"/index.html");
                }
                else if(requestUrl.startsWith("/user/login")){
                    String query = IOUtils.readData(br,length);
                    Map<String, String> stringMap = HttpRequestUtils.parseQueryString(query);
                    User user= DataBase.findUserById(stringMap.get("userId"));

                    if(user==null || !user.getPassword().equals(stringMap.get("password"))){
                        log.debug("login fail");
                        response302HeaderWithCookie(dos,"/user/login_failed.html",false);
                    }
                    else {
                        log.debug("login success!");
                        response302HeaderWithCookie(dos, "/index.html", true);
                    }
                }

            }
            else if(tokens[0].equals("GET")){

                if(requestUrl.startsWith("/user/list")){

                    String[] tempTokens;

                    while(true){
                        String head=br.readLine();
                        tempTokens=head.split(": ");
                        if(tempTokens[0].equals("Cookie")){
                            break;
                        }
                    }
                    Map<String, String> parameters = HttpRequestUtils.parseCookies(tempTokens[1]);

                    if(!Boolean.parseBoolean(parameters.get("logined"))){
                        response302HeaderWithCookie(dos,"/index.html",false);
                        return;
                    }
                    else{ // make File
                        Collection<User> users = DataBase.findAll();
                        StringBuilder sb = new StringBuilder();
                        sb.append("<table border='1'>");
                        for (User user : users) {
                            sb.append("<tr>");
                            sb.append("<td>" + user.getUserId() + "</td>");
                            sb.append("<td>" + user.getName() + "</td>");
                            sb.append("<td>" + user.getEmail() + "</td>");
                            sb.append("</tr>");
                        }
                        sb.append("</table>");
                        body = sb.toString().getBytes();
                        dos = new DataOutputStream(out);
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    }

                }


                responseResource(out,requestUrl);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    private void responseResource(OutputStream out, String url) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
        response200Header(dos, body.length);
        responseBody(dos, body);
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderWithCookie(DataOutputStream dos, String redirectUrl,boolean cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: "+ redirectUrl+"\r\n");
            dos.writeBytes("Set-Cookie: logined="+ cookie+"\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String redirectUrl) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: "+ redirectUrl+"\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
