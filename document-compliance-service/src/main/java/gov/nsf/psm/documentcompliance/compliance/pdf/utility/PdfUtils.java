package gov.nsf.psm.documentcompliance.compliance.pdf.utility;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.TextMarginFinder;

import gov.nsf.psm.documentcompliance.compliance.common.utility.Constants;
import gov.nsf.psm.documentcompliance.compliance.pdf.PdfExtractionListener;
import gov.nsf.psm.foundation.exception.CommonUtilException;
import gov.nsf.psm.foundation.model.compliance.doc.FontModel;
import gov.nsf.psm.foundation.model.compliance.doc.LineModel;
import gov.nsf.psm.foundation.model.compliance.doc.MarginModel;
import gov.nsf.psm.foundation.model.compliance.doc.PageModel;
import gov.nsf.psm.foundation.model.compliance.doc.SectionModel;

public class PdfUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfUtils.class);

    private PdfUtils() {
        // Private constructor
    }

    /**
     * Extracts the font names from page or XObject resources.
     * 
     * @param set
     *            the set with the font names
     * @param resources
     *            the resources dictionary
     * @throws IOException
     * @throws DocumentException
     */
    public static void processResource(List<FontModel> list, PdfDictionary resource) throws IOException {
        if (resource == null)
            return;
        PdfDictionary xobjects = resource.getAsDictionary(PdfName.XObject);
        if (xobjects != null) {
            for (PdfName key : xobjects.keySet()) {
                processResource(list, xobjects.getAsDictionary(key));
            }
        }
        PdfDictionary fonts = resource.getAsDictionary(PdfName.Font);
        if (fonts == null)
            return;
        PdfDictionary fontDict;
        for (PdfName key : fonts.keySet()) {
            FontModel font = new FontModel();
            fontDict = fonts.getAsDictionary(key);
            String name = fontDict.getAsName(PdfName.BaseFont).toString();
            name = name.substring(1);
            font.setName(name);
            if (!list.contains(font)) {
                list.add(font);
            }
        }

    }

    /**
     * Extracts the margins from a page.
     * 
     * @param set
     *            the set with the font names
     * @param resources
     *            the resources dictionary
     * @throws IOException
     * @throws DocumentException
     */
    public static PageModel processResource(TextMarginFinder finder, Rectangle rect, double topLineSpacing, float lastBottomY) {
        if (finder == null)
            return null;
        MarginModel margin = new MarginModel();
        PageModel page = new PageModel();
        page.setHeight(rect.getHeight());
        page.setWidth(rect.getWidth());
        double left = finder.getTextRectangle().getLeft();
        double right = finder.getTextRectangle().getRight();
        double rm = rect.getWidth() - right;
        margin.setMarginLeft(left);
        margin.setMarginRight(Math.abs(rm));
        double topMargin = Math.abs(rect.getHeight() - finder.getTextRectangle().getTop());
        double correctedTopMargin = topMargin + topLineSpacing;
        if(topMargin < correctedTopMargin) { // Check for incorrect font size
            topMargin = correctedTopMargin;
        }
        margin.setMarginTop(topMargin);
        margin.setMarginBottom(lastBottomY); // Use baseline lower Y since getBottom() not always accurate
        page.setMargin(margin);
        return page;
    }

    public static float getDistance(Vector x0, Vector x1, Vector x2) {
        return (x2.subtract(x1)).cross(x1.subtract(x0)).lengthSquared() / x2.subtract(x1).lengthSquared();
    }

    // Handle font type information
    public static FontModel checkFont(FontModel font, PdfDictionary fontDict) {
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] allFonts = e.getAllFonts();
        PdfDictionary desc = fontDict.getAsDictionary(PdfName.FontDescriptor);
        if (desc == null) {
            font.setHasFontDescriptor(false);
            font.setEmbedded(false);
        } else if (desc.get(PdfName.FontFile) != null) {
            font.setHasFontDescriptor(true);
            font.setEmbedded(true);
            font.setType(FontModel.TYPE_ONE);
        } else if (desc.get(PdfName.FontFile2) != null) {
            font.setHasFontDescriptor(true);
            font.setEmbedded(true);
            font.setType(FontModel.TRUE_TYPE);
        } else if (desc.get(PdfName.FontFile3) != null) {
            font.setHasFontDescriptor(true);
            font.setEmbedded(true);
        }
        if (!font.isHasFontDescriptor()) {
            boolean hasLibrary = false;
            for (Font f : allFonts) {
                String internalLibName = f.getFontName().trim();
                String internalPSLibName = f.getPSName().trim();
                internalLibName = internalLibName.replaceAll(" ", "").replaceAll("-", "");
                internalPSLibName = internalPSLibName.replaceAll(" ", "").replaceAll("-", "");
                String compareName = font.getName();
                compareName = compareName.replaceAll(" ", "").replaceAll("-", "");
                if (compareName.equalsIgnoreCase(internalLibName) || compareName.equalsIgnoreCase(internalPSLibName)) {
                    hasLibrary = true;
                }
            }
            if (hasLibrary) {
                font.setCompatibleLibraryAvailableOnServer(FontModel.LIBRARY_AVAILABLE_TRUE);
            } else {
                font.setCompatibleLibraryAvailableOnServer(FontModel.LIBRARY_AVAILABLE_FALSE);
            }
        } else {
            font.setCompatibleLibraryAvailableOnServer(FontModel.LIBRARY_AVAILABLE_NA);
        }
        return font;
    }
    
    public static List<String> getTextLinesFromLines(List<LineModel> lines, boolean sortedByYPos) throws CommonUtilException {
        List<String> textList = new ArrayList<>();
        if(sortedByYPos) {
            try {
                Collections.sort(lines, new Comparator<LineModel>() {
                    @Override
                    public int compare(LineModel lm1, LineModel lm2){
                        return Comparator.comparingInt(LineModel::getColBlockNo).reversed()
                                  .thenComparing(LineModel::getLowerY).reversed()
                                  .compare(lm1,lm2);
                    }
                });
            } catch (Exception e) {
                throw new CommonUtilException(e);
            }
        }
        try {
            textList = lines.stream().map(LineModel::getText).collect(Collectors.toList());
        } catch (Exception e) {
            throw new CommonUtilException(e);
        }
        return textList;
    }

    public static List<String> getTextLinesFromText(String text) throws CommonUtilException {
        List<String> textList = new ArrayList<>();
        try {
            String[] textArray;
            if (!StringUtils.isEmpty(text)) {
                textArray = text.split("\\n");
                textList = Arrays.asList(textArray);
            }
        } catch (Exception e) {
            throw new CommonUtilException(e);
        }
        return textList;
    }
    
    public static String getTextFromTextLines(List<String> linesOfText) {
        String pageText = "";
        if (linesOfText != null && !linesOfText.isEmpty()) {
            pageText = Joiner.on(Constants.TEXT_LINE_SEPARATOR).join(linesOfText);
        }
        return pageText;
    }

    public static boolean isHeading(String text) {
        boolean isHeading = false;
        final Pattern secPattern = Pattern.compile(Constants.HEADING_REGEX_CHAR_INCLUDE);
        String matcherText = text.trim().replaceAll(Constants.HEADING_CHAR_EXCLUDE, "");
        Matcher matcher = secPattern.matcher(matcherText);
        while (matcher.find()) {
            int matchStart = matcher.start(1);
            int matchEnd = matcher.end();
            if (!StringUtils.isEmpty(matcherText.substring(matchStart, matchEnd))
                    && getWordCount(matcherText) <= Constants.HEADING_MAX_WORD_COUNT) {
                isHeading = true;
            }
        }
        return isHeading;
    }

    public static List<Double> getPredominantLeading(PdfExtractionListener info) {
        List<Double> predominantLeadingValues = new ArrayList<>();
        List<Double> uniqueLeadingValues = info.getLeadingValues();
        Multimap<Integer, Double> frequencyMap = HashMultimap.create();
        Collections.sort(uniqueLeadingValues);
        Iterator<Double> iter = uniqueLeadingValues.iterator();
        int freq = 0;
        while (iter.hasNext()) {
            Double leading = iter.next();
            LineModel line = new LineModel();
            line.setLeading(leading);
            freq = Collections.frequency(info.getLines(), line);
            frequencyMap.put(freq, leading);
        }
        if (frequencyMap.size() > 0) {
            int max = Collections.max(frequencyMap.keySet());
            predominantLeadingValues.addAll(frequencyMap.get(max));
        }
        return predominantLeadingValues;
    }

    public static List<String> findAllTextUrls(String text) {

        final Pattern urlPattern = Pattern.compile(
                "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                        + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

        List<String> urls = new ArrayList<>();
        Matcher matcher = urlPattern.matcher(text);
        while (matcher.find()) {
            int matchStart = matcher.start(1);
            int matchEnd = matcher.end();
            urls.add(text.substring(matchStart, matchEnd));
        }

        return urls;

    }

    public static List<String> findAllLinks(PdfDictionary dict) {
        List<String> urls = new ArrayList<>();
        PdfArray annots = dict.getAsArray(PdfName.Annots);

        // Loop through each annotation
        if (annots != null) {
            for (PdfObject a : annots) {
                if (a.isDictionary()) {
                    PdfDictionary aDict = (PdfDictionary) a;
                    String url = getURL(aDict);
                    updateUrls(urls, url);
                }
            }
        }

        return urls;

    }

    public static List<String> correctUrlList(List<String> urls, List<String> nonLinkUrls) {
        if (!nonLinkUrls.isEmpty()) {
            for (String url : nonLinkUrls) {
                if (!urls.contains(url.trim())) {
                    urls.add(url);
                }
            }
        }
        return urls;
    }

    public static String getURL(PdfDictionary aDict) {
        String url = null;
        if (aDict.get(PdfName.Subtype).equals(PdfName.Link) && aDict.get(PdfName.A) != null) {
            try {
                PdfDictionary action = (PdfDictionary) aDict.get(PdfName.A);
                if(action.get(PdfName.URI) != null) {
                  url = action.get(PdfName.URI).toString();
                }
            } catch (Exception e) {
                LOGGER.debug("Error reading URL: " + url);
                LOGGER.debug(e.getMessage(), e);
            }
        }
        return url;
    }

    public static List<FontModel> getUnusedFonts(PdfExtractionListener info, List<FontModel> allFonts) {
        List<FontModel> unusedFonts = new ArrayList<>();
        for (FontModel font : allFonts) {
            boolean fontNamesMatch = false;
            for (FontModel textFont : info.getFonts()) {
                if (textFont.getName().trim().equalsIgnoreCase(font.getName().trim())) {
                    fontNamesMatch = true;
                }
            }
            if (!fontNamesMatch
                    && !unusedFonts.stream().filter(f -> f.getName().equals(font.getName())).findAny().isPresent()) {
                unusedFonts.add(font);
            }
        }
        return unusedFonts;
    }

    public static PdfExtractionListener getPredominantFont(PdfExtractionListener info, List<Double> leadingValues) {
        for (LineModel line : info.getLines()) {
            if (!leadingValues.isEmpty() && line.getLeading() != null
                    && Double.doubleToLongBits(line.getLeading().doubleValue()) == Double
                            .doubleToLongBits(leadingValues.get(0).doubleValue())) {
                FontModel font = line.getFonts().get(0);
                if (info.getFonts().contains(font)) {
                    info.getFonts().remove(font);
                    font.setPredominantFont(true);
                    info.getFonts().add(font);
                }
            }
        }
        return info;
    }

    public static int getWordCount(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    public static StringBuilder getSuperscriptLines(PageModel page) {
        StringBuilder bldr = new StringBuilder();
        for (LineModel line : page.getSuperscriptLines()) {
            long numberOfOccurences = page.getSuperscriptLines().stream()
                    .filter(number -> number.getNumber() == line.getNumber()).count();
            String lineStr = line.getNumber() + " (" + numberOfOccurences + "); ";
            if (bldr.indexOf(lineStr) < 0)
                bldr.append(lineStr);
        }
        return bldr;
    }

    public static StringBuilder getSubscriptLines(PageModel page) {
        StringBuilder bldr = new StringBuilder();
        for (LineModel line : page.getSubscriptLines()) {
            long numberOfOccurences = page.getSubscriptLines().stream()
                    .filter(number -> number.getNumber() == line.getNumber()).count();
            String lineStr = line.getNumber() + " (" + numberOfOccurences + "); ";
            if (bldr.indexOf(lineStr) < 0)
                bldr.append(lineStr);
        }
        return bldr;
    }

    public static String correctFileName(String fileName) throws CommonUtilException {
        String fileNameCorrected = fileName.replace('\'', ' ').trim();
        String ext = Files.getFileExtension(fileNameCorrected);
        String fileNameWithExt = null;
        if (ext.length() < 1) {
            fileNameWithExt = fileNameCorrected + ".pdf";
        } else if (!("pdf").equals(ext)) {
            throw new CommonUtilException("Wrong file extension");
        }
        return fileNameWithExt;
    }

    public static FontModel setIgnoreSuperSubScriptFont(FontModel font) {
        if (font.getSize() >= Constants.MINIMUM_SUPERSUBSCRIPT_PT_SIZE) {
            font.setIgnore(true);
        }
        return font;
    }

    public static String cleanUpFontName(String fontName) {
        if (fontName.indexOf('-') > -1) {
            return fontName.substring(0, fontName.indexOf('-'));
        }
        return fontName;
    }

    public static List<String> getHeadingList(List<SectionModel> headings) {
        List<String> headingList = new ArrayList<>();
        for (SectionModel model : headings) {
            if (model.getHeading() != null) {
                headingList.add("'" + model.getHeading().getValue() + "'");
            }
        }
        return headingList;
    }

    public static int getLineNumber(List<LineModel> lines) {
        return lines.size() + 1;
    }

    public static String getPdfVersion(String versionStr) {
        return versionStr.substring(versionStr.lastIndexOf('-') + 1, versionStr.length());
    }

    public static float getFontSize(TextRenderInfo renderInfo) {
        float fontSize = renderInfo.getTextMatrix().get(Matrix.I11); // Check for TM value
        float ctmValue = renderInfo.getGraphicsState().getCtm().get(Matrix.I11);
        float ctmCheckValue = renderInfo.getGraphicsState().getCtm().get(Matrix.I22);
        BigDecimal ctmValueDec = BigDecimal.valueOf(ctmValue);
        BigDecimal ctmCheckValueDec = BigDecimal.valueOf(ctmCheckValue);
        BigDecimal fontSizeDec = BigDecimal.valueOf(Math.round(fontSize));
        if(ctmValueDec.compareTo(ctmCheckValueDec) == 0 && fontSizeDec.compareTo(BigDecimal.ONE) <= 0) { // Correct for situation where transformation value is misplaced
            ctmValue = fontSize;
        }
        if (fontSizeDec.compareTo(BigDecimal.ONE) <= 0) { // Get TF value if TM equals 1
            fontSize = renderInfo.getFontSize();
        }
        fontSizeDec = BigDecimal.valueOf(Math.round(fontSize)); // Check again in case of rotation for some producers
        if (fontSizeDec.compareTo(BigDecimal.ONE) <= 0) {
            fontSize = renderInfo.getTextMatrix().get(Matrix.I12);
        }
        if(ctmValueDec.compareTo(BigDecimal.ZERO) >= 1 && ctmValueDec.compareTo(BigDecimal.ONE) <= 0) {
            fontSize = ctmValue * fontSize; // Apply a CM transformation, if available
        }
        return fontSize;
    }
    
    public static boolean isTextRotated(TextRenderInfo renderInfo) {
        if(renderInfo.getAscentLine().getStartPoint().get(1) < renderInfo.getDescentLine().getEndPoint().get(1)) {
            return true;
        }
        return false;
    }
    
    public static boolean characterMatches(String specialCharacters, String input) {
        Pattern p = Pattern.compile(specialCharacters);
        Matcher m = p.matcher(input);
        if (m.find()) {
            return true;
        }
        return false;
    }
    
    public static double getLineSpacing(double leading, double fontSize) {
        double topLineSpacing = 0.0;
        if(leading > 0.0) {
           topLineSpacing = leading - fontSize;
        }
        return topLineSpacing;
    }
    
    public static double getTypeDensity(String currentLineText, float lineWidth) {
        return new Double(currentLineText.chars().count()/(lineWidth/Constants.LINEAR_INCH_EQUIVALENT_IN_POINTS));
    }
    
    public static double getAverageTypeDensity(List<LineModel> lines) {
       return lines.stream().mapToDouble(l -> l.getTypeDensity()).average().getAsDouble();
    }
    
    public static double getTopLeading(List<Double> leadingValues, List<LineModel> lines) {
        double topLeading = 0.0d;
        if(!leadingValues.isEmpty() && !lines.isEmpty()) {
            topLeading = PdfUtils.getLineSpacing(leadingValues.get(0), lines.get(0).getFonts().get(0).getSize());
        }
        return topLeading;
    }
    
    public static float getLastY(List<LineModel> lines) {
        float lastY = 0.0f;
        Collections.sort(lines, new Comparator<LineModel>() {
            @Override
            public int compare(LineModel l1, LineModel l2) {
                return l1.getLowerY().compareTo(l2.getLowerY());
            }
        });
        if(!lines.isEmpty()) {
            lastY = lines.get(0).getLowerY();
        }
        return lastY;
    }
    
    public static boolean getColumnsPresent(List<LineModel> lines) {
        return lines.stream().filter(l->l.getColBlockNo() > 1).count() > 0;
    }
    
    public static float getAscentDescentDifference(TextRenderInfo renderInfo) {
        return renderInfo.getAscentLine().getStartPoint().subtract(renderInfo.getDescentLine().getStartPoint())
                .length();
    }
    
    private static void updateUrls(List<String> urls, String url) {
        if (url != null) {
            urls.add(url);
        }
    }

}