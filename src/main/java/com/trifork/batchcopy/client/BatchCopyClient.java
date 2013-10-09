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

import com.trifork.batchcopy.jaxws.generated.ReplicationRequestType;
import com.trifork.batchcopy.jaxws.generated.ReplicationResponseType;
import dk.sosi.seal.model.Reply;
import dk.sosi.seal.xml.XmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

    /**
     * Perform a batch copy request
     * @param register Register name
     * @param dataType Datatype
     * @param offsetToken offset token
     * @return offset token of last entry
     * @throws JAXBException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public String performRequest(String register, String dataType, String offsetToken)
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
        Node atomFeedNode = (Node)response.getAny();

        printResponse(atomFeedNode);

        return extractLastToken(atomFeedNode);
    }

    private String extractLastToken(Node atomFeedNode) {
        // <atom:feed ..
        //   <atom:id> ..
        //   <atom:updated> ..
        //   <atom:title> ..
        //   <atom:author> ..
        //   <atom:entry> ..
        //    ...
        //   <atom:entry>
        //     <atom:id>tag:nsi.dk,2011:doseringsforslag/dosageunit/v1/13709460140000000001</atom:id>
        //                                                             |- Sidste del af ovenstående ID er det offset vi skal sende med i næste request
        //     ...
        //   </atom:entry>

        NodeList childNodes = atomFeedNode.getLastChild().getChildNodes();
        for (int i=0; i<childNodes.getLength(); ++i) {
            Node currentChild = childNodes.item(i);
            if (currentChild.getLocalName().equals("id")) {
                String completeId = currentChild.getTextContent();
                return completeId.substring(completeId.lastIndexOf("/")+1);
            }
        }
        return null;
    }

    private void printResponse(Node atomFeedNode) {
        System.out.println("**********************************");
        System.out.println(XmlUtil.node2String(atomFeedNode, true, false));
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
