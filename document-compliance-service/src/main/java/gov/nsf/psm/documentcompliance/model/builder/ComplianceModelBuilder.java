package gov.nsf.psm.documentcompliance.model.builder;

import java.io.InputStream;

import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.ComplianceBaseModel;

public interface ComplianceModelBuilder {

    public ComplianceBaseModel buildMetadata(InputStream inputStream, String fileName, long sizeInBytes)
            throws CommonUtilException;

    public ComplianceBaseModel buildModel(InputStream inputStream, String fileName, long sizeInBytes)
            throws CommonUtilException;

}
