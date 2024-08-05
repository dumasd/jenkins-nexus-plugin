package io.jenkins.plugins.nexus.utils;

import java.io.IOException;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;

/**
 * @author Bruce.Wu
 * @date 2024-08-05
 */
public final class HttpUtils {

    private HttpUtils() {}

    public static CloseableHttpClient createClient() throws IOException {
        SSLContext sslCtx;
        try {
            sslCtx = SSLContexts.custom()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();
        } catch (Exception e) {
            throw new IOException(e);
        }
        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslCtx)
                .setHostnameVerifier(new NoopHostnameVerifier())
                .build();
        PoolingHttpClientConnectionManager connMgt = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();
        return HttpClientBuilder.create()
                .setConnectionManager(connMgt)
                .setRedirectStrategy(DefaultRedirectStrategy.INSTANCE)
                .build();
    }
}
