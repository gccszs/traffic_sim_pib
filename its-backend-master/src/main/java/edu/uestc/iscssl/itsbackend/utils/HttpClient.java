package edu.uestc.iscssl.itsbackend.utils;

import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @description:http发送工具类
 */
public class HttpClient {
    /**
     * 向目的URL发送post请求
     * @param url       目的url
     * @param params    发送的参数
     * @return  JsonTokenData
     */
    public static String sendPostRequest(String url, MultiValueMap<String, String> params){
        RestTemplate client = new RestTemplate();
        //新建Http头，add方法可以添加参数
        HttpHeaders headers = new HttpHeaders();
        //设置请求发送方式
        HttpMethod method = HttpMethod.POST;
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //将请求头部和参数合成一个请求
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
        //执行HTTP请求，将返回的结构使用String 类格式化（可设置为对应返回值格式的类）
        ResponseEntity<String> response = client.exchange(url, method, requestEntity,String.class);
        //ResponseEntity<String> response = client.postForEntity(url,requestEntity,String.class);
        return response.getBody();
    }
    /**
     * 向目的URL发送get请求
     * @param url       目的url
     * @param params    发送的参数
     * @param headers   发送的http头，可在外部设置好参数后传入
     * @return  String
     */
    public static String sendGetRequest(String url, MultiValueMap<String, String> params,HttpHeaders headers){
        RestTemplate client = new RestTemplate();
        HttpMethod method = HttpMethod.GET;
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //将请求头部和参数合成一个请求
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
        //执行HTTP请求，将返回的结构使用String 类格式化
        ResponseEntity<String> response = client.exchange(url, method, requestEntity, String.class);
        return response.getBody();
    }
    /**
     * 向目的URL发送post请求,与上面方法不同的是，这里将参数直接附在url上，而不是以表单形式提交
     * @param url       目的url
     * @return  JsonTokenData
     */
    public static String sendPostRequest(String url){
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = HttpMethod.POST;
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //将请求头部和参数合成一个请求
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(null, headers);
        //执行HTTP请求，将返回的结构使用String 类格式化（可设置为对应返回值格式的类）
        ResponseEntity<String> response = client.exchange(url, method, requestEntity,String.class);
        return response.getBody();
    }
    /**
     * 向目的URL发送post请求
     * @param url       目的url带参数
     * @param out    发送的文件
     * @return  JsonTokenData
     */
    public static String sendPostRequest2(String url, byte[] out ) {
        //url = "http://www.ilab-x.com/project/log/attachment/upload?totalChunks=2&current=0&filename=%E5%AE%9E%E9%AA%8C%E6%8A%A5%E5%91%8A.pdf&chunkSize=1048576&xjwt=f%2F%2F%2F%2F%2F%2F%2F%2F%2F8C.MahFRppxqbptG46O8IBoCQ%3D%3D.YwMeROmxRvEAhUEPc7TpDdG9DYWu0mr9VaLfamdGOEo%3D";
        RestTemplate client = new RestTemplate();
        //新建Http头，add方法可以添加参数
        HttpHeaders headers = new HttpHeaders();
        //设置请求发送方式
        HttpMethod method = HttpMethod.POST;
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //将请求头部和参数合成一个请求
        HttpEntity<MultiValueMap<String,String>> requestEntity = new HttpEntity(out,headers);
        //执行HTTP请求，将返回的结构使用String 类格式化（可设置为对应返回值格式的类）
        client.setErrorHandler(new CustomResponseErrorHandler());
        ResponseEntity<String> response = client.exchange(url, method, null, String.class);
        //String response = client.postForObject(url,null,String.class);
        return response.getBody();
    }
    public static String sendPostRequest3(String url, MultiValueMap<String, Object> params){
        RestTemplate client = new RestTemplate();
        //新建Http头，add方法可以添加参数
        HttpHeaders headers = new HttpHeaders();
        //设置请求发送方式
        HttpMethod method = HttpMethod.POST;
        // 以表单的方式提交
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //将请求头部和参数合成一个请求
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(params, headers);
        //执行HTTP请求，将返回的结构使用String 类格式化（可设置为对应返回值格式的类）
        ResponseEntity<String> response = client.exchange(url, method, requestEntity,String.class);
        //ResponseEntity<String> response = client.postForEntity(url,requestEntity,String.class);
        return response.getBody();
    }
}

