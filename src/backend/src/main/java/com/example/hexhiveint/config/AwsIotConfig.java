package com.example.hexhiveint.config;

import com.example.hexhiveint.model.SensorData;
import com.example.hexhiveint.repository.SensorDataRepository;
import com.example.hexhiveint.service.SensorDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/**
 * Configuration bean for AWS IoT Core MQTT connectivity.
 *
 * <p>Establishes a mutual-TLS MQTT connection to AWS IoT Core using
 * X.509 certificates. Subscribes to the configured sensor topic and
 * routes incoming telemetry through the {@link SensorDataService}
 * decision engine before persisting to the database.</p>
 *
 * @see SensorDataService
 * @see SensorDataRepository
 */
@Configuration
public class AwsIotConfig {

    private static final Logger log = LoggerFactory.getLogger(AwsIotConfig.class);

    @Value("${aws.iot.endpoint}")
    private String endpoint;

    @Value("${aws.iot.clientId}")
    private String clientId;

    @Value("${aws.iot.topic}")
    private String topic;

    @Value("${aws.iot.rootCaPath}")
    private String rootCaPath;

    @Value("${aws.iot.certPath}")
    private String certPath;

    @Value("${aws.iot.keyPath}")
    private String keyPath;

    @Autowired
    private SensorDataRepository repository;

    @Autowired
    private SensorDataService service;

    @Autowired
    private ResourceLoader resourceLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates and configures the MQTT client bean.
     *
     * <p>Connects to AWS IoT Core over TLS 1.2, subscribes to the sensor topic,
     * and registers a callback that deserialises incoming JSON payloads into
     * {@link SensorData} entities, applies the decision engine, and persists them.</p>
     *
     * @return the connected {@link MqttClient}, or {@code null} if connection fails
     */
    @Bean
    public MqttClient mqttClient() {
        try {
            String brokerUrl = "ssl://" + endpoint + ":8883";
            MqttClient client = new MqttClient(brokerUrl, clientId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setSocketFactory(getSocketFactory());

            client.connect(options);
            log.info("Connected to AWS IoT Core via MQTT at {}", endpoint);

            client.subscribe(topic, (t, msg) -> {
                try {
                    String payload = new String(msg.getPayload());
                    log.debug("MQTT payload received on topic [{}]: {}", t, payload);

                    SensorData data = objectMapper.readValue(payload, SensorData.class);
                    data.setId(null);
                    data.setTimestamp(System.currentTimeMillis());

                    service.processSensorData(data);
                    repository.save(data);
                    log.debug("Sensor data persisted with severity={}", data.getSeverity());
                } catch (Exception e) {
                    log.error("Failed to process MQTT message: {}", e.getMessage(), e);
                }
            });

            return client;
        } catch (Exception e) {
            log.error("Could not connect to AWS IoT Core: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Builds a TLS 1.2 {@link SSLSocketFactory} using the configured X.509 certificates.
     *
     * <p>Uses BouncyCastle to parse PEM-encoded private keys and constructs
     * a keystore containing the Root CA, client certificate, and private key.</p>
     *
     * @return the configured SSL socket factory
     * @throws Exception if certificate loading or SSL context initialisation fails
     */
    private SSLSocketFactory getSocketFactory() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Resource caRes = resourceLoader.getResource(rootCaPath);
        InputStream caInput = caRes.getInputStream();
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInput);

        Resource crtRes = resourceLoader.getResource(certPath);
        InputStream crtInput = crtRes.getInputStream();
        X509Certificate clientCert = (X509Certificate) cf.generateCertificate(crtInput);

        Resource keyRes = resourceLoader.getResource(keyPath);
        InputStream keyInput = keyRes.getInputStream();

        PEMParser pemParser = new PEMParser(new InputStreamReader(keyInput));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

        PrivateKey privateKey;

        if (object instanceof PEMKeyPair) {
            KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
            privateKey = kp.getPrivate();
        } else if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
            privateKey = converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
        } else {
            throw new IllegalArgumentException("Unsupported key format: " + object.getClass().getName());
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca-cert", caCert);
        keyStore.setCertificateEntry("certificate", clientCert);
        keyStore.setKeyEntry("private-key", privateKey, "password".toCharArray(),
                new java.security.cert.Certificate[]{clientCert});

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "password".toCharArray());

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        return context.getSocketFactory();
    }
}
