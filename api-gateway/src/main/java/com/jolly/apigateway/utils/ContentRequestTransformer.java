package com.jolly.apigateway.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

public class ContentRequestTransformer extends ProxyRequestTransformer {

    @Override
    public RequestBuilder transform(HttpServletRequest request) throws HttpRequestMethodNotSupportedException, URISyntaxException, IOException {
        RequestBuilder requestBuilder = predecessor.transform(request);

        String requestContent = request.getReader().lines().collect(Collectors.joining(""));
        if (!requestContent.isEmpty()) {
            StringEntity entity = new StringEntity(requestContent, ContentType.APPLICATION_JSON);
            requestBuilder.setEntity(entity);
        }

        return requestBuilder;
    }
}
