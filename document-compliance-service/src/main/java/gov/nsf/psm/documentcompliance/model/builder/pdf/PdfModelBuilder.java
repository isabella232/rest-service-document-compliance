package gov.nsf.psm.documentcompliance.model.builder.pdf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.kernel.crypto.BadPasswordException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

import gov.nsf.psm.documentcompliance.compliance.common.utility.DocComplianceUtils;
import gov.nsf.psm.documentcompliance.compliance.pdf.utility.PdfModelBuilderUtils;
import gov.nsf.psm.documentcompliance.model.builder.ComplianceModelBuilder;
import gov.nsf.psm.documentcompliance.service.parameter.PdfParameters;
import gov.nsf.psm.factmodel.DocumentFactModel;
import gov.nsf.psm.factmodel.FileFactModel;
import gov.nsf.psm.factmodel.PageFactModel;
import gov.nsf.psm.factmodel.SectionFactModel;
import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.DocumentPart;
import gov.nsf.psm.foundation.model.compliance.doc.DocumentModel;
import gov.nsf.psm.foundation.model.compliance.doc.PageModel;
import gov.nsf.psm.foundation.model.compliance.doc.SectionModel;

public class PdfModelBuilder implements ComplianceModelBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfModelBuilder.class);

    private PdfParameters params = null;

    public PdfModelBuilder() {
        // Empty constructor
    }

    public PdfModelBuilder(PdfParameters params) {
        this.params = params;
    }

    @Override
    public DocumentModel buildMetadata(InputStream inputStream, String fileName, long sizeInBytes)
            throws CommonUtilException {
        DocumentModel document = null;
        PdfReader reader = null;
        PdfDocument doc = null;
        float fileSize = DocComplianceUtils.convertFileSizeFromBytesToMB(sizeInBytes);
        try {
            reader = new PdfReader(inputStream);
            doc = new PdfDocument(reader);
            document = PdfModelBuilderUtils.initDocumentModel(doc);
            PdfModelBuilderUtils.displayPdfMetadataHeading();
            PdfModelBuilderUtils.displayPdfMetadata(doc);
            FileFactModel fileFactModel = new FileFactModel();
            int noOfPages = PdfModelBuilderUtils.initDocumentMetadataOutput(fileName, doc, sizeInBytes, fileSize);
            PdfModelBuilderUtils.displayFileSourceOutput(document);
            fileFactModel.setSize(fileSize);
            fileFactModel.setName(fileName);
            DocumentFactModel docFactModel = new DocumentFactModel();
            docFactModel.setFile(fileFactModel);
            docFactModel.setCorrectMimeType(true);
            document.setEncrypted(false);
            docFactModel.setNoOfPages(noOfPages);
            document.setDocFactModel(docFactModel);
            PdfModelBuilderUtils.displaySuccessMessage();
        } catch (BadPasswordException e) {
            LOGGER.debug(e.getMessage(), e);
            document = new DocumentModel();
            document.setEncrypted(true);
            document.setPasswordProtected(true);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        } finally {
            LOGGER.info("");
            PdfModelBuilderUtils.cleanUpFileObjects(reader, inputStream);
        }

        return document;

    }

    @Override
    public DocumentModel buildModel(InputStream inputStream, String fileName, long sizeInBytes)
            throws CommonUtilException {

        int numberOfPages = 0;
        DocumentModel document = null;
        PdfReader reader = null;

        float fileSize = DocComplianceUtils.convertFileSizeFromBytesToMB(sizeInBytes);

        try {
            
            reader = new PdfReader(inputStream);
            List<PageModel> pages = new ArrayList<>();
            List<PageFactModel> pageFactModels = new ArrayList<>();
            PdfDocument doc = new PdfDocument(reader);

            FileFactModel fileFactModel = new FileFactModel();
            PdfModelBuilderUtils.displayPdfMetadataHeading();
            PdfModelBuilderUtils.displayPdfMetadata(doc);
            numberOfPages = PdfModelBuilderUtils.initDocumentMetadataOutput(fileName, doc, sizeInBytes, fileSize);
            document = PdfModelBuilderUtils.initDocumentModel(doc);
            PdfModelBuilderUtils.displayFileSourceOutput(document);
            PdfModelBuilderUtils.displayParameterSettingOutput(params);
            fileFactModel.setSize(fileSize);
            fileFactModel.setName(fileName);
            List<SectionModel> sections = new ArrayList<>();

            Set<String> urls = new TreeSet<>();
            
            PdfModelBuilderUtils.displayBeginParsingMessage();

            for (int k = 1; k <= numberOfPages; ++k) {
                DocumentPart docPart = PdfModelBuilderUtils.getDocumentPart(doc, k, params);
                PageModel page = docPart.getPage();
                List<SectionModel> sectionHeadings = docPart.getSectionHeadings();
                if (!sectionHeadings.isEmpty()) {
                    sections.addAll(sectionHeadings);
                }
                if (page.getNoOfTextChars() > 0) {
                    PageFactModel pageFactModel = DocComplianceUtils.getPageFactModel(page, params.getFontMap());
                    pages.add(page);
                    pageFactModels.add(pageFactModel);
                    urls.addAll(page.getUrls());
                }
            }
            
            document = PdfModelBuilderUtils.completeDocumentModel(document, pages, sections);
            PdfModelBuilderUtils.displayEndParsingMessage();
            
            List<SectionFactModel> secFactModels = DocComplianceUtils.getSectionFactModelList(sections);
            DocumentFactModel docFactModel = document.getDocFactModel();
            if (docFactModel == null) {
                docFactModel = new DocumentFactModel();
            }
            docFactModel.setFile(fileFactModel);
            docFactModel.setPages(pageFactModels);
            docFactModel.setNoOfPages(document.getNoOfPages());
            docFactModel.setSections(secFactModels);
            docFactModel.setCorrectMimeType(true);
            if (!urls.isEmpty()) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                List<String> urlList = new ArrayList(Arrays.asList(urls.toArray()));
                docFactModel.setUrls(urlList);
            } else {
                docFactModel.setUrls(new ArrayList<String>());
            }
            document.setDocFactModel(docFactModel);
            document.setEncrypted(false);
            
            PdfModelBuilderUtils.displaySuccessMessage();
            
        } catch (BadPasswordException e) {
            LOGGER.debug(e.getMessage(), e);
            document = new DocumentModel();
            document.setEncrypted(true);
            document.setPasswordProtected(true);
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
        } finally {
            PdfModelBuilderUtils.cleanUpFileObjects(reader, inputStream);
        }

        return document;

    }

}
