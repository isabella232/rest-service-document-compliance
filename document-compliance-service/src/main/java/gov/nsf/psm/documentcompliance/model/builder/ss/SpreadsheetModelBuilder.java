package gov.nsf.psm.documentcompliance.model.builder.ss;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nsf.psm.documentcompliance.compliance.common.utility.DocComplianceUtils;
import gov.nsf.psm.documentcompliance.compliance.ss.utility.SpreadsheetModelBuilderUtils;
import gov.nsf.psm.documentcompliance.model.builder.ComplianceModelBuilder;
import gov.nsf.psm.documentcompliance.service.parameter.SpreadsheetParameters;
import gov.nsf.psm.factmodel.FileFactModel;
import gov.nsf.psm.foundation.model.compliance.doc.FontModel;
import gov.nsf.psm.foundation.model.compliance.doc.ImageModel;
import gov.nsf.psm.foundation.model.compliance.ss.RowModel;
import gov.nsf.psm.foundation.model.compliance.ss.SpreadsheetModel;
import gov.nsf.psm.foundation.model.compliance.ss.TableModel;
import gov.nsf.psm.foundation.model.compliance.ss.WorksheetModel;

public class SpreadsheetModelBuilder implements ComplianceModelBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpreadsheetModelBuilder.class);

    private SpreadsheetParameters params = null;
    private Boolean isTablesOnly = false;
    private List<String> nonTextColumns = null;

    public SpreadsheetModelBuilder() {
        // Empty constructor
    }

    public SpreadsheetModelBuilder(boolean isTablesOnly, List<String> nonTextColumns, SpreadsheetParameters params) {
        this.isTablesOnly = isTablesOnly;
        this.nonTextColumns = nonTextColumns;
        this.params = params;
    }

    @Override
    public SpreadsheetModel buildMetadata(InputStream inputStream, String fileName, long sizeInBytes) {
        long t1 = 0;
        long t2 = 0;
        SpreadsheetModel model = new SpreadsheetModel();
        Workbook wb = null;
        try {
            wb = WorkbookFactory.create(inputStream);
            model.setNoOfSheets(wb.getNumberOfSheets());
            FileFactModel fileModel = new FileFactModel();
            fileModel.setSize(DocComplianceUtils.convertFileSizeFromBytesToMB(sizeInBytes));
            fileModel.setName(fileName);
            model.setFileModel(fileModel);
        } catch (EncryptedDocumentException e) {
            model.setEncrypted(true);
            LOGGER.info(e.getMessage(), e);
        } catch (InvalidFormatException | IOException e) {
            LOGGER.info(e.getMessage(), e);
        } finally {
            t2 = (int) ((System.currentTimeMillis() - t1) / 1000);
            model.setProcessingTime(t2 - t1);
            try {
                if (wb != null)
                    wb.close();
            } catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
            }
        }
        return model;
    }

    @Override
    public SpreadsheetModel buildModel(InputStream inputStream, String fileName, long sizeInBytes) {

        SpreadsheetModel model = new SpreadsheetModel();
        Workbook wb = null;

        try {
            wb = WorkbookFactory.create(inputStream);
            model.setNoOfSheets(wb.getNumberOfSheets());
            List<WorksheetModel> workSheets = new ArrayList<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                WorksheetModel sheetModel = new WorksheetModel();
                List<RowModel> rowModels = new ArrayList<>();
                List<TableModel> tableModels = new ArrayList<>();
                sheetModel.setFonts(new ArrayList<FontModel>());
                sheetModel.setUrls(new ArrayList<String>());
                sheetModel.setImages(new ArrayList<ImageModel>());
                sheetModel.setFonts(SpreadsheetModelBuilderUtils.getFonts(wb));
                sheetModel.setUrls(SpreadsheetModelBuilderUtils.getLinks(sheet));
                sheetModel.setImages(SpreadsheetModelBuilderUtils.getImages(wb));
                if (isTablesOnly) {
                    tableModels.addAll(SpreadsheetModelBuilderUtils.processXSSFTables(sheet, nonTextColumns, params));
                } else {
                    rowModels.addAll(SpreadsheetModelBuilderUtils.processAllRows(sheet, nonTextColumns, params));
                }
                sheetModel.setRows(rowModels);
                sheetModel.setTables(tableModels);
                workSheets.add(sheetModel);
            }
            model.setWorksheets(workSheets);
        } catch (EncryptedDocumentException e) {
            model.setEncrypted(true);
            LOGGER.info(e.getMessage(), e);
        } catch (InvalidFormatException | IOException e) {
            LOGGER.info(e.getMessage(), e);
        } finally {
            try {
                if (wb != null)
                    wb.close();
                inputStream.close();
            } catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
            }
        }
        return model;
    }

}
