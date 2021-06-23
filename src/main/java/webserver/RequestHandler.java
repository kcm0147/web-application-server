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
                    response302Header(dos,"/index.html");
                }

            }
            else if(tokens[0].equals("GET")){
                body = Files.readAllBytes(new File("./webapp" + requestUrl).toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
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
