/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.provisioning.connector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.ProvisionedIdentifier;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.AbstractOutboundProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;

import java.util.Properties;

public class InweboProvisioningConnector extends AbstractOutboundProvisioningConnector {

    private static final Log log = LogFactory.getLog(InweboProvisioningConnector.class);
    private InweboProvisioningConnectorConfig configHolder;

    @Override
    public void init(Property[] provisioningProperties) throws IdentityProvisioningException {
        Properties configs = new Properties();

        if (provisioningProperties != null && provisioningProperties.length > 0) {
            for (Property property : provisioningProperties) {
                configs.put(property.getName(), property.getValue());
                if (IdentityProvisioningConstants.JIT_PROVISIONING_ENABLED.equals(property
                        .getName())) {
                    if ("1".equals(property.getValue())) {
                        jitProvisioningEnabled = true;
                    }
                }
            }
        }
        configHolder = new InweboProvisioningConnectorConfig(configs);
    }

    @Override
    public ProvisionedIdentifier provision(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {
        String provisionedId = null;
        if (provisioningEntity != null) {
            String login = provisioningEntity.getEntityName().toString();
            String userId = configHolder.getValue(InweboConnectorConstants.INWEBO_USER_ID);
            String serviceId = configHolder.getValue(InweboConnectorConstants.INWEBO_SERVICE_ID);
            String p12file = configHolder.getValue(InweboConnectorConstants.INWEBO_P12FILE);
            String p12password = configHolder.getValue(InweboConnectorConstants.INWEBO_P12PASSWORD);
            String firstName = configHolder.getValue(InweboConnectorConstants.INWEBO_FIRSTNAME);
            String name = configHolder.getValue(InweboConnectorConstants.INWEBO_NAME);
            String mail = configHolder.getValue(InweboConnectorConstants.INWEBO_MAIL);
            String phone = configHolder.getValue(InweboConnectorConstants.INWEBO_PHONENUMBER);
            String status = configHolder.getValue(InweboConnectorConstants.INWEBO_STATUS);
            String role = configHolder.getValue(InweboConnectorConstants.INWEBO_ROLE);
            String access = configHolder.getValue(InweboConnectorConstants.INWEBO_ACCESS);
            String codeType = configHolder.getValue(InweboConnectorConstants.INWEBO_CODETYPE);
            String extraFields = StringUtils.isNotEmpty(configHolder.getValue(InweboConnectorConstants.INWEBO_EXTRAFIELDS))
                    ? configHolder.getValue(InweboConnectorConstants.INWEBO_EXTRAFIELDS) : "";
            String language = StringUtils.isNotEmpty(configHolder.getValue(InweboConnectorConstants.INWEBO_LANG))
                    ? configHolder.getValue(InweboConnectorConstants.INWEBO_LANG)
                    : InweboConnectorConstants.INWEBO_LANG_ENGLISH;

            if (provisioningEntity.isJitProvisioning() && !isJitProvisioningEnabled()) {
                if (log.isDebugEnabled()) {
                    log.debug("JIT provisioning disabled for inwebo connector");
                }
                return null;
            }
            if (provisioningEntity.getEntityType() == ProvisioningEntityType.USER) {
                java.lang.System.setProperty(InweboConnectorConstants.AXIS2, InweboConnectorConstants.AXIS2_FILE);
                if (provisioningEntity.getOperation() == ProvisioningOperation.POST) {
                    provisionedId = createAUser(provisioningEntity, userId, serviceId, login, firstName,
                            name, mail, phone, status, role, access, codeType, language, extraFields, p12file, p12password);
                    if (StringUtils.isNotEmpty(provisionedId)) {
                        log.info("User creation in InWebo is done.");
                    }
                } else if (provisioningEntity.getOperation() == ProvisioningOperation.PUT) {
                    login = provisioningEntity.getAttributes().get(ClaimMapping
                            .build(InweboConnectorConstants.USERNAME_CLAIM, null,
                                    (String) null, false)).get(0);
                    firstName = provisioningEntity.getAttributes().get(ClaimMapping
                            .build(InweboConnectorConstants.FIRST_NAME_CLAIM, InweboConnectorConstants.FIRST_NAME_CLAIM,
                                    (String) null, false)).get(0);
                    name = provisioningEntity.getAttributes().get(ClaimMapping
                            .build(InweboConnectorConstants.LAST_NAME_CLAIM, InweboConnectorConstants.LAST_NAME_CLAIM,
                                    (String) null, false)).get(0);
                    mail = provisioningEntity.getAttributes().get(ClaimMapping
                            .build(InweboConnectorConstants.MAIL_CLAIM, InweboConnectorConstants.MAIL_CLAIM,
                                    (String) null, false)).get(0);
                    phone = provisioningEntity.getAttributes().get(ClaimMapping
                            .build(InweboConnectorConstants.PHONE_CLAIM, InweboConnectorConstants.PHONE_CLAIM,
                                    (String) null, false)).get(0);
                    boolean updationStatus = updateAUser(provisioningEntity, userId, serviceId, login, firstName, name,
                            mail, phone, status, role, extraFields, p12file, p12password);
                    if (updationStatus) {
                        log.info("User updation in InWebo is done.");
                    }
                } else if (provisioningEntity.getOperation() == ProvisioningOperation.DELETE) {
                    boolean deletionStatus = deleteUser(provisioningEntity, serviceId, userId, p12file, p12password);
                    if (deletionStatus) {
                        log.info("User deletion in InWebo is done.");
                    }
                } else {
                    throw new IdentityProvisioningException("Unsupported provisioning opertaion.");
                }
            } else {
                throw new IdentityProvisioningException("Unsupported provisioning opertaion.");
            }
        }
        // creates a provisioned identifier for the provisioned user.
        ProvisionedIdentifier identifier = new ProvisionedIdentifier();
        identifier.setIdentifier(provisionedId);
        return identifier;
    }

    private String createAUser(ProvisioningEntity provisioningEntity, String userId, String serviceId, String login,
                               String firstName, String name, String mail, String phone, String status, String role,
                               String access, String codeType,
                               String language, String extraFields, String p12file, String p12password)
            throws IdentityProvisioningException {
        String provisionedId = null;
        try {
            Util.setHttpsClientCert(p12file, p12password);
        } catch (Exception e) {
            throw new IdentityProvisioningException("Error while adding certificate", e);

        }
        try {
            UserCreation userCreation = new UserCreation(userId, serviceId, login, firstName, name, mail, phone, status,
                    role, access, codeType, language, extraFields);
            provisionedId = userCreation.invokeSOAP();
        } catch (Exception e) {
            throw new IdentityProvisioningException("Error while creating the user", e);
        }
        return provisionedId;
    }

    private boolean updateAUser(ProvisioningEntity provisioningEntity, String userId, String serviceId,
                                String login, String firstName, String name, String mail, String phone, String status,
                                String role, String extraFields, String p12file, String p12password)
            throws IdentityProvisioningException {
        boolean updationStatus = false;
        try {
            Util.setHttpsClientCert(p12file, p12password);
        } catch (Exception e) {
            throw new IdentityProvisioningException("Error while adding certificate", e);

        }
        try {
            String loginId = provisioningEntity.getIdentifier().getIdentifier();
            UserUpdation userUpdation = new UserUpdation(userId, serviceId, loginId, login, firstName, name, mail,
                    phone, status, role, extraFields);
            updationStatus = userUpdation.invokeSOAP();
        } catch (Exception e) {
            throw new IdentityProvisioningException("Error while updating the user", e);
        }
        return updationStatus;
    }

    /**
     * @param provisioningEntity
     * @throws IdentityProvisioningException
     */
    private boolean deleteUser(ProvisioningEntity provisioningEntity, String serviceId, String userId, String p12file, String p12password)
            throws IdentityProvisioningException {
        boolean deletionStatus = false;
        try {
            Util.setHttpsClientCert(p12file, p12password);
        } catch (Exception e) {
            throw new IdentityProvisioningException("Error while adding certificate", e);
        }
        try {
            String loginId = provisioningEntity.getIdentifier().getIdentifier();
            UserDeletion UserDeletion = new UserDeletion(loginId, userId, serviceId);
            deletionStatus = UserDeletion.deleteUser();
        } catch (IdentityProvisioningException e) {
            throw new IdentityProvisioningException("Error while creating the user", e);
        }
        return deletionStatus;
    }
}



