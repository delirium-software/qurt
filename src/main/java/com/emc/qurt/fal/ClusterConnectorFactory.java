package com.emc.qurt.fal;

import com.emc.qurt.domain.SystemSettings;
import org.apache.tomcat.util.codec.binary.Base64;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Created by morand3 on 1/13/2015.
 */
public class ClusterConnectorFactory {
    private static int port = 7225;

    static {
        disableSslVerification();
    }

    public static Cluster41Connector getConnector(SystemSettings settings) {
        RestAdapter restAdapter = new RestAdapter.Builder()
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint("https://" + settings.getSystemIp() + ":" + port + "/")
                .setConverter(new JacksonConverter())
                .setRequestInterceptor(buildInterceptor(settings))
                .build();

        Cluster41Connector service = restAdapter.create(Cluster41Connector.class);
        return service;
    }

    private static RequestInterceptor buildInterceptor(SystemSettings settings) {
        byte[] authEncBytes = Base64.encodeBase64(String.format("%s:%s", settings.getUser(), settings.getRealPassword()).getBytes());
        String authStringEnc = new String(authEncBytes);

        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Authorization", "Basic " + authStringEnc);
            }
        };
        return requestInterceptor;
    }

    private static void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }
}