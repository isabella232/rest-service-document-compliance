package gov.nsf.psm.documentcompliance.compliance.pdf.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.FilteredEventListener;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import com.itextpdf.kernel.pdf.canvas.parser.listener.TextMarginFinder;

import gov.nsf.psm.documentcompliance.compliance.common.utility.Constants;
import gov.nsf.psm.documentcompliance.compliance.pdf.PdfExtractionListener;
import gov.nsf.psm.documentcompliance.compliance.pdf.TextRenderInfoListener;
import gov.nsf.psm.documentcompliance.compliance.pdf.filter.TextEventFilter;
import gov.nsf.psm.documentcompliance.service.parameter.PdfParameters;
import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.DocumentPart;
import gov.nsf.psm.foundation.model.compliance.doc.DocumentModel;
import gov.nsf.psm.foundation.model.compliance.doc.FontModel;
import gov.nsf.psm.foundation.model.compliance.doc.ImageModel;
import gov.nsf.psm.foundation.model.compliance.doc.LineModel;
import gov.nsf.psm.foundation.model.compliance.doc.PageModel;
import gov.nsf.psm.foundation.model.compliance.doc.SectionModel;

public class PdfModelBuilderUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfModelBuilderUtils.class);

    private PdfModelBuilderUtils() {
        // Private constructor
    }

    public static DocumentPart getDocumentPart(PdfDocument doc, int pageNumber, PdfParameters params)
            throws CommonUtilException {

        boolean skipPage = false;
        TextMarginFinder finder = new TextMarginFinder();
        PageModel page = null;
        PdfPage pdfPage = null;
        DocumentPart docPart = null;
        FilteredEventListener listener = new FilteredEventListener(finder, new TextEventFilter());
        TextRenderInfoListener textListener = null;
        PdfCanvasProcessor parser = null;

        try {
            textListener = new TextRenderInfoListener(listener);
            parser = new PdfCanvasProcessor(textListener);
            pdfPage = doc.getPage(pageNumber);
            parser.processPageContent(pdfPage);
        } catch (Exception e) {
            skipPage = true;
            LOGGER.debug("Page #" + pageNumber + " does not have any text");
            LOGGER.debug(e.getMessage(), e);
        }

        if (!skipPage) { // Skip page if no text margin
            try {
                PdfExtractionListener info = new PdfExtractionListener();
                info.setFontDetectionIgnoreBlankSpaces(params.getFontDetectionIgnoreBlankSpaces());
                info.setFontDetectionIgnoreSuperSubscript(params.getFontDetectionIgnoreSuperSubscript());
                info.setSpecialCharacters(params.getSpecialCharacters());
                info.setPageNumber(pageNumber);
                info.setLastBlockCount(textListener.getBlockCount());
                info.setAvgSingleSpaceWidth(textListener.getAverageSingleSpaceWidth());
                info.setLastTextBlockOnSameLine(textListener.isLastBlockOnSameLine());
                if(finder.getTextRectangle() != null) {
                     info.setTextWidth(finder.getTextRectangle().getWidth());
                }

                LOGGER.debug("");
                LOGGER.debug(Constants.DOUBLE_CODE_BLOCK_SEPARATOR + " Page No. " + pageNumber + " "
                        + Constants.DOUBLE_CODE_BLOCK_SEPARATOR);
                
                parser = new PdfCanvasProcessor(info);
                pdfPage = doc.getPage(pageNumber);
                parser.processPageContent(pdfPage);
                PdfExtractionListener infoProcessed = (PdfExtractionListener) parser.getEventListener();

                // Set lines of Text
                String pageText = null;
                List<String> linesOfText = null;
                List<LineModel> lines = null;
                
                LOGGER.debug("");
                LOGGER.debug("Text");
                LOGGER.debug(Constants.CODE_BLOCK_SEPARATOR);

                if (params.getUseTextExtractor()) {
                    // If this option is selected, a custom strategy
                    // may be required for some pdfs
                    pageText = PdfTextExtractor.getTextFromPage(pdfPage, new SimpleTextExtractionStrategy());
                    linesOfText = PdfUtils.getTextLinesFromText(pageText);
                } else {
                    lines = infoProcessed.getLines();
                    linesOfText = PdfUtils.getTextLinesFromLines(lines, true);
                    
                    // To avoid conversion issues, use lines of text rather than
                    // text extraction
                    pageText = PdfUtils.getTextFromTextLines(linesOfText);
                }
                
                PdfNumber userUnit = pdfPage.getPdfObject().getAsNumber(PdfName.UserUnit); // Check for UserUnit

                docPart = processDocumentPart(doc, infoProcessed, finder, linesOfText, pageText, pageNumber);
                page = initOutput(docPart, finder, userUnit);
                docPart.setPage(page);
            } catch (Exception e) {
                throw new CommonUtilException(e);
            }
        }

        return docPart;

    }
    
    public static void displayPdfMetadataHeading() {
        LOGGER.debug("");
        LOGGER.debug("PDF Metadata");
        LOGGER.debug(Constants.CODE_BLOCK_SEPARATOR);
    }

    public static void displayPdfMetadata(PdfDocument doc) {
        LOGGER.debug("PDF Version: " + doc.getPdfVersion());
        LOGGER.debug("Number of pages: " + doc.getNumberOfPages());
        LOGGER.debug("Uses accessible (UA) format: "
                + (Double.parseDouble(PdfUtils.getPdfVersion(doc.getPdfVersion().toString())) >= 1.7d ? "true"
                        : "false"));
        LOGGER.debug("Encrypted: " + doc.getReader().isEncrypted()
                + (doc.getReader().isEncrypted()
                        ? " (Uses 128-bit encryption key: " + doc.getReader().getCryptoMode() + ")"
                        : ""));
        if (doc.getReader().isEncrypted()) {
            LOGGER.debug("Opened with the owner password: " + doc.getReader().isOpenedWithFullPermission());
        }
        LOGGER.debug("Tagged: " + doc.isTagged());
        LOGGER.debug("Keywords: " + (doc.getDocumentInfo() != null 
            && doc.getDocumentInfo().getKeywords() != null 
                 && !doc.getDocumentInfo().getKeywords().isEmpty()?StringUtils.collectionToDelimitedString(Arrays.asList(doc.getDocumentInfo().getKeywords()),", "):"None detected"));
    }

    // Initialize model
    public static DocumentModel initDocumentModel(PdfDocument doc) {
        DocumentModel document = new DocumentModel();
        document.setPasswordProtected(false);
        document.setEncrypted(doc.getReader().isEncrypted());
        document.setNoOfPages(doc.getNumberOfPages());
        document.setPdfVersion(String.valueOf(doc.getPdfVersion()));
        if (doc.getDocumentInfo() != null && doc.getDocumentInfo().getProducer() != null) {
            document.setProducer(doc.getDocumentInfo().getProducer());
        }
        return document;
    }
    
    public static DocumentModel completeDocumentModel(DocumentModel document, List<PageModel> pages, List<SectionModel> sections) {
        document.setPages(pages);
        document.setSections(sections);
        return document;
    }

    public static DocumentModel copyDocumentMetadata(DocumentModel dm1, DocumentModel dm2) {
        dm1.setEncrypted(dm2.isEncrypted());
        dm1.setPasswordProtected(dm2.isPasswordProtected());
        dm1.setNoOfPages(dm2.getNoOfPages());
        return dm1;
    }

    // Populate Page Model
    public static DocumentPart processDocumentPart(PdfDocument doc, PdfExtractionListener infoProcessed,
            TextMarginFinder finder, List<String> linesOfText, String pageText, int pageNumber)
            throws CommonUtilException {
        Rectangle rect = doc.getPage(pageNumber).getPageSizeWithRotation();
        List<String> urls = PdfUtils.findAllLinks(doc.getPage(pageNumber).getPdfObject());
        PageModel page = null;
        DocumentPart docPart = new DocumentPart();
        List<Float> nonBlankTextAscentLines = infoProcessed.getNonBlankTextAscentLines();
        List<Float> nonBlankTextDescentLines = infoProcessed.getNonBlankTextDescentLines();
        if (!nonBlankTextAscentLines.isEmpty() && !nonBlankTextDescentLines.isEmpty()) {
            try {
                List<Double> leadingValues = PdfUtils.getPredominantLeading(infoProcessed);
                List<FontModel> allFonts = new ArrayList<>();
                List<FontModel> unusedFonts = new ArrayList<>();
                PdfUtils.processResource(allFonts, doc.getPage(pageNumber).getResources().getPdfObject());
                unusedFonts.addAll(PdfUtils.getUnusedFonts(infoProcessed, allFonts));
                PdfExtractionListener infoProcessedWithFont = PdfUtils.getPredominantFont(infoProcessed, leadingValues);
                List<LineModel> lines = infoProcessedWithFont.getLines(); 
                page = PdfUtils.processResource(finder, rect, PdfUtils.getTopLeading(leadingValues, lines), PdfUtils.getLastY(lines));
                page.setPageNumber(pageNumber);
                List<String> nonLinkUrls = PdfUtils.findAllTextUrls(pageText);
                if (!urls.isEmpty()) {
                    urls = PdfUtils.correctUrlList(urls, nonLinkUrls);
                } else if (!nonLinkUrls.isEmpty()) {
                    urls = nonLinkUrls;
                }
                page.setUrls(urls);
                page.setTextFonts(infoProcessedWithFont.getFonts());
                page.setNonTextFonts(unusedFonts);
                page.setSuperscriptLines(infoProcessedWithFont.getSuperscriptLines());
                page.setTextLines(linesOfText);
                page.setNoOfTextChars(infoProcessed.getTotalCharacterCount());
                page.setNoOfTextCharsiText(pageText.chars().count());
                page.setImages(infoProcessedWithFont.getImages());
                page.setSubscriptLines(infoProcessedWithFont.getSubscriptLines());
                page.setLines(infoProcessedWithFont.getLines());
                if (!leadingValues.isEmpty()) {
                    page.setLeadingValues(leadingValues);
                }
                docPart.setPage(page);
                docPart.setSectionHeadings(infoProcessedWithFont.getHeadingLines());
            } catch (Exception e) {
                throw new CommonUtilException(e);
            }
        } else {
            page = new PageModel();
            page.setWidth(rect.getWidth());
            page.setHeight(rect.getHeight());
            page.setImages(infoProcessed.getImages());
            docPart.setPage(page);
            docPart.setSectionHeadings(new ArrayList<SectionModel>());
            LOGGER.debug("None");
        }
        if(LOGGER.isDebugEnabled()) {
            for(String line: linesOfText) {
                LOGGER.debug(line);
            }
        }
        return docPart;
    }

    // Logger output routine
    public static PageModel initOutput(DocumentPart part, TextMarginFinder finder, PdfNumber userUnit) {

        PageModel page = part.getPage();
        String addIn = " pt";
        LOGGER.debug("");
        LOGGER.debug("Data");
        LOGGER.debug(Constants.CODE_BLOCK_SEPARATOR);
        LOGGER.debug("Page Width : " + page.getWidth() + " pt");
        LOGGER.debug("Page Height : " + page.getHeight() + " pt");       
        LOGGER.debug("Predominant Leading : " + (page.getLeadingValues() != null && !page.getLeadingValues().isEmpty()
                ? page.getLeadingValues().get(0) + " pt"
                : "None"));
        if (page.getTextFonts() != null) {
            LOGGER.debug("Text Width : " + finder.getTextRectangle().getWidth());
            LOGGER.debug("Text Height : " + finder.getTextRectangle().getHeight());
            LOGGER.debug("Type Density (avg) : " + PdfUtils.getAverageTypeDensity(page.getLines()) + " characters/inch");
            LOGGER.debug("Left Margin : " + page.getMargin().getMarginLeft() + addIn);
            LOGGER.debug("Right Margin : " + page.getMargin().getMarginRight() + addIn);
            LOGGER.debug("Top Margin : " + page.getMargin().getMarginTop() + addIn);
            LOGGER.debug("Bottom Margin : " + page.getMargin().getMarginBottom() + addIn);
            LOGGER.debug("No of Text Lines Read: " + page.getLines().size());
            LOGGER.debug("Columns Detected: " + PdfUtils.getColumnsPresent(page.getLines()));
            LOGGER.debug("Heading Candidates Detected: "
                    + (part.getSectionHeadings() == null || part.getSectionHeadings().isEmpty() ? "None"
                            : StringUtils.collectionToDelimitedString(
                                    PdfUtils.getHeadingList(part.getSectionHeadings()), "; ")));
        }
        LOGGER.debug("No of Text Characters Read: " + page.getNoOfTextChars());
        LOGGER.debug("No of Text Characters Read by iText: " + page.getNoOfTextCharsiText());
        StringBuilder fontList = new StringBuilder("Text Fonts : ");
        long charCount = 0;
        if (page.getTextFonts() != null && !page.getTextFonts().isEmpty()) {
            Collections.sort(page.getTextFonts(), new Comparator<FontModel>() {
                @Override
                public int compare(FontModel f1, FontModel f2) {
                    return f1.getName().compareTo(f2.getName());
                }
            });
            for (FontModel font : page.getTextFonts()) {
                charCount += font.getNoOfChars();
                initFontOutput(fontList, font);
            }
        }
        page.setNoOfTextChars(charCount); // More accurate then relying on a
                                          // regex expression
        initUserUnitCheck(userUnit);
        showFontImageUrlOutput(page, fontList);
        return page;

    }

    public static StringBuilder addFontPercentage(PageModel page, int freq, long occurrenceCount, long noOfChars) {
        StringBuilder fontPercentList = new StringBuilder();
        if (occurrenceCount == freq) {
            fontPercentList.append(" (");
            DecimalFormat df = new DecimalFormat("#.#####");
            df.setRoundingMode(RoundingMode.HALF_DOWN);
            double percent = ((double) noOfChars / page.getNoOfTextChars()) * 100;
            fontPercentList.append(df.format(percent) + "%");
            fontPercentList.append("); ");
        }
        return fontPercentList;
    }

    @SuppressWarnings({ "squid:S00117" })
    public static StringBuilder createFontPercentList(PageModel page) {
        long noOfChars = 0;
        int freq = 0;
        StringBuilder fontPercentList = new StringBuilder("Text Fonts (Percentage of Total Characters): ");
        for (FontModel font : page.getTextFonts()) {
            if (fontPercentList.indexOf(font.getName()) < 0) {
                fontPercentList.append(font.getName());
                noOfChars = font.getNoOfChars();
                freq = 1;
            } else {
                noOfChars = noOfChars + font.getNoOfChars();
                freq++;
            }
            long occurrenceCount = page.getTextFonts().stream()
                    .filter(FontModel -> font.getName().equals(FontModel.getName())).count();
            fontPercentList.append(addFontPercentage(page, freq, occurrenceCount, noOfChars).toString());
        }
        return fontPercentList;
    }

    public static void showFontImageUrlOutput(PageModel page, StringBuilder fontList) {

        if (page.getNoOfTextChars() > 0) {
            int superScriptCount = page.getSuperscriptLines() != null ? page.getSuperscriptLines().size() : 0;
            int subScriptCount = page.getSubscriptLines() != null ? page.getSubscriptLines().size() : 0;
            StringBuilder fontPercentList = createFontPercentList(page);
            if (superScriptCount > 0) {
                StringBuilder bldr = PdfUtils.getSuperscriptLines(page);
                LOGGER.debug("Superscript line (count): " + bldr.toString().substring(0, bldr.toString().length() - 2));
            }
            if (subScriptCount > 0) {
                StringBuilder bldr = PdfUtils.getSubscriptLines(page);
                LOGGER.debug("Subscript line (count): " + bldr.toString().substring(0, bldr.toString().length() - 2));
            }
            LOGGER.debug(fontList.toString().substring(0, fontList.toString().length() - 2));
            LOGGER.debug(fontPercentList.toString().substring(0, fontPercentList.toString().length() - 2));
            StringBuilder unusedFontList = new StringBuilder("Non-Text Fonts : ");
            for (FontModel font : page.getNonTextFonts()) {
                unusedFontList.append(font.getName());
                unusedFontList.append("; ");
            }
            LOGGER.debug(page.getNonTextFonts().isEmpty() ? unusedFontList.append("None").toString()
                    : unusedFontList.toString().substring(0, unusedFontList.toString().length() - 2));
        }

        showImageOutput(page);

        showURLOutput(page);

        if (page.getNoOfTextChars() > 0 && fontList.toString().indexOf('*') > -1) {
            LOGGER.debug("");
            LOGGER.debug("*Point sizes for this font may not be accurate.  Abnormally large sizes may indicate a watermark");
        }
    }

    public static void showImageOutput(PageModel page) {
        if (!page.getImages().isEmpty()) {
            LOGGER.debug("Image count = " + page.getImages().size());
            LOGGER.debug("Images:");
            int l = 0;
            for (ImageModel img : page.getImages()) {
                l++;
                LOGGER.debug("    " + l + ") Filter = " + img.getFilterUsed() + "; Size = " + img.getSize() + " bytes"
                        + "; Height = " + img.getHeight() + " pixels ; Width = " + img.getWidth()
                        + " pixels: Bit Type = " + img.getBitType() + "; X Coordinate = " + img.getXCoordinate()
                        + " pt; Y Coordinate = " + img.getYCoordinate() + " pt");
            }
        }
    }

    public static void showURLOutput(PageModel page) {
        if (page.getUrls() != null && !page.getUrls().isEmpty()) {
            LOGGER.debug("URLs = " + page.getUrls().toString().replaceAll("\\[|\\]|[,][ ]", "; ")
                    .substring(1, page.getUrls().toString().length()).trim());
        }
    }

    public static void cleanUpFileObjects(PdfReader reader, InputStream inputStream) throws CommonUtilException {
        try {
            if (reader != null)
                reader.close();
            if (inputStream != null)
                inputStream.close();
        } catch (IOException e) {
            throw new CommonUtilException(e);
        }
    }
    
    public static String getXmpTagValue(PdfDocument doc, String tagName) {
        
        try {
            byte[] xmlMetadata = doc.getXmpMetadata();
            if(xmlMetadata != null) {
                String xmlMetadataStr = new String(doc.getXmpMetadata());
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setFeature(Constants.XML_SCRIPT_INJECTION_PREVENTION_FEATURE, true);
                dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document xmlDoc = db.parse(new InputSource(new StringReader(xmlMetadataStr)));
                NodeList nodes = xmlDoc.getElementsByTagName(tagName);
                if(nodes.getLength() > 0 
                      && !StringUtils.isEmpty(nodes.item(0).getTextContent())) {
                           return nodes.item(0).getTextContent().trim();
                } else {
                    return "";
                }
            } else {
                return "";
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            LOGGER.debug(e.getMessage(), e);
            return "";
        }
    }
    
    public static int initDocumentMetadataOutput(String fileName, PdfDocument doc, long sizeInBytes, float fileSize) {
        int noOfPages = doc.getNumberOfPages();
        LOGGER.debug("File name: " + fileName);
        LOGGER.debug("Actual size (bytes): " + sizeInBytes);
        LOGGER.debug("Calculated size (MB): " + fileSize);
        LOGGER.debug("No. of pages: " + noOfPages);
        if(doc.getDocumentInfo() != null) {
              LOGGER.debug("Author: " + (!StringUtils.isEmpty(doc.getDocumentInfo().getAuthor())?doc.getDocumentInfo().getAuthor():Constants.LOGGING_EMPTY_DATA_PLACEHOLDER));
              LOGGER.debug("Title: " + (!StringUtils.isEmpty(doc.getDocumentInfo().getTitle())?doc.getDocumentInfo().getTitle():Constants.LOGGING_EMPTY_DATA_PLACEHOLDER));
        }
        LOGGER.debug("PDF/A: " + !StringUtils.isEmpty(getXmpTagValue(doc, Constants.XMP_METADATA_PDFA_TAG)));
        return noOfPages;
    }
    
    public static void displayFileSourceOutput(DocumentModel document) {
        LOGGER.debug("File producer: "
                + (!StringUtils.isEmpty(document.getProducer()) ? document.getProducer() : Constants.LOGGING_EMPTY_DATA_PLACEHOLDER));
        LOGGER.debug("PDF version: " + document.getPdfVersion());
    }
    
    public static void displayParameterSettingOutput(PdfParameters params) {
        LOGGER.debug("");
        LOGGER.debug("Service Settings");
        LOGGER.debug(Constants.CODE_BLOCK_SEPARATOR);
        LOGGER.debug("Blank spaces ignored: " + params.getFontDetectionIgnoreBlankSpaces());
        LOGGER.debug(
                "Superscript and subscript font sizes ignored: " + params.getFontDetectionIgnoreSuperSubscript());
        LOGGER.debug("Ignored special character codes (regex): " + params.getSpecialCharacters());
        LOGGER.debug("Font family filters: " + (params.getFontMap() != null && !params.getFontMap().isEmpty()?StringUtils.collectionToDelimitedString(Arrays.asList(params.getFontMap().keySet().toArray()),", "):""));
        LOGGER.debug("Logging text extraction from listener: " + !params.getUseTextExtractor());
    }
    
    public static void displayBeginParsingMessage() {
        LOGGER.debug("");
        LOGGER.debug("BEGIN DOCUMENT PARSING");
    }
    
    public static void displayEndParsingMessage() {
        LOGGER.debug("");
        LOGGER.debug("END DOCUMENT PARSING");
    }
    
    public static void displaySuccessMessage() {
        LOGGER.debug("");
        LOGGER.debug("File stream read successfully");
        LOGGER.debug("");
    }
    
    private static void initFontOutput(StringBuilder fontList, FontModel font) {
        fontList.append(font.getName());
        if (font.getCompatibleLibraryAvailableOnServer() == FontModel.LIBRARY_AVAILABLE_FALSE
                && !font.isEmbedded())
            fontList.append("* ");
        fontList.append(" (");
        fontList.append((font.isEmbedded() ? "Embedded" : "Non-Embedded")
                + (StringUtils.isEmpty(font.getType()) ? "" : " " + font.getType()) + ": "
                + (((int) font.getSize()) < 1 ? " Font size cannot be determined" : font.getSize() + " pt"));
        fontList.append("); ");
    }
    
    private static void initUserUnitCheck(PdfNumber userUnit) {
        if(userUnit != null) {
            LOGGER.debug("User Unit (Not Supported): " + userUnit.floatValue());
        }
    }

}
