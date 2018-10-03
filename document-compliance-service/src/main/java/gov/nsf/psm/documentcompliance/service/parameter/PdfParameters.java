package gov.nsf.psm.documentcompliance.service.parameter;

import java.util.Map;

public class PdfParameters {

    private Boolean fontDetectionIgnoreBlankSpaces;
    private Boolean fontDetectionIgnoreSuperSubscript;
    private Boolean useTextExtractor;
    private String specialCharacters;
    private Map<String, String> fontMap;

    public PdfParameters(Boolean fontDetectionIgnoreBlankSpaces, Boolean fontDetectionIgnoreSuperSubscript,
            Boolean useTextExtractor, String specialCharacters, Map<String, String> fontMap) {
        this.fontDetectionIgnoreBlankSpaces = fontDetectionIgnoreBlankSpaces;
        this.fontDetectionIgnoreSuperSubscript = fontDetectionIgnoreSuperSubscript;
        this.specialCharacters = specialCharacters;
        this.useTextExtractor = useTextExtractor;
        this.fontMap = fontMap;
    }

    public Boolean getFontDetectionIgnoreBlankSpaces() {
        return fontDetectionIgnoreBlankSpaces;
    }

    public void setFontDetectionIgnoreBlankSpaces(Boolean fontDetectionIgnoreBlankSpaces) {
        this.fontDetectionIgnoreBlankSpaces = fontDetectionIgnoreBlankSpaces;
    }

    public Boolean getFontDetectionIgnoreSuperSubscript() {
        return fontDetectionIgnoreSuperSubscript;
    }

    public void setFontDetectionIgnoreSuperSubscript(Boolean fontDetectionIgnoreSuperSubscript) {
        this.fontDetectionIgnoreSuperSubscript = fontDetectionIgnoreSuperSubscript;
    }

    public String getSpecialCharacters() {
        return specialCharacters;
    }

    public void setSpecialCharacters(String specialCharacters) {
        this.specialCharacters = specialCharacters;
    }

    public Boolean getUseTextExtractor() {
        return useTextExtractor;
    }

    public void setUseTextExtractor(Boolean useTextExtractor) {
        this.useTextExtractor = useTextExtractor;
    }
    
    public Map<String, String> getFontMap() {
        return fontMap;
    }

    public void setFontMap(Map<String, String> fontMap) {
        this.fontMap = fontMap;
    }

}
