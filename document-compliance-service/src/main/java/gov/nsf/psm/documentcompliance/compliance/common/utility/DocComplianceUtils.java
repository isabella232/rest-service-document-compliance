package gov.nsf.psm.documentcompliance.compliance.common.utility;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.Part;

import org.apache.catalina.core.ApplicationPart;
import org.apache.cxf.common.util.StringUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.google.common.base.Splitter;

import gov.nsf.psm.documentcompliance.model.builder.pdf.PdfModelBuilder;
import gov.nsf.psm.documentcompliance.model.builder.ss.SpreadsheetModelBuilder;
import gov.nsf.psm.documentcompliance.service.parameter.PdfParameters;
import gov.nsf.psm.documentcompliance.service.parameter.SpreadsheetParameters;
import gov.nsf.psm.factmodel.FontFactModel;
import gov.nsf.psm.factmodel.HeadingFactModel;
import gov.nsf.psm.factmodel.MarginFactModel;
import gov.nsf.psm.factmodel.PageFactModel;
import gov.nsf.psm.factmodel.SectionFactModel;
import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.ComplianceModel;
import gov.nsf.psm.foundation.model.compliance.doc.DocumentModel;
import gov.nsf.psm.foundation.model.compliance.doc.FontModel;
import gov.nsf.psm.foundation.model.compliance.doc.PageModel;
import gov.nsf.psm.foundation.model.compliance.doc.SectionModel;
import gov.nsf.psm.foundation.model.compliance.ss.SpreadsheetModel;

public class DocComplianceUtils {

    private DocComplianceUtils() {
    }

