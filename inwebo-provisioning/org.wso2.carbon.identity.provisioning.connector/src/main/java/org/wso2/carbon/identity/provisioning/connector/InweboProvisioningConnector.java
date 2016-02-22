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
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.ProvisionedIdentifier;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.AbstractOutboundProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.user.profile.mgt.UserFieldDTO;
import org.wso2.carbon.identity.user.profile.mgt.UserProfileDTO;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        try {
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
                        if (StringUtils.isNotEmpty(provisionedId) && !provisionedId.equals("0")) {
                            log.info("User creation in InWebo is done.");
//                        addProfile(login, phone, mail, firstName, name);
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
                            log.info("User deletion from InWebo is done.");
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
            if (StringUtils.isNotEmpty(provisionedId) && !provisionedId.equals("0")) {
                identifier.setIdentifier(provisionedId);
            }
            return identifier;
        } catch (IdentityProvisioningException e) {
            log.error(e);
            return null;
        }
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
            UserCreation userCreation = new UserCreation();
            provisionedId = userCreation.invokeSOAP(userId, serviceId, login, firstName, name, mail, phone, status,
                    role, access, codeType, language, extraFields);
        } catch (IdentityProvisioningException e) {
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
            UserUpdation userUpdation = new UserUpdation();
            updationStatus = userUpdation.invokeSOAP(userId, serviceId, loginId, login, firstName, name, mail,
                    phone, status, role, extraFields);
        } catch (IdentityProvisioningException e) {
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
            UserDeletion UserDeletion = new UserDeletion();
            deletionStatus = UserDeletion.deleteUser(loginId, userId, serviceId);
        } catch (IdentityProvisioningException e) {
            throw new IdentityProvisioningException("Error while creating the user", e);
        }
        return deletionStatus;
    }

    /**
     * @return
     * @throws UserStoreException
     */
    private Claim[] getAllSupportedClaims(UserRealm realm, String dialectUri)
            throws org.wso2.carbon.user.api.UserStoreException {
        org.wso2.carbon.user.api.ClaimMapping[] claims = null;
        List<Claim> reqClaims = null;

        claims = realm.getClaimManager().getAllSupportClaimMappingsByDefault();
        reqClaims = new ArrayList<Claim>();
        for (int i = 0; i < claims.length; i++) {
            if (dialectUri.equals(claims[i].getClaim().getDialectURI()) && (claims[i] != null && claims[i].getClaim().getDisplayTag() != null
                    && !claims[i].getClaim().getClaimUri().equals(IdentityConstants.CLAIM_PPID))) {

                reqClaims.add((Claim) claims[i].getClaim());
            }
        }
        return reqClaims.toArray(new Claim[reqClaims.size()]);
    }

    public void addProfile(String login, String phone, String mail, String firstName, String name) {
        if (StringUtils.isNotEmpty(login)) {
            UserRealm userRealm = null;
            try {
                String tenantDomain = MultitenantUtils.getTenantDomain(login);
                int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
                RealmService realmService = IdentityTenantUtil.getRealmService();
                userRealm = (UserRealm) realmService.getTenantUserRealm(tenantId);
            } catch (Exception e) {
            }
            String username = MultitenantUtils.getTenantAwareUsername(String.valueOf(login));
            if (userRealm != null) {
                UserProfileDTO userprofile = new UserProfileDTO();
                List<UserFieldDTO> userFields = new ArrayList<UserFieldDTO>();
                Claim[] claims = new Claim[0];
                try {
                    claims = getAllSupportedClaims(userRealm, UserCoreConstants.DEFAULT_CARBON_DIALECT);
                } catch (org.wso2.carbon.user.api.UserStoreException e) {

                }
                for (int j = 0; j < claims.length; j++) {
                    Claim claim = claims[j];
                    String claimUri = claim.getClaimUri();
                    if (InweboConnectorConstants.PHONE_CLAIM.equals(claimUri)
                            || InweboConnectorConstants.MAIL_CLAIM.equals(claimUri)
                            || InweboConnectorConstants.LAST_NAME_CLAIM.equals(claimUri)
                            || InweboConnectorConstants.FIRST_NAME_CLAIM.equals(claimUri)) {
                        UserFieldDTO fieldDTO = new UserFieldDTO();
                        if (InweboConnectorConstants.PHONE_CLAIM.equals(claimUri)) {
                            fieldDTO.setFieldValue(phone);
                        } else if (InweboConnectorConstants.MAIL_CLAIM.equals(claimUri)) {
                            fieldDTO.setFieldValue(mail);
                        } else if (InweboConnectorConstants.FIRST_NAME_CLAIM.equals(claimUri)) {
                            fieldDTO.setFieldValue(firstName);
                        } else if (InweboConnectorConstants.LAST_NAME_CLAIM.equals(claimUri)) {
                            fieldDTO.setFieldValue(name);
                        }
                        fieldDTO.setClaimUri(claimUri);
                        fieldDTO.setDisplayName(claim.getDisplayTag());
                        fieldDTO.setRegEx(claim.getRegEx());
                        fieldDTO.setRequired(claim.isRequired());
                        fieldDTO.setDisplayOrder(claim.getDisplayOrder());
                        fieldDTO.setCheckedAttribute(claim.isCheckedAttribute());
                        fieldDTO.setReadOnly(claim.isReadOnly());
                        userFields.add(userFields.size(), fieldDTO);
                    }
                }
                userprofile.setFieldValues(userFields.toArray(new UserFieldDTO[userFields.size()]));

                HashMap map = new HashMap();
                UserFieldDTO[] newUserFields = userprofile.getFieldValues();
                int len$ = newUserFields.length;
                for (int i$ = 0; i$ < len$; ++i$) {
                    UserFieldDTO data = newUserFields[i$];
                    String claimURI = data.getClaimUri();
                    String value = data.getFieldValue();
                    if (!data.isReadOnly()) {
                        if (value == "" && "http://wso2.org/claims/identity/otp".equals(claimURI)) {
                            value = "false";
                        }
                        map.put(claimURI, value);
                    }
                }

                if (userprofile.getProfileConifuration() != null) {
                    map.put(InweboConnectorConstants.PROFILE_CONFIGURATION, userprofile.getProfileConifuration());
                } else {
                    map.put(InweboConnectorConstants.PROFILE_CONFIGURATION,
                            InweboConnectorConstants.PROFILE_CONFIGURATION_DEFAULT);
                }

                UserStoreManager userStoreManager = null;
                try {
                    userStoreManager = userRealm.getUserStoreManager();
                } catch (UserStoreException e) {
                    log.error("Unable to get user store manager to update the profile:" + e.getMessage(), e);
                }
                try {
                    userStoreManager.setUserClaimValues(username, map, userprofile.getProfileName());
                } catch (UserStoreException e) {
                    log.error("Unable to set user claim values while updating the profile: " + e.getMessage(), e);
                }
            }
        }
    }
}