/**
 * The MIT License
 *
 * Original work sponsored and donated by National Board of e-Health (NSI), Denmark
 * (http://www.nsi.dk)
 *
 * Copyright (C) 2011 National Board of e-Health (NSI), Denmark (http://www.nsi.dk)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
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
 */
package com.trifork.batchcopy.client;

import dk.sosi.seal.SOSIFactory;
import dk.sosi.seal.model.*;
import dk.sosi.seal.model.constants.SubjectIdentifierTypeValues;
import dk.sosi.seal.pki.SOSIFederation;
import dk.sosi.seal.pki.SOSITestFederation;
import dk.sosi.seal.vault.CredentialPair;
import dk.sosi.seal.vault.CredentialVault;
import dk.sosi.seal.vault.GenericCredentialVault;
import dk.sosi.seal.xml.XmlUtil;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import static dk.sosi.seal.model.SignatureUtil.setupCryptoProviderForJVM;

@Component
public class SosiUtil {

    private static Logger logger = Logger.getLogger(SosiUtil.class);

    @Value("${sosi.sts.url}")
    private String stsUrl;

    @Value("${sosi.careprovider.name}")
    private String careproviderName;

    @Value("${sosi.careprovider.cvr}")
    private String careproviderCvr;

    @Value("${sosi.production.federation}")
    private Boolean useProductionFederation;

    @Value("${sosi.system.name}")
    private String sosiSystemName;

    @Value("${sosi.keystore.keyname}")
    private String keyStoreKeyName;

    @Value("${sosi.keystore.file}")
    private String keyFilePath;

    @Value("${sosi.keystore.password}")
    private String keyStorePassword;

    private final Properties props = setupCryptoProviderForJVM();
    private CredentialVault companyVault;

    private SOSIFactory factory;
    private DocumentBuilder builder;
    private IDCard idCard;

    @PostConstruct
    public void init() throws ParserConfigurationException, UnrecoverableKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        builder = docFactory.newDocumentBuilder();

        companyVault = getCompanyVault();
        if (useProductionFederation == null || useProductionFederation) {
            SOSIFederation federation = new SOSIFederation(props);
            factory = new SOSIFactory(federation, companyVault, props);
        } else {
            SOSITestFederation federation = new SOSITestFederation(props);
            factory = new SOSIFactory(federation, companyVault, props);
        }
    }

    private CredentialVault getCompanyVault() throws NoSuchAlgorithmException, CertificateException, IOException,
            KeyStoreException, UnrecoverableKeyException {
        CredentialVault result = new GenericCredentialVault(props, keyStorePassword);

        FileInputStream keyInputStream = new FileInputStream(keyFilePath);
        result.getKeyStore().load(keyInputStream, keyStorePassword.toCharArray());
        keyInputStream.close();

        X509Certificate certificate = (X509Certificate) result.getKeyStore().getCertificate(keyStoreKeyName);
        PrivateKey privateKey = (PrivateKey) result.getKeyStore().getKey(keyStoreKeyName, keyStorePassword.toCharArray());
        result.setSystemCredentialPair(new CredentialPair(certificate, privateKey));

        return result;
    }

    public IDCard getIdCard() throws IOException {
        if ((idCard != null) && (idCard.isValidInTime()))
            return idCard;

        CareProvider careProvider = new CareProvider(SubjectIdentifierTypeValues.CVR_NUMBER, careproviderCvr, careproviderName);
        IDCard requestCard = factory.createNewSystemIDCard(
                sosiSystemName,
                careProvider,
                AuthenticationLevel.VOCES_TRUSTED_SYSTEM,
                null,
                null,
                companyVault.getSystemCredentialPair().getCertificate(),
                null);
        if (requestCard == null) {
            throw new RuntimeException("Failed to create a new systemIDCard");
        }

        SecurityTokenRequest request = factory.createNewSecurityTokenRequest();
        request.setIDCard(requestCard);
        String xml = XmlUtil.node2String(request.serialize2DOMDocument(), false, true);
        String response = sendRequest(stsUrl, "", xml, true);
        SecurityTokenResponse securityTokenResponse = factory.deserializeSecurityTokenResponse(response);
        if (securityTokenResponse.isFault()) {
            logger.error("FaultActor: " + securityTokenResponse.getFaultActor());
            logger.error("FaultCode: " + securityTokenResponse.getFaultCode());
            logger.error("FaultString: " + securityTokenResponse.getFaultString());
            throw new RuntimeException("Security token response is faulty: " + securityTokenResponse.getFaultString());
        }
        idCard = securityTokenResponse.getIDCard();
        if (idCard == null) {
            throw new RuntimeException("The response from the STS did not contain an IDCard:\n" + response);
        }
        return idCard;
    }

    /**
     * Sends a request to a given url
     * @param url service url
     * @param docXml the data that should be sent
     * @param failOnError throw exception on error?
     * @return The reply from the service
     * @throws IOException
     */
    private String sendRequest(String url, String action, String docXml, boolean failOnError) throws IOException {
        HttpURLConnection uc = null;
        OutputStream os = null;
        InputStream is = null;
        try {
            URL u = new URL(url);
            uc = (HttpURLConnection) u.openConnection();
            uc.setDoOutput(true);
            uc.setDoInput(true);
            uc.setRequestMethod("POST");
            uc.setRequestProperty("SOAPAction", "\"" + action + "\"");
            uc.setRequestProperty("Content-Type", "text/xml; encoding=utf-8");
            os = uc.getOutputStream();
            IOUtils.write(docXml, os, "UTF-8");
            os.flush();
            if (uc.getResponseCode() != 200) {
                is = uc.getErrorStream();
            } else {
                is = uc.getInputStream();
            }
            String res = IOUtils.toString(is, "UTF-8");
            if (uc.getResponseCode() != 200 && (uc.getResponseCode() != 500 || failOnError)) {
                throw new RuntimeException("Got unexpected response " + uc.getResponseCode() +" from " + url);
            }
            return res;
        } finally {
            if (os != null) IOUtils.closeQuietly(os);
            if (is != null) IOUtils.closeQuietly(is);
            if (uc != null) uc.disconnect();
        }
    }

    private Node string2Node(String xmlstr) throws SAXException, IOException {
        InputSource inStream = new InputSource();
        inStream.setCharacterStream(new StringReader(xmlstr));

        Document docBody = builder.parse(inStream);
        return docBody.getFirstChild();
    }

    /**
     * Send a service request
     * @param url service url
     * @param strSoapBodyRequest the body of the request that should be sent (everything else is added automatically)
     * @return The service repl
     * @throws IOException
     */
    public Reply sendServiceRequest(String url, String strSoapBodyRequest) throws IOException, SAXException {
        IDCard card = getIdCard();

        Element bodyElem = (Element) string2Node(strSoapBodyRequest);

        Request req = factory.createNewRequest(false, null);
        req.setIDCard(card);
        req.setBody(bodyElem);
        Document dom = req.serialize2DOMDocument();
        String strRequest = XmlUtil.node2String(dom, false, true);

        String strReply = sendRequest(url, "replicate", strRequest, false);
        return factory.deserializeReply(strReply);
    }

}
