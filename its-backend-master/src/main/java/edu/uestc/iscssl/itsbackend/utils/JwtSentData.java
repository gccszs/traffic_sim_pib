package edu.uestc.iscssl.itsbackend.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;

public class JwtSentData {
    public static long chunkSize = 1048576L; //将文件切割成多少份
    //private static String loginUrl = "http://202.205.145.156:8017/sys/api/user/validate";
    //private static String uploadUrl = "http://202.205.145.156:8017/project/log/attachment/upload";
    //private static String serverURI="http://202.205.145.156:8017/project/log/upload";
    //private static String statusURI="http://202.205.145.156:8017/third/api/test/result/upload";
    private static String loginUrl = "http://www.ilab-x.com/sys/api/user/validate";//登陆接口url
    private static String uploadUrl = "http://www.ilab-x.com/project/log/attachment/upload";//文件上传接口，之后需要将地址改为正式地址“http://www.ilab-x.com”
    private static String serverURI="http://www.ilab-x.com/project/log/upload";//数据上传接口url
    private static String statusURI="http://www.ilab-x.com/third/api/test/result/upload";//状态上传接口url
    public static int sendReport(String reportId) throws IOException {
        //报告完成以后，将实验报告发送至实验空间
        RandomAccessFile raf = null;
        int attachmentId = 0;
        String file = "C:\\its\\pdfFolder\\" + reportId + ".pdf";
        raf = new RandomAccessFile(new File(file), "r");// 获取目标文件 预分配文件所占的空间 在磁盘中创建一个指定大小的文件  r 是只读
        long length = raf.length();//文件长度
        int count = (int) (Math.ceil((double) length / (double) chunkSize));//文件切片后的分片数
        //xjwt认证
        String json = "SYS";
        String xjwt = JWT.encrty(json);
        CookieStore store = new BasicCookieStore();// 当分片大于1时需要使用httpclient返回的cookie
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(store).build();
        CloseableHttpResponse response = null;
        String cookies = null;
        for (int i = 0; i < count; i++)   //最后一片单独处理
        {
            //设置发送内容
            long begin = i * chunkSize;
            long end = ((i + 1) * chunkSize > length) ? length : (i + 1) * chunkSize;
            //System.out.println(begin+"---"+end);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte [] buffer = new byte[1024];
            int realLength = 0;
            raf.seek(begin);//将文件流的位置移动到指定begin字节处
            while(raf.getFilePointer() <= end && (realLength = raf.read(buffer)) > 0)
            {
                byteArrayOutputStream.write(buffer, 0, realLength);
            }
            //POST 有参数，基本参数+对象参数
            String url = uploadUrl + "?chunkSize=" + chunkSize + "&totalChunks=" + count + "&current=" + i + "&filename=" + URLEncoder.encode(new File(file).getName(), "UTF-8") + "&xjwt=" + xjwt;
            HttpPost httpPost = new HttpPost(url);
            ByteArrayEntity entity = new ByteArrayEntity(byteArrayOutputStream.toByteArray());
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-type","application/octet-stream; charset=UTF-8");
            httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36");
            if(i>0)
                httpPost.setHeader("Set-Cookie", cookies);
            //获取返回信息
            response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            String resp = EntityUtils.toString(responseEntity);
            System.out.println(resp);
            // 获取返回的cookie
            if (statusCode == 200 && i == 0 && count > 1)
            {
                cookies = response.getLastHeader("Set-Cookie").getValue();
            }
            JSONObject jsonObject = JSONObject.parseObject(resp);
            String id = (String) jsonObject.get("id");
            if(id != null)
                attachmentId = Integer.parseInt(id);
            System.out.println(jsonObject);
        }
        return attachmentId;
    }
    public static int sendData(String json,int i) throws IOException {//i = 1时，发送实验状态；i = 2时，发送实验数据
        int id = 0;
        String xjwt= JWT.encrty(json);
        String url;
        if(i == 1)
            url = statusURI +"?xjwt=" +  xjwt ;
        else
            url = serverURI +"?xjwt=" +  xjwt ;
        try {
            String sr = sendPost(url,"");
            System.out.println("-------POST-------"+sr);
            //读取实验结果
            JSONObject retJson = JSONObject.parseObject(sr);
            int code = (int) retJson.get("code");
            if(code == 0)
                id = Integer.parseInt((String) retJson.get("id"));
            else
                id = code;
        } catch (Exception e) {
            e.printStackTrace();
        }
        String strJson=JWT.dencrty(xjwt);
        System.out.println(strJson);
        return id;
    }
    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            //conn.setRequestProperty("method", "POST");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();

            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String lines;
            StringBuffer sb = new StringBuffer("");
            while ((lines = in.readLine()) != null) {
                lines = new String(lines.getBytes(), "utf-8");
                sb.append(lines);
            }
            result = sb.toString();
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }
    public static String uploadFile(String serverURL,File file){
        String result ="";
        try {
            URL url = new URL(serverURL);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type","text/html");
            conn.setRequestProperty("Cache-Control","no-cache");
            conn.setRequestProperty("Charsert", "UTF-8");
            //conn.setRequestProperty("Set-Cookie",);
            conn.connect();
            conn.setConnectTimeout(10000);
            OutputStream out =conn.getOutputStream();

            DataInputStream in = new DataInputStream(new FileInputStream(file));

            int bytes = 0;
            byte[] buffer = new byte[1024];
            while ((bytes = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytes);
            }
            in.close();
            out.flush();
            out.close();
            // 得到响应码
            int res = conn.getResponseCode();
            StringBuilder sb2;
            if (res == 200){
                // 定义BufferedReader输入流来读取URL的响应
                BufferedReader in2 = new BufferedReader( new InputStreamReader(conn.getInputStream()));
                String lines;
                StringBuffer sb = new StringBuffer("");
                while ((lines = in2.readLine()) != null) {
                    lines = new String(lines.getBytes(), "utf-8");
                    sb.append(lines);
                }
                result = sb.toString();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }
    public static JSONObject sentLoginInfo(String userName,String password) throws NoSuchAlgorithmException {
        JWT jwt = new JWT();
        String nonce = JWT.getRandomChar(16);
        String cnonce = JWT.getRandomChar(16);
        password = nonce+jwt.encrypt(password).toUpperCase()+cnonce;
        password = jwt.encrypt(password).toUpperCase();
        String url = loginUrl+"?username="+userName+"&password="+password+"&nonce="+nonce+"&cnonce="+cnonce;
        String response = HttpClient.sendPostRequest(url);
        //String result = HttpClient.sendPostRequest("http://127.1.0.1:8080/receiveLogin",params);
        return JSONObject.parseObject(response);
    }
}