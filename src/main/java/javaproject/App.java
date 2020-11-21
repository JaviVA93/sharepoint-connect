package javaproject;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;

/**
 * Hello world!
 *
 */
public class App {
    public static String digest_url = "https://gruposifu.sharepoint.com/_api/contextinfo";
    public static String kpmg_sharepoint_url = "gruposifu.sharepoint.com";

    public static void main(String[] args) {
        try {
            String request_security_token_body = requestSecurityToken();
            if (request_security_token_body != "") {
                String security_token_value;
                security_token_value = getVariableValueFromXML(request_security_token_body, "wsse:BinarySecurityToken");
                if (security_token_value != "") {
                    System.out.print("\n\nSECURITY TOKEN\n" + security_token_value);
                    ArrayList<String> access_token_cookies = requestAccessTokenCookies(security_token_value);
                    if (access_token_cookies.size() == 2) {
                        String digest_value = getDigest(access_token_cookies);
                        System.out.println("\n\nDigest value: " + digest_value);
                    } else {
                        System.out.print("\n\nERROR GETTING THE ACCESS COOKIES");
                    }
                } else {
                    System.out.print("\n\nERROR GETTING THE SECURITY TOKEN");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String requestSecurityToken() throws ClientProtocolException, IOException {

        String user_email = "kpmg@quercus.com.es";
        String user_pass = "#gQUSEg2045Y+i";
        String endpoint_url = "https://gruposifu.sharepoint.com/sites/test/gestioncomercial/";
        String xml_data = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"\r\n      xmlns:a=\"http://www.w3.org/2005/08/addressing\"\r\n      xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\r\n    <s:Header>\r\n        <a:Action s:mustUnderstand=\"1\">http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue</a:Action>\r\n        <a:ReplyTo>\r\n            <a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address>\r\n        </a:ReplyTo>\r\n        <a:To s:mustUnderstand=\"1\">https://login.microsoftonline.com/extSTS.srf</a:To>\r\n        <o:Security s:mustUnderstand=\"1\"\r\n       xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\r\n            <o:UsernameToken>\r\n                <o:Username>"
                + user_email + "</o:Username>\r\n                <o:Password>" + user_pass
                + "</o:Password>\r\n            </o:UsernameToken>\r\n        </o:Security>\r\n    </s:Header>\r\n    <s:Body>\r\n        <t:RequestSecurityToken xmlns:t=\"http://schemas.xmlsoap.org/ws/2005/02/trust\">\r\n            <wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\r\n                <a:EndpointReference>\r\n                    <a:Address>"
                + endpoint_url
                + "</a:Address>\r\n                </a:EndpointReference>\r\n            </wsp:AppliesTo>\r\n            <t:KeyType>http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey</t:KeyType>\r\n            <t:RequestType>http://schemas.xmlsoap.org/ws/2005/02/trust/Issue</t:RequestType>\r\n            <t:TokenType>urn:oasis:names:tc:SAML:1.0:assertion</t:TokenType>\r\n        </t:RequestSecurityToken>\r\n    </s:Body>\r\n</s:Envelope>";

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("https://login.microsoftonline.com/extSTS.srf");
        StringEntity xmlData = new StringEntity(xml_data, ContentType.create("application/xml"));
        httppost.setEntity(xmlData);
        try {
            CloseableHttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String result = EntityUtils.toString(entity);
                return result;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String getVariableValueFromXML(String xml_data, String element_name) {
        try {
            DocumentBuilder builder;
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(xml_data));

            Document doc;
            doc = builder.parse(src);
            String s_token = doc.getElementsByTagName(element_name).item(0).getTextContent();

            return s_token;
        } catch (Exception e) {
            return "";
        }
    }

    public static ArrayList<String> requestAccessTokenCookies(String security_token)
            throws ParseException, IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("https://gruposifu.sharepoint.com/_forms/default.aspx?wa=wsignin1.0");
        StringEntity request_body = new StringEntity(security_token,
                ContentType.create("application/x-www-form-urlencoded"));
        httppost.setEntity(request_body);
        ArrayList<String> access_cookies = new ArrayList<String>();
        try {
            CloseableHttpResponse response = httpclient.execute(httppost);
            Header[] response_header = response.getHeaders("Set-Cookie");
            for (Header cookie : response_header) {
                String cookie_value = cookie.getValue();
                if (cookie_value.startsWith("rtFa=") || cookie_value.startsWith("FedAuth=")) {
                    access_cookies.add(cookie_value.split(";")[0]);
                }
            }
            return access_cookies;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return access_cookies;
        }
    }

    public static String getDigest(ArrayList<String> access_cookies) throws ClientProtocolException, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CookieStore cookie_store = new BasicCookieStore();
        for (String cookie : access_cookies) {
            String[] splitted_cookie = cookie.split("=", 2);
            System.out.print("\n\nCookie name: " + splitted_cookie[0]);
            System.out.print("\nCookie value: " + splitted_cookie[1]);
            BasicClientCookie client_cookie = new BasicClientCookie(splitted_cookie[0], splitted_cookie[1]);
            client_cookie.setDomain(kpmg_sharepoint_url);
            client_cookie.setPath("/");
            cookie_store.addCookie(client_cookie);
        }
        HttpContext local_context = new BasicHttpContext();
        HttpPost httppost = new HttpPost(digest_url);
        local_context.setAttribute(HttpClientContext.COOKIE_STORE, cookie_store);
        CloseableHttpResponse response = httpclient.execute(httppost, local_context);
        HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(entity);
                String digest_value = getVariableValueFromXML(result, "d:FormDigestValue");
                return digest_value;
            }
        return "";
    }
}
