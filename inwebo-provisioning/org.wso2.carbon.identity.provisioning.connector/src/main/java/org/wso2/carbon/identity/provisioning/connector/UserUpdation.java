/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.identity.provisioning.connector;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;

import javax.xml.soap.*;
import java.io.IOException;

public class UserUpdation {
    public static String provisionedId = null;
    private static String userId;
    private static String serviceId;
    private static String loginId;
    private static String login;
    private static String firstName;
    private static String name;
    private static String mail;
    private static String phone;
    private static String status;
    private static String role;
    private static String extrafields;

    public UserUpdation(String userId, String serviceId, String loginId, String login, String firstName, String name,
                        String mail, String phone, String status, String role, String extrafields) {
        this.userId = userId;
        this.serviceId = serviceId;
        this.loginId = loginId;
        this.login = login;
        this.firstName = firstName;
        this.name = name;
        this.mail = mail;
        this.phone = phone;
        this.status = status;
        this.role = role;
        this.extrafields = extrafields;
    }

    /**
     * Method for create SOAP connection
     */
    public static boolean invokeSOAP() throws IdentityProvisioningException {
        try {
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();
            String url = InweboConnectorConstants.INWEBO_URL;
            SOAPMessage soapResponse = soapConnection.call(createUserObject(), url);
            String updationStatus = soapResponse.getSOAPBody().getElementsByTagName("loginUpdateReturn").item(0)
                    .getTextContent().toString();
            soapConnection.close();
            return StringUtils.equals("OK", updationStatus);
        } catch (Exception e) {
            throw new IdentityProvisioningException("Error occurred while sending SOAP Request to Server", e);
        }
    }

    private static SOAPMessage createUserObject() throws SOAPException, IdentityProvisioningException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        String serverURI = InweboConnectorConstants.INWEBO_URI;
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("con", serverURI);
        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapBodyElem = soapBody.addChildElement("loginUpdate", "con");
        SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("userid", "con");
        soapBodyElem1.addTextNode(UserUpdation.userId);
        SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("serviceid", "con");
        soapBodyElem2.addTextNode(UserUpdation.serviceId);
        SOAPElement soapBodyElem3 = soapBodyElem.addChildElement("loginid", "con");
        soapBodyElem3.addTextNode(UserUpdation.loginId);
        SOAPElement soapBodyElem4 = soapBodyElem.addChildElement("login", "con");
        soapBodyElem4.addTextNode(UserUpdation.login);
        SOAPElement soapBodyElem5 = soapBodyElem.addChildElement("firstname", "con");
        soapBodyElem5.addTextNode(UserUpdation.firstName);
        SOAPElement soapBodyElem6 = soapBodyElem.addChildElement("name", "con");
        soapBodyElem6.addTextNode(UserUpdation.name);
        SOAPElement soapBodyElem7 = soapBodyElem.addChildElement("mail", "con");
        soapBodyElem7.addTextNode(UserUpdation.mail);
        SOAPElement soapBodyElem8 = soapBodyElem.addChildElement("phone", "con");
        soapBodyElem8.addTextNode(UserUpdation.phone);
        SOAPElement soapBodyElem9 = soapBodyElem.addChildElement("status", "con");
        soapBodyElem9.addTextNode(UserUpdation.status);
        SOAPElement soapBodyElem10 = soapBodyElem.addChildElement("role", "con");
        soapBodyElem10.addTextNode(UserUpdation.role);
        SOAPElement soapBodyElem11 = soapBodyElem.addChildElement("extrafields", "con");
        soapBodyElem11.addTextNode(extrafields);
        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", serverURI + "/services/ConsoleAdmin");
        soapMessage.saveChanges();
        //         Print the request message
        System.out.print("Request SOAP Message = ");
        try {
            soapMessage.writeTo(System.out);
        } catch (IOException e) {
            throw new IdentityProvisioningException("Error while printing",e);
        }
        System.out.println();
        return soapMessage;
    }
}