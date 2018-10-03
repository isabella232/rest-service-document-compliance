package gov.nsf.psm.documentcompliance.service;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.Part;

import org.apache.catalina.core.ApplicationPart;

import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.ComplianceModel;

public interface DocumentComplianceService {

    public ComplianceModel getComplianceModel(Collection<Part> parts, boolean metadataOnly) throws CommonUtilException;

    public ComplianceModel getMetadata(ApplicationPart filePart, List<String> mimeTypes) throws CommonUtilException;

    public ComplianceModel getModel(ApplicationPart filePart, List<String> mimeTypes, List<String> nonTextColumns,
            boolean isTablesOnly) throws CommonUtilException;

}
