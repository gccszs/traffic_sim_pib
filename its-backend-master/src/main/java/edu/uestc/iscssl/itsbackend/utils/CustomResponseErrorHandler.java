package edu.uestc.iscssl.itsbackend.utils;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class CustomResponseErrorHandler implements ResponseErrorHandler {
    private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {

// 队请求头的处理
        List<String> customHeader = response.getHeaders().get("x-app-err-id");

        String svcErrorMessageID = "";
        if (customHeader != null) {
            svcErrorMessageID = customHeader.get(0);
        }
//对body 的处理 (inputStream)
        String body = convertStreamToString(response.getBody());

        try {

            errorHandler.handleError(response);

        } catch (RestClientException scx) {

            try {
                throw new CustomException(scx.getMessage(), scx, body);
            } catch (CustomException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return errorHandler.hasError(response);
    }

    // inputStream 装换为 string
    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }
}
