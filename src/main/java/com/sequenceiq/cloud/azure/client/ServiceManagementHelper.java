package com.sequenceiq.cloud.azure.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class ServiceManagementHelper {

    private static KeyStore getKeyStore(String keyStoreName, String password) throws IOException {
        KeyStore ks = null;
        FileInputStream fis = null;
        try {
            ks = KeyStore.getInstance("JKS");
            char[] passwordArray = password.toCharArray();
            fis = new java.io.FileInputStream(keyStoreName);
            ks.load(fis, passwordArray);
            fis.close();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return ks;
    }

    private static SSLSocketFactory getSSLSocketFactory(String keyStoreName, String password) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, IOException {
        KeyStore ks = getKeyStore(keyStoreName, password);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(ks, password.toCharArray());

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        return context.getSocketFactory();
    }

    private static String processGetRequest(URL url, String keyStore, String keyStorePassword) throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, IOException {
        SSLSocketFactory sslFactory = getSSLSocketFactory(keyStore, keyStorePassword);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(sslFactory);
        con.setRequestMethod("GET");
        con.addRequestProperty("x-ms-version", "2014-04-01");
        con.addRequestProperty("ContentType", "application/json");
        InputStream responseStream = (InputStream) con.getContent();
        String response = getStringFromInputStream(responseStream);
        responseStream.close();
        return response;
    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {


        String subscriptionId = "id";
        String keyStorePath = "//Users//sample//prj//WindowsAzureKeyStore.jks";
        String keyStorePassword = "test123";
        String url = "";

        //List locations
        url = String.format("https://management.core.windows.net/%s/locations", subscriptionId);
        String response = processGetRequest(new URL(url), keyStorePath, keyStorePassword);
        System.out.println(response);


    }
}
