package gov.nsf.psm.documentcompliance.compliance.common.utility;

/* 
 * This enum contains cp850 codes for unicode characters not supported by the native cp850 (Latin-1) charset
 */
public enum UnicodeToCp850Enum {
    
    E1("201c","22"),
    E2("2022","7"),
    E3("2019","27"),
    E4("2010","F0"),
    E5("e9","82"),
    E6("201d","22"),
    E7("2013","2D"),
    E8("f6","94"),
    E9("ed","A1"),
    E10("ea","88"),
    E11("b7","FA"),
    E12("131","D5");
   
    String unicode;
    String cp850Code;
    
    private UnicodeToCp850Enum(String unicode, String cp850Code) {
        this.unicode = unicode;
        this.cp850Code = cp850Code;
    }
    
    public String getUnicode() {
        return unicode;
    }

    public String getCp850Code() {
        return cp850Code;
    }

}
