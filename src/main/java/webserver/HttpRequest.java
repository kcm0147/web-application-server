package webserver;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import util.HttpRequestUtils;
import util.IOUtils;

public class HttpRequest {

    private BufferedReader request;
    private Map<String,String> requestHeader;
    private Map<String,String> queryParameter;
    private String requestLine;
    private String pathUrl;

    public HttpRequest(){}

    public HttpRequest(InputStream in) throws IOException {
        request=new BufferedReader(
            new InputStreamReader(in, "UTF-8")
        );

        requestLine=request.readLine();
        setHeaderMap();
        setQueryMap();
        setPath();

    }

    public void setPath(){
        if(getMethod().equals("GET")) {
            String url = requestLine.split(" ")[1];
            int index = url.indexOf("?");
            this.pathUrl=url.substring(0, index);
        }
        else if(getMethod().equals("POST")){
            this.pathUrl=requestLine.split(" ")[1];
        }
    }

    public void setHeaderMap() throws IOException {
        String input;
        requestHeader=new HashMap<>();

        while(!(input=request.readLine()).equals("")){
            String[] s = input.split(": ");
            requestHeader.put(s[0],s[1]);
        }
    }

    public void setQueryMap() throws IOException {
        if(getMethod().equals("POST")){
            int length=Integer.parseInt(getHeader("Content-Length"));
            String body = IOUtils.readData(request,length);

            queryParameter= HttpRequestUtils.parseQueryString(body);

        }
        else if(getMethod().equals("GET")){
            String url = requestLine.split(" ")[1];
            int index=url.indexOf("?");
            String queryString=url.substring(index+1);

            queryParameter = HttpRequestUtils.parseQueryString(queryString);
        }
    }


    public String getMethod(){
        return requestLine.split(" ")[0];
    }

    public String getPath(){
        return this.pathUrl;

    }

    public String getHeader(String fieldName){
        return requestHeader.get(fieldName);
    }

    public String getParameter(String queryName){
        return queryParameter.get(queryName);
    }
}
