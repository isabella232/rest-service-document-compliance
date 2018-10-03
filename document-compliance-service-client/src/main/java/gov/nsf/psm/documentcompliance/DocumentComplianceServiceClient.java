package gov.nsf.psm.documentcompliance;

import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.ComplianceConfig;
import gov.nsf.psm.foundation.model.compliance.ComplianceModel;

public interface DocumentComplianceServiceClient {

    public ComplianceModel getMetadata(String origFileName, byte[] bytes, ComplianceConfig config)
            throws CommonUtilException;

    public ComplianceModel getComplianceModel(String origFileName, byte[] bytes, ComplianceConfig config)
            throws CommonUtilException;

    public String getServerURL();

    public Boolean getServiceEnabled();

    public void setServiceEnabled(Boolean serviceEnabled);

    public String getUsername();

    public void setUsername(String username);

    public String getPassword();

    public void setPassword(String password);

    public int getRequestTimeout();

    public void setRequestTimeout(int requestTimeout);

    public boolean isAuthenticationRequired();

    public void setAuthenticationRequired(boolean authenticationRequired);

    public void setServerURL(String serverURL);
}