    public static String getMimeType(ApplicationPart part) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(part.getInputStream());
        TikaConfig config = TikaConfig.getDefaultConfig();
        Detector detector = config.getDetector();
        TikaInputStream stream = TikaInputStream.get(bis);
        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, part.getName());
        MediaType mediaType = detector.detect(stream, metadata);
        String mimeType = mediaType.toString();
        bis.close();
        return mimeType;
    }

    public static PageFactModel getPageFactModel(PageModel page, Map<String, String> fontMap) {
        PageFactModel pageFactModel = new PageFactModel();
        pageFactModel.setHeight(page.getHeight());
        pageFactModel.setLeading(page.getLeadingValues());
        pageFactModel.setTextLines(page.getTextLines());
        pageFactModel.setPageNumber(page.getPageNumber());
        MarginFactModel marginFactModel = new MarginFactModel();
        marginFactModel.setMarginLeft(page.getMargin().getMarginLeft());
        marginFactModel.setMarginRight(page.getMargin().getMarginRight());
        marginFactModel.setMarginTop(page.getMargin().getMarginTop());
        marginFactModel.setMarginBottom(page.getMargin().getMarginBottom());
        pageFactModel.setMargin(marginFactModel);
        List<FontFactModel> fonts = new ArrayList<>();
        for (FontModel model : page.getTextFonts()) {
            if (!model.isIgnore()) {
                FontFactModel fontFactModel = new FontFactModel();
                fontFactModel.setName(getMatchingFontFamily(model.getName(), fontMap));
                fontFactModel.setSize(model.getSize());
                fontFactModel.setType(model.getType());
                fonts.add(fontFactModel);
            }
        }
        pageFactModel.setTextFonts(fonts);
        pageFactModel.setWidth(page.getWidth());
        return pageFactModel;
    }

    public static List<SectionFactModel> getSectionFactModelList(List<SectionModel> sections) {
        List<SectionFactModel> sectionFMs = new ArrayList<>();
        for (SectionModel sectionModel : sections) {
            SectionFactModel sectionFactModel = new SectionFactModel();
            sectionFactModel.setHeading(new HeadingFactModel(sectionModel.getHeading().getValue()));
            sectionFMs.add(sectionFactModel);
        }
        return sectionFMs;
    }

    public static String getOrigFileName(Part part) throws IOException {
        String fileName = "";
        String disposition = part.getHeader("Content-Disposition");
        fileName = disposition.replaceFirst("(?i)^.*filename=\"([^\"]+)\".*$", "$1");

        return fileName;
    }

    public static float convertFileSizeFromBytesToMB(long sizeInBytes) {
        return ((float) sizeInBytes / 1024) / 1024;
    }

    public static ComplianceModel getDocumentMetadata(ComplianceModel compliance, List<String> mimeTypes,
            String mimeType, ApplicationPart filePart) throws CommonUtilException {
        if (mimeTypes.indexOf(mimeType) > -1) {
            PdfModelBuilder builder = new PdfModelBuilder();
            InputStream inputStream;
            try {
                inputStream = filePart.getInputStream();
                DocumentModel document = builder.buildMetadata(inputStream, getFileName(filePart), filePart.getSize());
                compliance.setDocModel(document);
                compliance.setCorrectMimeType(true);
            } catch (IOException e) {
                throw new CommonUtilException(e);
            }
        }
        return compliance;
    }

    public static ComplianceModel getDocumentModel(ComplianceModel compliance, List<String> mimeTypes,
            ApplicationPart filePart, PdfParameters params) throws CommonUtilException {
        DocumentModel document = null;
        if (mimeTypes.indexOf(compliance.getMimeType()) > -1) {
            PdfModelBuilder builder = new PdfModelBuilder(params);
            InputStream inputStream;
            try {
                inputStream = filePart.getInputStream();
                document = builder.buildModel(inputStream, getFileName(filePart), filePart.getSize());
                compliance.setCorrectMimeType(true);
            } catch (IOException e) {
                throw new CommonUtilException(e);
            }
        }
        if (document != null) {
            compliance.setDocModel(document);
        } else {
            document = new DocumentModel();
            compliance.setDocModel(document);
        }
        return compliance;
    }

    public static ComplianceModel getSpreadsheetMetadata(ComplianceModel compliance, List<String> mimeTypes,
            String mimeType, ApplicationPart filePart) throws CommonUtilException {
        if (mimeTypes.indexOf(mimeType) > -1) {
            SpreadsheetModelBuilder xlsBuilder = new SpreadsheetModelBuilder();
            SpreadsheetModel ssModel;
            try {
                ssModel = xlsBuilder.buildMetadata(filePart.getInputStream(), getFileName(filePart),
                        filePart.getSize());
                compliance.setSsModel(ssModel);
                compliance.setCorrectMimeType(true);
            } catch (Exception e) {
                throw new CommonUtilException(e);
            }
        }
        return compliance;
    }

    public static ComplianceModel getSpreadsheetModel(ComplianceModel compliance, List<String> mimeTypes,
            String mimeType, List<String> nonTextColumns, ApplicationPart filePart, SpreadsheetParameters params, boolean isTablesOnly)
            throws CommonUtilException {
        if (mimeTypes.indexOf(mimeType) > -1) {
            SpreadsheetModelBuilder xlsBuilder = new SpreadsheetModelBuilder(isTablesOnly, nonTextColumns, params);
            SpreadsheetModel ssModel;
            try {
                ssModel = xlsBuilder.buildModel(filePart.getInputStream(), getFileName(filePart), filePart.getSize());
                compliance.setSsModel(ssModel);
                compliance.setCorrectMimeType(true);
            } catch (Exception e) {
                throw new CommonUtilException(e);
            }
        }
        return compliance;
    }

    public static boolean getIsTablesOnly(ApplicationPart part) throws CommonUtilException {
        boolean isTablesOnly = false;
        try {
            isTablesOnly = Boolean.parseBoolean(part.getString(Constants.CHARACTER_SET_DEFAULT));
        } catch (UnsupportedEncodingException e) {
            throw new CommonUtilException(e);
        }
        return isTablesOnly;
    }

    public static List<String> getParams(ApplicationPart part) throws CommonUtilException {
        List<String> params = new ArrayList<>();
        try {
            params.add(part.getString(Constants.CHARACTER_SET_DEFAULT));
        } catch (UnsupportedEncodingException e) {
            throw new CommonUtilException(e);
        }
        return params;
    }

    public static String getFileName(ApplicationPart filePart) {
        return filePart.getName().equalsIgnoreCase(Constants.PROGRAMMATIC_FILE_PLACEHOLDER)
                ? filePart.getSubmittedFileName()
                : filePart.getName();
    }

    public static List<String> convertToList(String str) {
        if(!StringUtils.isEmpty(str)) {
            return Arrays.asList(str.split("\\s*,\\s*"));
        } else {
            return new ArrayList<>();
        }
    }
    
    public static Map<String, String> convertToMap(String str) {
        if(!StringUtils.isEmpty(str)) {
            return Splitter.on(",").withKeyValueSeparator(Constants.DEFAULT_FONT_NAME_SEPARATOR).split(str);
        } else {
            return new HashMap<String,String>();
        }
    }
    
    public static String getMatchingFontFamily(String fontName, Map<String, String> fontMap) {
       int idx = fontName.indexOf(Constants.DEFAULT_FONT_NAME_SEPARATOR);
       String fontNameMatch = null;
       String fontFamily = null;
       if(idx > -1) {
          fontNameMatch = fontName.substring(idx+1, fontName.length());
       } else {
          fontNameMatch = fontName;
       }
       for(Map.Entry<String, String> fontKey: fontMap.entrySet()) {
           if(fontNameMatch.indexOf(fontKey.getKey()) == 0) {
               fontFamily = fontMap.get(fontKey.getKey());
           }
       }
       if(!StringUtils.isEmpty(fontFamily)) {
           return fontFamily;
       } else {
           return fontName;
       }
    }
    
    public static String getMatchingCp850Character(char character) {
        Optional<UnicodeToCp850Enum> result = Arrays.asList(UnicodeToCp850Enum.values()).stream().filter(u -> u.getUnicode().equals(Integer.toHexString(character))).findAny();
        if(result != null && result.isPresent()) {
            return result.get().getCp850Code();
        } else {
            return null;
        }
    }
}
