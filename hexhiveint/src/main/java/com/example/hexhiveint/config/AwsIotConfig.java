package com.example.hexhiveint.config;

import com.example.hexhiveint.model.SensorData;
import com.example.hexhiveint.repository.SensorDataRepository;
import com.example.hexhiveint.service.SensorDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.KeyPair;
import java.security.PrivateKey;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.SecureRandom;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

@Configuration
public class AwsIotConfig {

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

    @Bean
    public MqttClient mqttClient() {
        try {
            String brokerUrl = "ssl://" + endpoint + ":8883";
            MqttClient client = new MqttClient(brokerUrl, clientId);
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setSocketFactory(getSocketFactory());

            client.connect(options);
            System.out.println("CONNECTED TO AWS IOT CORE via MQTT!");

            client.subscribe(topic, (t, msg) -> {
                try {
                    String payload = new String(msg.getPayload());
                    System.out.println("MQTT Received: " + payload);
                    
                    SensorData data = objectMapper.readValue(payload, SensorData.class);
                    data.setId(null);
                    data.setTimestamp(System.currentTimeMillis());
                    
                    service.processSensorData(data);
                    repository.save(data);
                    System.out.println("SAVED PERISTED DATA TO DB");
                } catch (Exception e) {
                    System.err.println("Failed to process MQTT message: " + e.getMessage());
                }
            });

            return client;
        } catch (Exception e) {
            System.err.println("COULD NOT CONNECT TO AWS IOT: " + e.getMessage());
            // e.printStackTrace(); 
            return null;
        }
    }

    private SSLSocketFactory getSocketFactory() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Load Root CA
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Resource caRes = resourceLoader.getResource(rootCaPath);
        InputStream caInput = caRes.getInputStream();
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInput);

        // Load Client Cert
        Resource crtRes = resourceLoader.getResource(certPath);
        InputStream crtInput = crtRes.getInputStream();
        X509Certificate clientCert = (X509Certificate) cf.generateCertificate(crtInput);

        // Load Private Key using Bouncy Castle
        Resource keyRes = resourceLoader.getResource(keyPath);
        InputStream keyInput = keyRes.getInputStream();
        
        PEMParser pemParser = new PEMParser(new InputStreamReader(keyInput));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        
        PrivateKey privateKey = null;
        
        if (object instanceof PEMKeyPair) {
            KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
            privateKey = kp.getPrivate();
        } else if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
             privateKey = converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
        } else {
            throw new Exception("Unknown Key Format: " + object.getClass().getName());
        }

        // Create Keystore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca-cert", caCert);
        keyStore.setCertificateEntry("certificate", clientCert);
        keyStore.setKeyEntry("private-key", privateKey, "password".toCharArray(), new java.security.cert.Certificate[]{clientCert});

        // Create SSL Context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "password".toCharArray());

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        return context.getSocketFactory();
    }
}

