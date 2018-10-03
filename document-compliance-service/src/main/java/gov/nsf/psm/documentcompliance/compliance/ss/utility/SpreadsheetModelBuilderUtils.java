package gov.nsf.psm.documentcompliance.compliance.ss.utility;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nsf.psm.documentcompliance.compliance.common.utility.Constants;
import gov.nsf.psm.documentcompliance.compliance.common.utility.DateUtils;
import gov.nsf.psm.documentcompliance.compliance.common.utility.DocComplianceUtils;
import gov.nsf.psm.documentcompliance.service.parameter.SpreadsheetParameters;
import gov.nsf.psm.foundation.model.compliance.doc.FontModel;
import gov.nsf.psm.foundation.model.compliance.doc.ImageModel;
import gov.nsf.psm.foundation.model.compliance.ss.CellModel;
import gov.nsf.psm.foundation.model.compliance.ss.RowModel;
import gov.nsf.psm.foundation.model.compliance.ss.TableModel;

public class SpreadsheetModelBuilderUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SpreadsheetModelBuilderUtils.class);

    private SpreadsheetModelBuilderUtils() {
        // Private constructor
    }

    public static List<String> getLinks(Sheet sheet) {
        List<String> urls = new ArrayList<>();
        for (Hyperlink link : sheet.getHyperlinkList()) {
            urls.add(link.getAddress());
        }
        return urls;
    }

    public static List<ImageModel> getImages(Workbook wb) {
        List<ImageModel> images = new ArrayList<>();
        for (PictureData pictData : wb.getAllPictures()) {
            ImageModel imageModel = new ImageModel();
            imageModel.setBitType(pictData.getPictureType());
            images.add(imageModel);
        }
        return images;
    }

    public static List<FontModel> getFonts(Workbook wb) {
        List<FontModel> fonts = new ArrayList<>();
        for (short s = 0; s < wb.getNumberOfFonts(); s++) {
            FontModel fontModel = new FontModel();
            fontModel.setName(wb.getFontAt(s).getFontName());
            fontModel.setSize(wb.getFontAt(s).getFontHeightInPoints());
            if (!fonts.contains(fontModel)) {
                fonts.add(fontModel);
            }
        }
        return fonts;
    }

    public static List<CellModel> processHSSFRow(Object rowObj,  String defaultEncoding, boolean checkCharset) {
        List<CellModel> cellModels = new ArrayList<>();
        if (rowObj instanceof HSSFRow) {
            HSSFRow row = (HSSFRow) rowObj;
            Iterator<Cell> cells = row.cellIterator();
            while (cells.hasNext()) {
                HSSFCell cell = (HSSFCell) cells.next();
                CellModel cellModel = new CellModel();
                cellModel.setCol(CellReference.convertNumToColString(cell.getAddress().getColumn()));
                cellModel.setRow(cell.getAddress().getRow());
                cellModel.setValue(cell.toString());
                String value = cellModel.getValue();
                cellModel = checkCharset(cellModel, value, defaultEncoding, checkCharset);
                cellModels.add(cellModel);
            }
        }
        return cellModels;
    }

    public static List<CellModel> processXSSFRow(Object rowObj, List<String> nonTextColumns, List<Integer> nonStrCols,
            String defaultEncoding, long tableRowNum, boolean checkCharset) {
        List<CellModel> cellModels = new ArrayList<>();
        if (rowObj instanceof XSSFRow) {
            XSSFRow row = (XSSFRow) rowObj;
            Iterator<Cell> cells = row.cellIterator();
            boolean goToNextCell = true;
            int emptyValueCount = 0;
            while (cells.hasNext()) {
                XSSFCell cell = (XSSFCell) cells.next();
                String value = cell.toString();
                if (StringUtils.isEmpty(value)) {
                    goToNextCell = goToNextCell(cell);
                    emptyValueCount++;
                } else {
                    nonStrCols.addAll(updateNonStrColumns(nonTextColumns, cell, value, tableRowNum));
                    value = updateNonStrValue(nonStrCols, cell, value);
                }
                if (goToNextCell) {
                    CellModel cellModel = new CellModel();
                    cellModel.setCol(CellReference.convertNumToColString(cell.getAddress().getColumn()));
                    cellModel.setRow(cell.getAddress().getRow());
                    cellModel.setFormat(cell.getCellTypeEnum().name());
                    value = getDateString(value);
                    cellModel.setValue(value);
                    cellModel = checkCharset(cellModel, value, defaultEncoding, checkCharset);
                    cellModels.add(cellModel);
                    cellModels = createCellModelList(cellModels, row, emptyValueCount);
                }
            }
        }
        return cellModels;
    }

    public static List<TableModel> processXSSFTables(Sheet sheet, List<String> nonTextColumns, SpreadsheetParameters params) {
        List<TableModel> tableModels = new ArrayList<>();
        if (sheet instanceof XSSFSheet) { // Tables only available for XSSF
                                          // format
            List<XSSFTable> tables = ((XSSFSheet) sheet).getTables();
            tables.sort((t1, t2) -> t1.getStartCellReference().getRow() - t2.getStartCellReference().getRow());
            int i = 1;
            for (XSSFTable table : tables) {
                int startRow = table.getStartRowIndex();
                int endRow = table.getEndRowIndex();
                long k = 0;
                TableModel tableModel = new TableModel();
                tableModel.setNum(i++);
                List<RowModel> rowModels = new ArrayList<>();
                String tableName = table.getDisplayName();
                if (!StringUtils.isEmpty(tableName)) {
                    tableName = tableName.replaceAll("(?<=[a-z])([A-Z])", " $1");
                    tableModel.setName(tableName);
                } else {
                    tableModel.setName("[" + "Table " + table.getCTTable().getId() + "]");
                }
                List<Integer> nonStrCols = new ArrayList<>();
                for (int j = startRow; j <= endRow; j++) {
                    Object rowObj = sheet.getRow(j);
                    List<CellModel> cellModels = processXSSFRow(rowObj, nonTextColumns, nonStrCols, params.getDefaultEncoding(), k, params.getCheckCharset());
                    addNewRowModel(cellModels, rowModels, j, ++k);
                }
                tableModel.setRows(rowModels);
                tableModels.add(tableModel);
            }
        }
        return tableModels;
    }

    public static List<RowModel> processAllRows(Sheet sheet, List<String> nonTextColumns, SpreadsheetParameters params) {
        int rowNum = 0;
        Iterator<Row> rows = sheet.rowIterator();
        List<RowModel> rowModels = new ArrayList<>();
        List<Integer> nonStrCols = new ArrayList<>();
        while (rows.hasNext()) {
            rowNum++;
            Object rowObj = rows.next();
            RowModel rowModel = new RowModel();
            List<CellModel> cellModels = new ArrayList<>();
            cellModels.addAll(processHSSFRow(rowObj, params.getDefaultEncoding(), params.getCheckCharset()));
            cellModels.addAll(processXSSFRow(rowObj, nonTextColumns, nonStrCols, params.getDefaultEncoding(), 0, params.getCheckCharset()));
            rowModel.setCells(cellModels);
            rowModel.setNum(rowNum);
            rowModels.add(rowModel);
        }
        return rowModels;
    }

    public static boolean goToNextCell(XSSFCell cell) {
        return cell.getCellStyle().getBottomBorderColor() == Constants.BORDER_EXCLUSION_COLOR ? false : true;
    }

    public static List<Integer> updateNonStrColumns(List<String> nonTextColumns, XSSFCell cell, String value,
            long tableRowNum) {
        List<Integer> nonStrCols = new ArrayList<>();
        for (String nonTextColumn : nonTextColumns) {
            if (tableRowNum == 0 && value.trim().toUpperCase().indexOf(nonTextColumn) > -1) {
                nonStrCols.add(cell.getColumnIndex());
            }
        }
        return nonStrCols;
    }

    public static String updateNonStrValue(List<Integer> nonStrCols, XSSFCell cell, String value) {
        return nonStrCols.contains(cell.getColumnIndex()) && !cell.getCellTypeEnum().equals(CellType.NUMERIC) ? ""
                : value;
    }

    public static String getDateString(String dateValue) {
        Object dateObj = DateUtils.convertToDate(dateValue);
        if (dateObj != null) {
            Date date = (Date) dateObj;
            return DateUtils.formatDate(date, Constants.DATE_FORMAT_MDY);
        } else {
            return dateValue;
        }
    }

    public static void addNewRowModel(List<CellModel> cellModels, List<RowModel> rowModels, int j, long rowNum) {
        RowModel rowModel = new RowModel();
        if (!cellModels.isEmpty()) {
            rowModel.setCells(cellModels);
            rowModel.setNum(j);
            rowModel.setTableRowNum(rowNum);
            rowModels.add(rowModel);
        }
    }

    public static List<CellModel> createCellModelList(List<CellModel> cellModels, XSSFRow row, int emptyValueCount) {
        List<CellModel> newCellModels = new ArrayList<>();
        if (emptyValueCount == row.getLastCellNum() || emptyValueCount == (row.getLastCellNum() - 1)) {
            return newCellModels;
        } else {
            return cellModels;
        }
    }
    
    public static CellModel checkCharset(CellModel cellModel, String value, String defaultEncoding, boolean checkCharset) {
       CellModel newCellModel = null;
       if(!StringUtils.isEmpty(value) && checkCharset) {
            newCellModel = enforceCharset(cellModel, defaultEncoding);
       } else {
           newCellModel = cellModel;
       }
       return newCellModel;
    }
    
    public static CellModel enforceCharset(CellModel cell, String charset) {
        String value = cell.getValue();
        List<String> unsupportedChars = new ArrayList<>();
        StringBuilder newValue = new StringBuilder();
        for(char character : value.toCharArray()) {
            String origValue = String.valueOf(character);
            String checkedValue = new String(origValue.getBytes(Charset.forName(charset)));
            if(!checkedValue.equalsIgnoreCase(origValue)) {
                String matchingStr = DocComplianceUtils.getMatchingCp850Character(character);
                checkedValue = getCheckedValue(character, cell.getRow(), matchingStr, charset);
                if(checkedValue.equalsIgnoreCase(Constants.DEFAULT_CHARSET_PLACEHOLDER) && !unsupportedChars.contains(origValue)) {
                    unsupportedChars.add(origValue);
                }
            }
            newValue.append(checkedValue);
        }
        cell.setValue(newValue.toString());
        if(!unsupportedChars.isEmpty()) {
            cell.setUnsupportedChars(unsupportedChars);
        }
        return cell;
    }
    
    public static String convertHexToString(String hex, String charset){
        String charStr = null;
        if(hex.length() < 2) {
            StringBuilder sb = new StringBuilder();
            for( int i=0; i<hex.length()-1; i+=2 ){
                String output = hex.substring(i, i + 2);
                int code = Integer.parseInt(output, 16);
                sb.append((char) code);
            } 
            charStr = sb.toString();
        } else {
            try {
                charStr = new String(DatatypeConverter.parseHexBinary(hex),charset);
            } catch(Exception e) {
                charStr = Constants.DEFAULT_CHARSET_PLACEHOLDER;
                LOGGER.debug(e.getMessage(), e);
            }
        }
        return charStr;
    }
    
    public static String getCheckedValue(char character, long rowNo, String matchingStr, String charset) {
        String checkedValue = null;
        if(!StringUtils.isEmpty(matchingStr)) {
            checkedValue = convertHexToString(matchingStr, charset);
        } else {
            checkedValue = Constants.DEFAULT_CHARSET_PLACEHOLDER;
            LOGGER.debug("Character in row " + rowNo + " is not supported by CP 850 Charset: " + character + " (" + Integer.toHexString(character) + ")");
        }
        return checkedValue;
    }

}