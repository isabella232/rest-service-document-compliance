package gov.nsf.psm.documentcompliance.service.parameter;

public class SpreadsheetParameters {
    
    private Boolean checkCharset;
    private String defaultEncoding;
    
    public SpreadsheetParameters(Boolean checkCharset, String defaultEncoding) {
        this.checkCharset = checkCharset;
        this.defaultEncoding = defaultEncoding;
    }
    
    public Boolean getCheckCharset() {
        return checkCharset;
    }
    public void setCheckCharset(Boolean checkCharset) {
        this.checkCharset = checkCharset;
    }
    public String getDefaultEncoding() {
        return defaultEncoding;
    }
    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }
    
}