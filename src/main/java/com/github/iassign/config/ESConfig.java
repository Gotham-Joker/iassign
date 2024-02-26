/*
 * MIT License
 *
 * Copyright (c) 2024 Hongtao Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.iassign.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * ES Client配置
 */
@Configuration
public class ESConfig {

    /**
     * 为elasticsearch配置https
     * 需要提供证书路径参数：es.certificate.path
     *
     * @return
     */
    @Bean
    public RestClientBuilderCustomizer sslRestClientBuilderCustomizer(Environment env) {
        String esCertificatePath = env.getProperty("es.certificate.path", "");
        return new RestClientBuilderCustomizer() {
            @Override
            public void customize(RestClientBuilder builder) {
            }

            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                if (!StringUtils.hasText(esCertificatePath)) {
                    return;
                }
                CertificateFactory factory;
                try {
                    factory = CertificateFactory.getInstance("X.509");
                    Certificate trustedCa;
                    if (esCertificatePath.startsWith("classpath:")) {
                        try (InputStream is = getClass().getResourceAsStream(esCertificatePath.split(":")[1])) {
                            trustedCa = factory.generateCertificate(is);
                        }
                    } else {
                        Path caCertificatePath = Paths.get(esCertificatePath);
                        try (InputStream is = Files.newInputStream(caCertificatePath)) {
                            trustedCa = factory.generateCertificate(is);
                        }
                    }
                    KeyStore trustStore = KeyStore.getInstance("pkcs12");
                    trustStore.load(null, null);
                    trustStore.setCertificateEntry("ca", trustedCa);
                    SSLContextBuilder sslContextBuilder = SSLContexts.custom().setProtocol("TLSv1.2")
                            .loadTrustMaterial(trustStore, null);
                    // 设置SSL上下文，使用https传输
                    final SSLContext sslContext = sslContextBuilder.build();
                    builder.setSSLContext(sslContext);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClientBuilder restClientBuilder) {
        RestClient restClient = restClientBuilder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
