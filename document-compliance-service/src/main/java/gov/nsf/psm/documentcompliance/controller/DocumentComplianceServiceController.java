package gov.nsf.psm.documentcompliance.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import gov.nsf.psm.documentcompliance.service.DocumentComplianceService;
import gov.nsf.psm.foundation.controller.PsmBaseController;
import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.ComplianceModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@CrossOrigin
@RestController
@RequestMapping(path = "/api/v1")
@ApiResponses(value = { @ApiResponse(code = 404, message = "Resource not found"),
        @ApiResponse(code = 500, message = "Internal server error") })
public class DocumentComplianceServiceController extends PsmBaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentComplianceServiceController.class);

    @Autowired
    DocumentComplianceService docComplianceService;

    @ApiOperation(value = "Get metadata", notes = "Returns a document model, containing metadata, for a given PDF document", response = ComplianceModel.class)
    @RequestMapping(path = "/complianceModel/metadata", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ComplianceModel getMetadata(MultipartHttpServletRequest request) throws CommonUtilException {
        LOGGER.debug("DocumentComplianceServiceController.getDocumentModel(MultipartHttpServletRequest request)");
        ComplianceModel document = null;
        try {
            document = docComplianceService.getComplianceModel(request.getParts(), true);
        } catch (Exception e) {
            throw new CommonUtilException(e);
        }
        return document;
    }

    @ApiOperation(value = "Get compliance model", notes = "Returns a document model for a given document", response = ComplianceModel.class)
    @RequestMapping(path = "/complianceModel", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ComplianceModel getComplianceModel(MultipartHttpServletRequest request) throws CommonUtilException {
        LOGGER.debug("DocumentComplianceServiceController.getDocumentModel(MultipartHttpServletRequest request)");
        ComplianceModel document = null;
        try {
            document = docComplianceService.getComplianceModel(request.getParts(), false);
        } catch (Exception e) {
            throw new CommonUtilException(e);
        }
        return document;
    }

}