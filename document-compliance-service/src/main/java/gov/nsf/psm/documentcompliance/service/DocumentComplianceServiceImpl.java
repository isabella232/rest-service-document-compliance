package gov.nsf.psm.documentcompliance.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.Part;

import org.apache.catalina.core.ApplicationPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import gov.nsf.psm.documentcompliance.compliance.common.utility.Constants;
import gov.nsf.psm.documentcompliance.compliance.common.utility.DocComplianceUtils;
import gov.nsf.psm.documentcompliance.service.parameter.PdfParameters;
import gov.nsf.psm.documentcompliance.service.parameter.SpreadsheetParameters;
import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.ComplianceConfig;
import gov.nsf.psm.foundation.model.compliance.ComplianceModel;

@Component("documentComplianceService")
public class DocumentComplianceServiceImpl implements DocumentComplianceService {

    @Value("${file.upload.max-file-size}")
    private String fileUploadSizeLimit;

    @Value("${pdf.font.detection.ignore.blankSpaces}")
    private Boolean fontDetectionIgnoreBlankSpaces;

    @Value("${pdf.font.detection.ignore.supersubscript}")
    private Boolean fontDetectionIgnoreSuperSubscript;

    @Value("${pdf.font.detection.ignore.characters}")
    private String specialCharacters;
    
    @Value("${pdf.font.filter.name-family}")
    private String fonts;

    @Value("${pdf.logging.text.extractor.use}")
    private Boolean useTextExtractor;
    
    @Value("${spreadsheet.encoding.charset.check}")
    private Boolean checkCharset;
    
    @Value("${spreadsheet.encoding.charset.default}")
    private String defaultEncoding;

    private String logNotACorrectMimeType = "The document to be checked is not an allowed MIME type";

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentComplianceServiceImpl.class);

    @Override
    public ComplianceModel getComplianceModel(Collection<Part> parts, boolean metadataOnly) throws CommonUtilException {
        int i = 0;
        boolean isTablesOnly = false;
        ApplicationPart filePart = null;
        List<String> mimeTypes = new ArrayList<>();
        List<String> nonTextColumns = new ArrayList<>();
        for (Part part : parts) {
            boolean addValues = false;
            switch (i) {
            case 0:
                filePart = (ApplicationPart) part;
                break;
            case 1:
                isTablesOnly = DocComplianceUtils.getIsTablesOnly((ApplicationPart) part);
                break;
            default:
                addValues = true;
                break;
            }
            if (addValues) {
                if (part.getName().indexOf(ComplianceConfig.MIME_TYPE + "_") > -1) {
                    mimeTypes.addAll(DocComplianceUtils.getParams((ApplicationPart) part));
                }
                if (part.getName().indexOf(ComplianceConfig.NONTEXT_COLUMN + "_") > -1) {
                    nonTextColumns.addAll(DocComplianceUtils.getParams((ApplicationPart) part));
                }
            }
            i++;
        }
        if (metadataOnly) {
            return getMetadata(filePart, mimeTypes);
        } else {
            return getModel(filePart, mimeTypes, nonTextColumns, isTablesOnly);
        }
    }

    @Override
    public ComplianceModel getMetadata(ApplicationPart filePart, List<String> mimeTypes) throws CommonUtilException {
        ComplianceModel compliance = new ComplianceModel();
        try {
            String mimeType = DocComplianceUtils.getMimeType(filePart);
            compliance.setMimeType(mimeType);
            float fileSize = DocComplianceUtils.convertFileSizeFromBytesToMB(filePart.getSize());
            if (fileUploadSizeLimit != null
                    && fileSize < Float.parseFloat(fileUploadSizeLimit.toUpperCase().replace("MB", ""))) {
                LOGGER.info("MIME Type: " + mimeType);
                switch (mimeType) {
                case ComplianceModel.MIME_TYPE_PDF:
                    compliance = DocComplianceUtils.getDocumentMetadata(compliance, mimeTypes, mimeType, filePart);
                    break;
                case ComplianceModel.MIME_TYPE_OOP:
                case ComplianceModel.MIME_TYPE_XLS:
                case ComplianceModel.MIME_TYPE_XLSX:
                case ComplianceModel.MIME_TYPE_MSO:
                    compliance = DocComplianceUtils.getSpreadsheetMetadata(compliance, mimeTypes, mimeType, filePart);
                    break;
                default:
                    LOGGER.info(logNotACorrectMimeType);
                    break;
                }
            }
        } catch (IOException e) {
            throw new CommonUtilException(Constants.DCS_GET_METADATA_EXCEPTION_MESSAGE, e);
        }
        return compliance;
    }

    @Override
    public ComplianceModel getModel(ApplicationPart filePart, List<String> mimeTypes, List<String> nonTextColumns,
            boolean isTablesOnly) throws CommonUtilException {
        ComplianceModel compliance = new ComplianceModel();
        try {
            String mimeType = DocComplianceUtils.getMimeType(filePart);
            compliance.setMimeType(mimeType);
            LOGGER.info("MIME Type: " + mimeType);
            float fileSize = DocComplianceUtils.convertFileSizeFromBytesToMB(filePart.getSize());
            if (fileUploadSizeLimit != null
                    && fileSize < Float.parseFloat(fileUploadSizeLimit.toUpperCase().replace("MB", ""))) {
                switch (mimeType) {
                case ComplianceModel.MIME_TYPE_PDF:
                    PdfParameters pdfParams = new PdfParameters(fontDetectionIgnoreBlankSpaces,
                            fontDetectionIgnoreSuperSubscript, useTextExtractor, specialCharacters, DocComplianceUtils.convertToMap(fonts));
                    compliance = DocComplianceUtils.getDocumentModel(compliance, mimeTypes, filePart, pdfParams);
                    break;
                case ComplianceModel.MIME_TYPE_XLS:
                case ComplianceModel.MIME_TYPE_XLSX:
                case ComplianceModel.MIME_TYPE_MSO:
                    SpreadsheetParameters spreadsheetParams = new SpreadsheetParameters(checkCharset, defaultEncoding);
                    compliance = DocComplianceUtils.getSpreadsheetModel(compliance, mimeTypes, mimeType, nonTextColumns,
                            filePart, spreadsheetParams, isTablesOnly);
                    break;
                default:
                    LOGGER.info(logNotACorrectMimeType);
                    break;
                }
            }
        } catch (IOException e) {
            throw new CommonUtilException(Constants.DCS_GET_MODEL_EXCEPTION_MESSAGE, e);
        }

        return compliance;
    }

}
