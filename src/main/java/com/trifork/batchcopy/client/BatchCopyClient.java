/**
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Contributor(s): Contributors are attributed in the source code
 * where applicable.
 *
 * The Original Code is "Stamdata".
 *
 * The Initial Developer of the Original Code is Trifork Public A/S.
 *
 * Portions created for the Original Code are Copyright 2011,
 * Lægemiddelstyrelsen. All Rights Reserved.
 *
 * Portions created for the FMKi Project are Copyright 2011,
 * National Board of e-Health (NSI). All Rights Reserved.
 */
package com.trifork.batchcopy.client;

import com.trifork.batchcopy.jaxws.generated.ReplicationRequestType;
import com.trifork.batchcopy.jaxws.generated.ReplicationResponseType;
import dk.sosi.seal.model.Reply;
import dk.sosi.seal.xml.XmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringWriter;

@Component
public class BatchCopyClient {

    @Autowired
    private SosiUtil sosiUtil;

    @Value("${batchcopy.url}")
    private String endpointUrl;

    private JAXBContext jaxContext;

    public BatchCopyClient() throws JAXBException {
        jaxContext = JAXBContext.newInstance(ReplicationRequestType.class, ReplicationResponseType.class);
    }

    private final QName serviceName = new QName("http://nsi.dk/2011/10/21/StamdataKrs/", "ReplicationRequest");

    public void performRequest(String register, String dataType, String offsetToken)
            throws JAXBException, IOException, SAXException, ParserConfigurationException {
        ReplicationRequestType request = new ReplicationRequestType();
        if (offsetToken != null) {
            request.setOffset(offsetToken);
        } else {
            request.setOffset("00000000000000000000");
        }
        request.setVersion(1L);
        request.setDatatype(dataType);
        request.setRegister(register);
        request.setMaxRecords(1L);

        String requestString = createRequestString(request);
        Reply reply = sosiUtil.sendServiceRequest(endpointUrl, requestString);

        Element body = reply.getBody();

        Unmarshaller unmarshaller = jaxContext.createUnmarshaller();
        JAXBElement<ReplicationResponseType> jaxbResponse = unmarshaller.unmarshal(body, ReplicationResponseType.class);
        ReplicationResponseType response = jaxbResponse.getValue();
        printResponse(response);
    }

    private void printResponse(ReplicationResponseType response) {
        System.out.println("**********************************");
        System.out.println("* " + response.getAny().toString());
        System.out.println("* " + XmlUtil.node2String((Node) response.getAny(), false, false));
        System.out.println("**********************************");
    }

    private String createRequestString(ReplicationRequestType request) throws JAXBException {

        StringWriter writer = new StringWriter();
        Marshaller marshaller = jaxContext.createMarshaller();
        marshaller.marshal(new JAXBElement<ReplicationRequestType>
                (serviceName, ReplicationRequestType.class, request), writer);
        return writer.toString();
    }

}
