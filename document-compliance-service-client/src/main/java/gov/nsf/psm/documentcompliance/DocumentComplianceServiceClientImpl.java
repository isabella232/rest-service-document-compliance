package gov.nsf.psm.documentcompliance;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.ComplianceConfig;
import gov.nsf.psm.foundation.model.compliance.ComplianceModel;
import gov.nsf.psm.foundation.restclient.NsfRestTemplate;

public class DocumentComplianceServiceClientImpl implements DocumentComplianceServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentComplianceServiceClientImpl.class);

    private boolean authenticationRequired;
    private int requestTimeout;
    private String serverURL;
    private String inputStreamURL = "/complianceModel";
    private String metadataURL = "/metadata";
    private Boolean serviceEnabled;
    private String username;
    private String password;
    private String eUrl = "Endpoint URL: ";

    @Override
    public String getServerURL() {
        return serverURL;
    }

    @Override
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    @Override
    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    @Override
    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    @Override
    public int getRequestTimeout() {
        return requestTimeout;
    }

    @Override
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @Override
    public Boolean getServiceEnabled() {
        return serviceEnabled;
    }

    @Override
    public void setServiceEnabled(Boolean serviceEnabled) {
        this.serviceEnabled = serviceEnabled;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    private static HttpHeaders createHttpHeaderswithAuth(String username, String password) {
        return NsfRestTemplate.createHeaderswithAuthentication(username, password);
    }

    @Override
    public ComplianceModel getMetadata(String origFileName, byte[] byteArr, ComplianceConfig config)
            throws CommonUtilException {
        try {
            RestTemplate documentComplianceServiceClient = NsfRestTemplate.setupRestTemplate(authenticationRequired,
                    requestTimeout);

            MultiValueMap<String, Object> requestParts = getRequestParts(origFileName, byteArr, config.getMimeTypes(),
                    config.getNonTextColumns(), config.isTablesOnly());
            StringBuilder endpointURL = new StringBuilder(serverURL);
            endpointURL.append(inputStreamURL);
            endpointURL.append(metadataURL);

            return getEndpointURL(endpointURL, documentComplianceServiceClient, requestParts).getBody();
        } catch (Exception e) {
            throw new CommonUtilException(e);
        }
    }

    @Override
    public ComplianceModel getComplianceModel(String origFileName, byte[] byteArr, ComplianceConfig config)
            throws CommonUtilException {
        try {
            RestTemplate documentComplianceServiceClient = NsfRestTemplate.setupRestTemplate(authenticationRequired,
                    requestTimeout);

            MultiValueMap<String, Object> requestParts = getRequestParts(origFileName, byteArr, config.getMimeTypes(),
                    config.getNonTextColumns(), config.isTablesOnly());
            StringBuilder endpointURL = new StringBuilder(serverURL);
            endpointURL.append(inputStreamURL);

            return getEndpointURL(endpointURL, documentComplianceServiceClient, requestParts).getBody();
        } catch (Exception e) {
            throw new CommonUtilException(e);
        }
    }

    private ResponseEntity<ComplianceModel> getEndpointURL(StringBuilder endpointURL,
            RestTemplate documentComplianceServiceClient, MultiValueMap<String, Object> requestParts) {

        LOGGER.info(eUrl + endpointURL);

        HttpHeaders headers = authenticationRequired ? createHttpHeaderswithAuth(username, password)
                : new HttpHeaders();
        headers.set("Content-Type", "multipart/form-data");
        LOGGER.debug("Headers: " + headers.toString());

        return documentComplianceServiceClient.exchange(endpointURL.toString(), HttpMethod.POST,
                new HttpEntity<>(requestParts, headers), ComplianceModel.class);
    }

    private static MultiValueMap<String, Object> getRequestParts(String origFileName, byte[] byteArr, List<String> mimeTypes,
            List<String> nonTextColumns, boolean isTablesOnly) throws CommonUtilException {
        MultiValueMap<String, Object> requestParts = new LinkedMultiValueMap<>();
        try {
            requestParts.add(origFileName, byteArr);
            requestParts.add(ComplianceConfig.TABLES_ONLY, isTablesOnly);
            if (mimeTypes != null && !mimeTypes.isEmpty()) {
                for (String mimeType : mimeTypes) {
                    requestParts.add(ComplianceConfig.MIME_TYPE + "_" + mimeTypes.indexOf(mimeType), mimeType);
                }
            }
            if (nonTextColumns != null && !nonTextColumns.isEmpty()) {
                for (String nonTextColumn : nonTextColumns) {
                    requestParts.add(ComplianceConfig.NONTEXT_COLUMN + "_" + nonTextColumns.indexOf(nonTextColumn),
                            nonTextColumn);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error encountered in getRequestParts()", e);
        }
        return requestParts;
    }
}
