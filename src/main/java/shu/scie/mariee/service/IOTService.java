package shu.scie.mariee.service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;



import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.*;


public class IOTService {

    private final HttpClient httpClient;

    private final String username;

    private final String password;

    private final String encodedAuthString;

    public IOTService(String username, String password) {
        this.httpClient = UtilService.createHttpClient();
        this.username = username;
        this.password = password;
        String authString = username + ":" + password;;
        this.encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
    }

    public Float readIOTReal(String url){

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", STR."Basic \{this.encodedAuthString}")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String OutVal=resloveXML(response.body(),"out");
            return Float.parseFloat(Objects.requireNonNull(OutVal));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Boolean readIOTBool(String url){

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", STR."Basic \{this.encodedAuthString}")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String OutVal=resloveXML(response.body(),"out");
            return Boolean.valueOf(Objects.requireNonNull(OutVal));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeIOTReal(String url , float val){
        String Poststr = getPostString(String.valueOf(val),"real");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", STR."Basic \{this.encodedAuthString}")
                .POST(HttpRequest.BodyPublishers.ofString(Poststr))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String OutVal=resloveXML(response.body(),"err");
            if (OutVal == "err")
                return;

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void writeIOTBool(String url , boolean val){
        String Poststr = getPostString(String.valueOf(val),"real");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", STR."Basic \{this.encodedAuthString}")
                .POST(HttpRequest.BodyPublishers.ofString(Poststr))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String OutVal=resloveXML(response.body(),"err");
            if (OutVal == "err")
                return;

        } catch (Exception e) {
            e.printStackTrace();

        }
    }


//    public static void main(String[] args) {
//        String username = "obix";
//        String password = "Obix123456";
//        String authString = username + ":" + password;
//        String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
//        //System.out.println(Boolean.valueOf("t"));
//
//        try {
//
//            String url = "https://172.16.10.220/obix/config/Drivers/BacnetNetwork/AHU_ICU_4/points/ALM2";
//
//            HttpClient httpClient = createHttpClient();
//
//            String Poststr = getPostString("False","bool");
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .header("Authorization", "Basic " + encodedAuthString)
//                    //.GET()
//                    .POST(HttpRequest.BodyPublishers.ofString(Poststr))
//                    .build();
//
//            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
//
//            System.out.println(response.statusCode());
//            System.out.println("//");
//            System.out.println(response.body());
//            String OutVal=resloveXML(response.body(),"out");
//            System.out.println(OutVal);
//
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("err");
//        }
//
//    }

//    public static HttpClient createHttpClient() {
//        TrustManager[] noopTrustManager = new TrustManager[]{
//                new X509TrustManager() {
//                    public void checkClientTrusted(X509Certificate[] xcs, String string) {
//                    }
//
//                    public void checkServerTrusted(X509Certificate[] xcs, String string) {
//                    }
//
//                    public X509Certificate[] getAcceptedIssuers() {
//                        return null;
//                    }
//                }
//        };
//
//
//        final Properties props = System.getProperties();
//        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
//
//
//        SSLContext sslContext;
//        try {
//            sslContext = SSLContext.getInstance("SSL");
//            sslContext.init(null, noopTrustManager, null);
//        } catch (NoSuchAlgorithmException | KeyManagementException e) {
//            return null;
//        }
//
//        SSLParameters sslParams = new SSLParameters();
//        sslParams.setEndpointIdentificationAlgorithm("");
//
//
//
//        return HttpClient.newBuilder()
//                .sslContext(sslContext)
//                .sslParameters(sslParams)
//                .version(HttpClient.Version.HTTP_1_1)
//                .followRedirects(HttpClient.Redirect.NORMAL)
//                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_NONE))
//                .connectTimeout(Duration.ofSeconds(99))
//                .build();
//
//    }

    private static String resloveXML(String resourse, String name) throws JDOMException, IOException {
        //1.创建SAXBuilder对象
        SAXBuilder saxBuilder = new SAXBuilder();
        //2.创建输入流
        //InputStream is = new FileInputStream(new File("src/main/resources/demo.xml"));
        ByteArrayInputStream tInputStringStream = new ByteArrayInputStream(resourse.getBytes());
        //3.将输入流加载到build中
        Document document = saxBuilder.build(tInputStringStream);
        //4.获取根节点
        Element rootElement = document.getRootElement();
        System.out.println("根节点:"+rootElement.getName()+":"+rootElement.getValue());
        List<Attribute> rootattributes = rootElement.getAttributes();
        if (rootElement.getName()=="err"){
            return "err";
        }
        for (Attribute attr : rootattributes) {
            //System.out.println(attr.getName()+":"+attr.getValue());
        }

        //5.获取子节点
        List<Element> children = rootElement.getChildren();
        for (Element child : children) {
            System.out.println("通过rollno获取属性值:"+child.getAttribute("rollno"));
            List<Attribute> attributes = child.getAttributes();

            //打印属性
            for (Attribute attr : attributes) {
                System.out.println(attr.getName()+":"+attr.getValue());
                if (attr.getName() == "name"){
                    if (attr.getValue().equals(name)) {
                        for (Attribute a : attributes) {
                            if (a.getName() == "val") {
                                return a.getValue();
                            }
                        }
                    }
                }
            }
            List<Element> childrenList = child.getChildren();
            System.out.println("======获取子节点-start======");
            for (Element o : childrenList) {
                if (o.getName() == "name"){
                    if (o.getValue().equals(name)) {
                        for (Element a : childrenList) {
                            if (a.getName() == "val") {
                                return a.getValue();
                            }
                        }
                    }
                }
                System.out.println("节点名:"+o.getName()+"---"+"节点值:"+o.getValue());
            }
            System.out.println("======获取子节点-end======");
        }
        return null;
    }

    private static String getPostString(String Value, String Type)
    {
        if(Type=="real"){
            return String.format("<real val=\"%s\"/>", Value );
        }
        else {
            return String.format("<bool val=\"%s\"/>", Value );
        }
    }
}