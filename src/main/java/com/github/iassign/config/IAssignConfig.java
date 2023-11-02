package com.github.iassign.config;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
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

@Configuration
public class IAssignConfig {

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
}
