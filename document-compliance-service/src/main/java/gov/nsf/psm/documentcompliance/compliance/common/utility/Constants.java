package gov.nsf.psm.documentcompliance.compliance.common.utility;

public class Constants {

    // PDF Constants
    public static final Double LINEAR_INCH_EQUIVALENT_IN_POINTS = 72.272;
    public static final Double SUP_SUB_MAXIMUM_FONT_PERCENTAGE = 0.75;
    public static final Double SUP_SUB_MINIMUM_BASELINE_PERCENTAGE = 0.75;
    public static final Double LINE_CHARACTER_OVERHEIGHT_MULTIPLIER = 1.1;
    public static final float MINIMUM_SUPERSUBSCRIPT_PT_SIZE = 5.0f; // Used to
                                                                     // correct
                                                                     // PDF
                                                                     // conversion
                                                                     // of
                                                                     // superscript
                                                                     // or
                                                                     // subscript
                                                                     // pt size
    
    // Controls insertion of space characters.  Primarily for Latek-generated PDFs
    public static final float SPACE_CHARACTER_WIDTH_DIVISOR = 0.5f; 
    public static final float SPACE_CHARACTER_WIDTH_THRESHOLD = 1.0f;
    
    public static final int LOWER_SUPERSCRIPT_CORRECTION_BOUNDARY = 2;
    public static final int UPPER_SUPERSCRIPT_CORRECTION_BOUNDARY = 3;
    public static final int MAX_NO_OF_COLUMNS_DETECTED = 3;
    
    public static final int MAX_FILE_SIZE_BYTES = 50000000; // Should always be
                                                            // higher than
                                                            // property

    // Date Constants
    public static final String DATE_FORMAT_MDY = "M/d/yyyy";

    // Info Keys
    public static final String INFO_KEY_PRODUCER = "Producer";
    
    public static final String LOGGING_EMPTY_DATA_PLACEHOLDER = "Unavailable";

    // Spreadsheet Constants
    public static final Short BORDER_EXCLUSION_COLOR = 8;

    // Heading Detection Constants
    public static final String HEADING_REGEX_CHAR_INCLUDE = "([a-zA-Z][^\\\\s]*)";
    public static final String HEADING_CHAR_EXCLUDE = "\\u003A";
    public static final int HEADING_MAX_WORD_COUNT = 10;

    // Service Constants
    public static final String CONTEXT_DEFAULT_NAME = "/document-compliance-service";
    public static final String CONFIG_FILE_DEFAULT_NAME = "document-compliance-service";

    public static final String CODE_BLOCK_SEPARATOR = "----------------------------------------------";
    public static final String DOUBLE_CODE_BLOCK_SEPARATOR = "=================";

    public static final String DCS_GET_METADATA_EXCEPTION_MESSAGE = "An error occurred trying to extract metadata";
    public static final String DCS_GET_MODEL_EXCEPTION_MESSAGE = "An error occurred trying to extract the model";

    public static final String CHARACTER_SET_DEFAULT = "UTF-8";

    public static final String TEXT_LINE_SEPARATOR = "\n";

    public static final String PROGRAMMATIC_FILE_PLACEHOLDER = "file";
    
    public static final String HEADER_IS_TABLES_ONLY = "isTablesOnly";
    
    public static final String DEFAULT_CHARSET_PLACEHOLDER = "?";
    
    public static final String DEFAULT_FONT_NAME_SEPARATOR = "+";
    
    public static final String XMP_METADATA_PDFA_TAG = "pdfaid:conformance";
    
    public static final String XML_SCRIPT_INJECTION_PREVENTION_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";

    private Constants() {
        // Private constructor
    }

}
