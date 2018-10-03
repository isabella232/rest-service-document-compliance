package gov.nsf.psm.documentcompliance.compliance.pdf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.itextpdf.kernel.geom.LineSegment;
import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;

import gov.nsf.psm.documentcompliance.compliance.common.utility.Constants;
import gov.nsf.psm.documentcompliance.compliance.pdf.utility.PdfUtils;
import gov.nsf.psm.foundation.model.compliance.doc.FontModel;
import gov.nsf.psm.foundation.model.compliance.doc.HeadingModel;
import gov.nsf.psm.foundation.model.compliance.doc.ImageModel;
import gov.nsf.psm.foundation.model.compliance.doc.LineModel;
import gov.nsf.psm.foundation.model.compliance.doc.SectionModel;

public class PdfExtractionListener implements IEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfExtractionListener.class);

    private boolean lastTextBlockOnSameLine;
    private int colBlockNo = 0;
    private long totalCharacterCount = 0;
    private long lastBlockCount = 0;
    private long totalBlockCount = 0;
    private long lastSupScriptBlockCount = 0;
    private FontModel font = null;
    private boolean isRotated = false;
    private float previousFontSize;
    private float previousBaseline;
    private float avgSingleSpaceWidth;
    private float textWidth = 0.0f;
    private float lineWidth = 0f;
    private double lineHeight;
    private List<LineModel> lines;
    private List<SectionModel> headingLines = new ArrayList<>();
    private List<Double> leadingValues;
    private double nextHeight;
    private double nextMaxY;
    private double prevMaxY;
    private double nextMaxX;
    private double prevMaxX;
    private double nextMinX;
    private double prevMinX;
    private List<FontModel> fonts;
    private List<LineModel> superscriptLines;
    private List<LineModel> subscriptLines;
    private double initLeading = 0.0;
    private Boolean fontDetectionIgnoreBlankSpaces;
    private Boolean fontDetectionIgnoreSuperSubscript;
    private String specialCharacters;
    private StringBuilder lineText = new StringBuilder();
    private int pageNumber;
    private List<Float> nonBlankTextAscentLines = new ArrayList<>();
    private List<Float> nonBlankTextDescentLines = new ArrayList<>();
    private List<ImageModel> images = new ArrayList<>();
    private List<String> textLines = new ArrayList<>();
    private Vector lastStart;
    private Vector lastEnd;
    private String lastSupText;
    private LineSegment prevSegment;

    private void renderText(TextRenderInfo renderInfo) {

        // Initial processing variables
        boolean ignoreBlankSpace = false;
        boolean isSupSubscript = false;
        float fontSize = 0F;

        // Fonts
        if (fonts == null) {
            fonts = new ArrayList<>();
        }

        if (fontDetectionIgnoreBlankSpaces != null && fontDetectionIgnoreBlankSpaces
                && (renderInfo.getText().trim().length() < 1
                        || PdfUtils.characterMatches(specialCharacters, renderInfo.getText()))) {
            ignoreBlankSpace = true;
        }

        if (!ignoreBlankSpace) {
            fontSize = PdfUtils.getFontSize(renderInfo);
            processFont(fontSize, renderInfo);
            populateNonBlankTextAscentLines(renderInfo);
            populateNonBlankTextDescentLines(renderInfo);
        }

        if (Double.doubleToLongBits(nextHeight) == 0 && lines == null) { // Capture
                                                                         // the
                                                                         // first
                                                                         // line
            lines = new ArrayList<>();
        }
        
        // Process superscript/subscript fonts 
        if (font != null) {
            isSupSubscript = processSuperscriptSubscriptFonts(renderInfo, fontSize);
        }
        
        // Check for rotated text
        isRotated = PdfUtils.isTextRotated(renderInfo);
        
        // Process leading (since iText does not always report)
        LineSegment segment = renderInfo.getBaseline();
        Vector curBaseline = segment.getStartPoint();
        processLeading(renderInfo, curBaseline, isSupSubscript);

        // Process Font character count
        processFontCharacterCount(renderInfo);
        
        // Make sure previous segment is populated
        setDefaultPreviousSegment(segment);
        
        // Add line of text
        addDefaultLine(renderInfo, prevSegment.getStartPoint().get(1), prevSegment.getEndPoint().get(1));

        Vector end = segment.getEndPoint();

        correctForMultiColumnsAndNoSpaces(renderInfo, curBaseline);

        lineText.append(renderInfo.getText());
        lineWidth = lineWidth + renderInfo.getBaseline().getLength();
        totalBlockCount++;

        addLastLine(renderInfo, prevSegment.getStartPoint().get(1), prevSegment.getEndPoint().get(1)); // Add last Line

        prevMinX = nextMinX;
        prevMaxX = nextMaxX;
        prevMaxY = nextMaxY;
        lastStart = curBaseline;
        lastEnd = end;
        prevSegment = segment;
        
    }
    
    private void setDefaultPreviousSegment(LineSegment segment) {
        if(prevSegment == null) {
            prevSegment = segment;
        }
    }
    
    private void addDefaultLine(TextRenderInfo renderInfo, float upperY, float lowerY) {
        LineSegment segment = renderInfo.getBaseline();
        if (segment != null) {
            nextMinX = segment.getBoundingRectangle().getLeft();
            nextMaxX = segment.getBoundingRectangle().getRight();
            if (PdfUtils.characterMatches(specialCharacters, renderInfo.getText().trim())) {
                nextMaxX = prevMaxX;
            }
            
            // Need to consider all scenarios for a new line since chunk sizes in PDFs can vary
            
            BigDecimal prevMaxXDec = BigDecimal.valueOf(prevMaxX);
            BigDecimal prevMaxYDec = BigDecimal.valueOf(prevMaxY);
            BigDecimal nextMaxYDec = BigDecimal.valueOf(nextMaxY);
                        
            if (getFirstNewLineCondition(prevMaxYDec, nextMaxYDec) || getSecondNewLineCondition(prevMaxYDec) || getThirdLineCondition(prevMaxXDec)) {
                addLine(upperY, lowerY);
            }
        }
    }
    
    private boolean getFirstNewLineCondition(BigDecimal prevMaxYDec, BigDecimal nextMaxYDec) {
        return prevMaxYDec.compareTo(BigDecimal.ZERO) == 0 && nextMaxYDec.compareTo(BigDecimal.ZERO) > 0 && nextMinX < prevMinX;    
    }
    
    private boolean getSecondNewLineCondition(BigDecimal prevMaxYDec) {
        return prevMaxYDec.compareTo(BigDecimal.ZERO) > 0 && prevMaxY > nextMaxY && nextMinX < prevMinX;
    }
    
    private boolean getThirdLineCondition(BigDecimal prevMaxXDec) {
        return prevMaxXDec.compareTo(BigDecimal.ZERO) > 0 && nextMaxX < prevMaxX;
    }

    private void addLastLine(TextRenderInfo renderInfo, float upperY, float lowerY) {
        LineSegment segment = renderInfo.getBaseline();
        if (segment != null && !PdfUtils.characterMatches(specialCharacters, renderInfo.getText())) {
            if(totalBlockCount == (lastBlockCount - 1) && !lastTextBlockOnSameLine) {
                addLine(upperY, lowerY);
            } else if (totalBlockCount == lastBlockCount) {
                addLine(upperY, lowerY);
            }
        }
    }

    private void correctForMultiColumnsAndNoSpaces(TextRenderInfo renderInfo, Vector start) {

        boolean firstRender = false;
        boolean hardReturn = false;

        firstRender = lineText.length() == 0; // Check for first render
        if (!firstRender) { // Correct for multi-column data (Adapted from iText
            // code)
            Vector x0 = start;
            Vector x1 = lastStart;
            Vector x2 = lastEnd;

            // see
            // http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
            float dist = PdfUtils.getDistance(x0, x1, x2);
            if (dist > nextMaxX) {
                setColumnBlockNo();
                hardReturn = true;
            }
        }

        if (hardReturn) {
            lineText = new StringBuilder();
            // Handle missing space characters (Adapted
            // from iText code)
        } else if (!firstRender && lineText.toString().charAt(lineText.toString().length() - 1) != ' '
                && renderInfo.getText().length() > 0 && renderInfo.getText().charAt(0) != ' ') { // Only
                                                                                                 // insert
                                                                                                 // a
            // blank space
            // if the
            // trailing
            // character of
            // the previous
            // string wasn't
            // a space, and
            // the leading
            // character of
            // the current
            // string isn't
            // a space
            float spacing = lastEnd.subtract(start).length();
            float divisor = getCorrectSpaceWidthDivisor(renderInfo);
            if (spacing > renderInfo.getSingleSpaceWidth() / divisor) {
                lineText.append(" ");
            }
        }

    }
    
    private void setColumnBlockNo() {
        if(nextMaxX < textWidth && nextMaxX >= textWidth/Constants.MAX_NO_OF_COLUMNS_DETECTED) {
            colBlockNo++;
        }
    }
    
    private float getCorrectSpaceWidthDivisor(TextRenderInfo renderInfo) {
        float divisor = Constants.SPACE_CHARACTER_WIDTH_DIVISOR; 
        if(avgSingleSpaceWidth > Constants.SPACE_CHARACTER_WIDTH_THRESHOLD) {
            divisor = renderInfo.getSingleSpaceWidth() * divisor;
        }
        return divisor;
    }

    // Handle non-blank text ascent lines for accurate determination of headers
    private void populateNonBlankTextAscentLines(TextRenderInfo renderInfo) {
        if (!StringUtils.isEmpty(renderInfo.getText()) && renderInfo.getText().trim().length() > 0) {
            nonBlankTextAscentLines.add(new Float(renderInfo.getAscentLine().getStartPoint().get(1)));
        }
    }

    // Handle non-blank text descent lines for accurate determination of footers
    private void populateNonBlankTextDescentLines(TextRenderInfo renderInfo) {
        if (!StringUtils.isEmpty(renderInfo.getText()) && renderInfo.getText().trim().length() > 0) {
            nonBlankTextDescentLines.add(new Float(renderInfo.getDescentLine().getStartPoint().get(1)));
        }
    }

    private void addLine(float upperY, float lowerY) {
        boolean setLineText = true;
        LineModel line = new LineModel();
        line.setLeading(initLeading);
        line.setUpperY(upperY);
        line.setLowerY(lowerY);
        line.setColBlockNo(colBlockNo);
        List<FontModel> lineFonts = new ArrayList<>();
        String currentLineText = lineText.toString().trim();
        if (fonts.isEmpty()) {
            lineFonts.add(font);
            line.setFonts(lineFonts);
        } else if (fonts.indexOf(font) > -1) {
            lineFonts.add(fonts.get(fonts.indexOf(font)));
            line.setFonts(lineFonts);
        } else {
            fonts.add(font);
            lineFonts.add(font);
            line.setFonts(lineFonts);
        }
        if (!StringUtils.isEmpty(currentLineText) && PdfUtils.isHeading(currentLineText)) {
            SectionModel sectionModel = new SectionModel();
            sectionModel.setHeading(new HeadingModel(currentLineText));
            sectionModel.setBeginLine(line.getNumber());
            sectionModel.setBeginPage(pageNumber);
            headingLines.add(sectionModel);
        }
        if (fontDetectionIgnoreBlankSpaces != null && fontDetectionIgnoreBlankSpaces
                && StringUtils.isEmpty(currentLineText)) {
            setLineText = false;
        }
        if (setLineText) {
            line.setNumber(PdfUtils.getLineNumber(lines));
            line.setTypeDensity(PdfUtils.getTypeDensity(currentLineText, lineWidth));
            line.setText(currentLineText);
            lines.add(line);
            textLines.add(currentLineText);
        }
        totalCharacterCount = totalCharacterCount + currentLineText.length();
        lineText = new StringBuilder();
        lineWidth = 0f;
    }

    private void processFontCharacterCount(TextRenderInfo renderInfo) {
        long charCount = fontDetectionIgnoreBlankSpaces != null && fontDetectionIgnoreBlankSpaces
                ? renderInfo.getText().replaceAll(" ", "").length()
                : renderInfo.getText().length();
        if (charCount > 0) {
            FontModel tempFont = null;
            if (fonts.isEmpty()) {
                tempFont = font;
            } else if (fonts.indexOf(font) > -1) {
                tempFont = fonts.get(fonts.indexOf(font));
            } else {
                fonts.add(font);
                tempFont = font;
            }
            if (tempFont != null) {
                tempFont.setNoOfChars(tempFont.getNoOfChars() + charCount);
                if (!fonts.isEmpty() && fonts.indexOf(font) > -1) {
                    fonts.remove(font);
                    fonts.add(tempFont);
                }
            }
        }
    }

    // Will detect most normal superscript and subscript font occurrences
    private boolean processSuperscriptSubscriptFonts(TextRenderInfo renderInfo, float fontSize) {
        boolean isSupSubscript = false;
        LineSegment segment = renderInfo.getBaseline(); 
        float baseline = segment.getStartPoint().get(1);
        double x = segment.getBoundingRectangle().getX();
        if (fontSize > 0.0) {
            isSupSubscript = identifySuperscriptSubscript(fontSize, baseline, x, renderInfo.getText());
            previousFontSize = fontSize;
            previousBaseline = baseline;
        }
        return isSupSubscript;
    }
    
    private boolean identifySuperscriptSubscript(float fontSize, float baseline, double x, String supSubText) {
        boolean isSupSubscript = false;
        if (!isRotated && getPrimarySuperscriptFilter(fontSize, baseline)) {
            processSuperscriptFonts();
            isSupSubscript = true;
            lastSupScriptBlockCount = totalBlockCount;
            lastSupText = supSubText;
        } else if (!StringUtils.isEmpty(lastSupText) 
            && previousFontSize <= (Constants.SUP_SUB_MAXIMUM_FONT_PERCENTAGE * fontSize) 
            && getSecondarySuperscriptFilter(baseline)) {
               int idx = lineText.indexOf(lastSupText);
               if(idx < 1) {
                   processSuperscriptFonts();
                   isSupSubscript = true;
               }
        } else if (getPrimarySubscriptFilter(fontSize, baseline, x)) {
            processSubcriptFonts();
            isSupSubscript = true;
        }
        return isSupSubscript;
    }
    
    private boolean getPrimarySuperscriptFilter(float fontSize, float baseline) {
        return fontSize <= (Constants.SUP_SUB_MAXIMUM_FONT_PERCENTAGE * previousFontSize)
                && baseline > previousBaseline
                && (baseline >= (Constants.SUP_SUB_MINIMUM_BASELINE_PERCENTAGE * previousBaseline));
    }
    
    private boolean getSecondarySuperscriptFilter(float baseline) {
        return getPreviousBaselineCondition(baseline) 
                && getLastBlockCountCondition() 
                && (subscriptLines == null || textLines.size() > subscriptLines.get(subscriptLines.size() - 1).getNumber() + 1);
    }
    
    private boolean getPreviousBaselineCondition(float baseline) {
        return previousBaseline > baseline 
                && (previousBaseline >= (Constants.SUP_SUB_MINIMUM_BASELINE_PERCENTAGE * baseline));
    }
    
    private boolean getLastBlockCountCondition() {
        return totalBlockCount - lastSupScriptBlockCount < Constants.LOWER_SUPERSCRIPT_CORRECTION_BOUNDARY || totalBlockCount - lastSupScriptBlockCount > Constants.UPPER_SUPERSCRIPT_CORRECTION_BOUNDARY;
    }
    
    private boolean getPrimarySubscriptFilter(float fontSize, float baseline, double x) {
        return isRoughlyEqual(x) && fontSize <= (Constants.SUP_SUB_MAXIMUM_FONT_PERCENTAGE * previousFontSize)
                && baseline < previousBaseline
                && (baseline >= (Constants.SUP_SUB_MINIMUM_BASELINE_PERCENTAGE * previousBaseline));
    }
    
    private boolean isRoughlyEqual(double x) {
        return Math.floor(x) >= Math.floor(prevMaxX) || Math.round(x) >= Math.round(prevMaxX);
    }

    private void processSuperscriptFonts() {
        if (superscriptLines == null)
            superscriptLines = new ArrayList<>();
        LineModel line = createLineModelForSuperSubscript();
        superscriptLines.add(line);
    }

    private void processSubcriptFonts() {
        if (subscriptLines == null)
            subscriptLines = new ArrayList<>();
        LineModel line = createLineModelForSuperSubscript();
        subscriptLines.add(line);
    }

    private LineModel createLineModelForSuperSubscript() {
        LineModel line = new LineModel();
        List<FontModel> lineFonts = new ArrayList<>();
        if (getFontDetectionIgnoreSuperSubscript() && font != null) {
            font = PdfUtils.setIgnoreSuperSubScriptFont(font);
        }
        if (fonts.isEmpty()) {
            lineFonts.add(font);
        } else {
            lineFonts.add(fonts.get(fonts.indexOf(font)));
        }
        line.setFonts(lineFonts);
        line.setNumber(PdfUtils.getLineNumber(lines));
        return line;
    }

    private void processLeading(TextRenderInfo renderInfo, Vector curBaseline, boolean isSupSubscript) {
        if (leadingValues == null)
            leadingValues = new ArrayList<>();
        double tempLeading = 0;
        lineHeight = nextHeight;
        nextHeight = curBaseline.get(1);
        processLineHeight(renderInfo, isSupSubscript);
        if (initLeading > 0) {
            tempLeading = initLeading;
        }
        if (tempLeading > 0 && !leadingValues.contains(tempLeading)) {
            leadingValues.add(tempLeading);
        }
    }

    private void processLineHeight(TextRenderInfo renderInfo, boolean isSupSubscript) {
        double maxY;
        if (renderInfo.getText().trim().length() > 0 && nextHeight < lineHeight && !isSupSubscript) { // All text lines
                                                          // except the first
                                                          // one
            maxY = renderInfo.getBaseline().getBoundingRectangle().getY();
            if (Double.doubleToLongBits(nextMaxY) != Double.doubleToLongBits(maxY)) { // Account
                                                                                      // for
                                                                                      // different
                                                                                      // height
                                                                                      // characters
                initLeading = lineHeight - nextHeight;
            }
            nextMaxY = maxY;
        }

    }

    private void processFont(float fontSize, TextRenderInfo renderInfo) {
        BigDecimal fontSizeDec = BigDecimal.valueOf(fontSize);
        if(fontSizeDec.compareTo(BigDecimal.ZERO) > 0) {
            String fontName = renderInfo.getFont().getFontProgram().getFontNames().getFontName();
            font = new FontModel();
            font.setName(fontName);
            font.setSize(fontSize);
            if (!fonts.contains(font)) {
                font = PdfUtils.checkFont(font, renderInfo.getFont().getPdfObject());
                fonts.add(font);
            }
        }
    }

    private void renderImage(ImageRenderInfo renderInfo) {
        try {
            PdfImageXObject image = renderInfo.getImage();
            if (image == null) {
                return;
            }
            PdfDictionary imageDict = image.getPdfObject();
            ImageModel fact = new ImageModel();
            fact.setBitType(Integer.parseInt(imageDict.get(PdfName.BitsPerComponent).toString()));
            fact.setHeight(Long.parseLong(imageDict.get(PdfName.Height).toString()));
            fact.setWidth(Long.parseLong(imageDict.get(PdfName.Width).toString()));
            fact.setSize(image.getImageBytes().length);
            fact.setXCoordinate(renderInfo.getImageCtm().get(Matrix.I31));
            fact.setYCoordinate(renderInfo.getImageCtm().get(Matrix.I32));
            if (imageDict.get(PdfName.Filter) != null) {
                fact.setFilterUsed(imageDict.get(PdfName.Filter).toString().replaceAll("/", ""));
            } else {
                fact.setFilterUsed("N/A");
            }
            images.add(fact);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }
    
    public long getTotalCharacterCount() {
        return totalCharacterCount;
    }

    public List<FontModel> getFonts() {
        return fonts;
    }

    public void setFonts(List<FontModel> fonts) {
        this.fonts = fonts;
    }

    public List<LineModel> getSuperscriptLines() {
        return superscriptLines;
    }

    public void setSuperscriptLines(List<LineModel> superscriptLines) {
        this.superscriptLines = superscriptLines;
    }

    public List<LineModel> getSubscriptLines() {
        return subscriptLines;
    }

    public void setSubscriptLines(List<LineModel> subscriptLines) {
        this.subscriptLines = subscriptLines;
    }

    public Boolean getFontDetectionIgnoreBlankSpaces() {
        return fontDetectionIgnoreBlankSpaces;
    }

    public void setFontDetectionIgnoreBlankSpaces(Boolean fontDetectionIgnoreBlankSpaces) {
        this.fontDetectionIgnoreBlankSpaces = fontDetectionIgnoreBlankSpaces;
    }

    public List<LineModel> getLines() {
        return lines;
    }

    public void setLines(List<LineModel> lines) {
        this.lines = lines;
    }

    public List<SectionModel> getHeadingLines() {
        return headingLines;
    }

    public void setHeadingLines(List<SectionModel> headingLines) {
        this.headingLines = headingLines;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public List<Float> getNonBlankTextAscentLines() {
        return nonBlankTextAscentLines;
    }

    public void setNonBlankTextLinePositions(List<Float> nonBlankTextAscentLines) {
        this.nonBlankTextAscentLines = nonBlankTextAscentLines;
    }

    public List<Float> getNonBlankTextDescentLines() {
        return nonBlankTextDescentLines;
    }

    public void setNonBlankTextDescentLines(List<Float> nonBlankTextDescentLines) {
        this.nonBlankTextDescentLines = nonBlankTextDescentLines;
    }

    public List<ImageModel> getImages() {
        return images;
    }

    public void setImages(List<ImageModel> images) {
        this.images = images;
    }

    public Boolean getFontDetectionIgnoreSuperSubscript() {
        return fontDetectionIgnoreSuperSubscript;
    }

    public void setFontDetectionIgnoreSuperSubscript(Boolean fontDetectionIgnoreSuperSubscript) {
        this.fontDetectionIgnoreSuperSubscript = fontDetectionIgnoreSuperSubscript;
    }

    public long getLastBlockCount() {
        return lastBlockCount;
    }

    public void setLastBlockCount(long lastBlockCount) {
        this.lastBlockCount = lastBlockCount;
    }

    public String getSpecialCharacters() {
        return specialCharacters;
    }

    public void setSpecialCharacters(String specialCharacters) {
        this.specialCharacters = specialCharacters;
    }

    public List<Double> getLeadingValues() {
        return leadingValues;
    }

    public void setLeadingValues(List<Double> leadingValues) {
        this.leadingValues = leadingValues;
    }

    public List<String> getTextLines() {
        return textLines;
    }

    public void setTextLines(List<String> textLines) {
        this.textLines = textLines;
    }
    
    public float getAvgSingleSpaceWidth() {
        return avgSingleSpaceWidth;
    }

    public void setAvgSingleSpaceWidth(float avgSingleSpaceWidth) {
        this.avgSingleSpaceWidth = avgSingleSpaceWidth;
    }
    
    public boolean isLastTextBlockOnSameLine() {
        return lastTextBlockOnSameLine;
    }

    public void setLastTextBlockOnSameLine(boolean lastTextBlockOnSameLine) {
        this.lastTextBlockOnSameLine = lastTextBlockOnSameLine;
    }
    
    public float getTextWidth() {
        return textWidth;
    }

    public void setTextWidth(float textWidth) {
        this.textWidth = textWidth;
    }

    @Override
    public void eventOccurred(IEventData data, EventType type) {
        if (type.equals(EventType.RENDER_TEXT)) {
            renderText((TextRenderInfo) data); // Render text
        } else if (type.equals(EventType.RENDER_IMAGE)) {
            renderImage((ImageRenderInfo) data); // Render images
        } else {
            LOGGER.debug("Event type " + type.toString() + " does not have a processing branch");
        }
    }

    @Override
    public Set<EventType> getSupportedEvents() {
        HashSet<EventType> supportedEvents = new HashSet<>();
        supportedEvents.add(EventType.RENDER_TEXT);
        supportedEvents.add(EventType.RENDER_IMAGE);
        return supportedEvents;
    }

}
