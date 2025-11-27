/**
 * 작성자: 황요한
 * 랙 Excel 처리 서비스
 */
package org.example.finalbe.domains.rack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.dto.*;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackExcelService {

    private final RackRepository rackRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final RackService rackService;

    // 랙 Excel 내보내기
    public byte[] exportRacksToExcel(Long serverRoomId) {
        List<Rack> racks = rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Racks");
            createExportHeader(workbook, sheet);
            fillExportData(sheet, racks);
            autosizeColumns(sheet, 11);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Excel 파일 생성 중 오류가 발생했습니다.");
        }
    }

    // 템플릿 생성
    public byte[] generateBulkUploadTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Rack Template");
            createTemplateHeader(workbook, sheet);
            createTemplateExample(sheet);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("템플릿 생성 중 오류가 발생했습니다.");
        }
    }

    // 일괄 업로드 미리보기
    public RackBulkUploadPreviewResponse previewBulkUpload(MultipartFile file, Long serverRoomId) {
        serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<RackBulkUploadPreviewResponse.PreviewRow> previewRows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            int valid = 0, invalid = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String rackName = getString(row.getCell(0));
                Integer totalUnits = getInteger(row.getCell(3));

                boolean isValid = true;
                StringBuilder msg = new StringBuilder();

                if (rackName == null || rackName.isBlank()) {
                    isValid = false;
                    msg.append("랙 이름은 필수입니다. ");
                }
                if (totalUnits == null || totalUnits < 1 || totalUnits > 100) {
                    isValid = false;
                    msg.append("총 유닛은 1~100 사이여야 합니다. ");
                }

                if (isValid) valid++;
                else invalid++;

                previewRows.add(RackBulkUploadPreviewResponse.PreviewRow.builder()
                        .rowNumber(i)
                        .rackName(rackName)
                        .gridX(getString(row.getCell(1)))
                        .gridY(getString(row.getCell(2)))
                        .totalUnits(totalUnits)
                        .status(getString(row.getCell(4)))
                        .isValid(isValid)
                        .errorMessage(msg.toString())
                        .build());
            }

            return RackBulkUploadPreviewResponse.builder()
                    .totalRows(sheet.getLastRowNum())
                    .validRows(valid)
                    .invalidRows(invalid)
                    .previewData(previewRows)
                    .errors(List.of())
                    .build();

        } catch (IOException e) {
            throw new BusinessException("파일 처리 중 오류가 발생했습니다.");
        }
    }

    // 일괄 업로드 실행
    @Transactional
    public RackBulkUploadResultResponse executeBulkUpload(MultipartFile file, Long serverRoomId) {

        serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<RackBulkUploadResultResponse.UploadResult> results = new ArrayList<>();
        int success = 0, fail = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String rackName = getString(row.getCell(0));
                    BigDecimal gridX = getBigDecimal(row.getCell(1));
                    BigDecimal gridY = getBigDecimal(row.getCell(2));
                    Integer totalUnits = getInteger(row.getCell(3));
                    String statusStr = getString(row.getCell(4));
                    BigDecimal maxPower = getBigDecimal(row.getCell(5));
                    String manufacturer = getString(row.getCell(6));
                    String serial = getString(row.getCell(7));
                    String notes = getString(row.getCell(8));

                    RackStatus status = statusStr != null ? RackStatus.valueOf(statusStr) : RackStatus.ACTIVE;

                    RackCreateRequest request = RackCreateRequest.builder()
                            .rackName(rackName)
                            .gridX(gridX)
                            .gridY(gridY)
                            .totalUnits(totalUnits)
                            .status(status)
                            .maxPowerCapacity(maxPower)
                            .manufacturer(manufacturer)
                            .serialNumber(serial)
                            .notes(notes)
                            .serverRoomId(serverRoomId)
                            .build();

                    rackService.createRack(request);

                    results.add(RackBulkUploadResultResponse.UploadResult.builder()
                            .rowNumber(i)
                            .rackName(rackName)
                            .success(true)
                            .message("성공")
                            .build());
                    success++;

                } catch (Exception e) {
                    results.add(RackBulkUploadResultResponse.UploadResult.builder()
                            .rowNumber(i)
                            .rackName(getString(row.getCell(0)))
                            .success(false)
                            .message(e.getMessage())
                            .build());
                    fail++;
                }
            }

            return RackBulkUploadResultResponse.builder()
                    .totalRows(sheet.getLastRowNum())
                    .successCount(success)
                    .failCount(fail)
                    .results(results)
                    .build();

        } catch (IOException e) {
            throw new BusinessException("파일 처리 중 오류가 발생했습니다.");
        }
    }

    /* ------------------------------ 내부 유틸 ------------------------------ */

    private void createExportHeader(Workbook wb, Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] columns = {"랙 이름", "X좌표", "Y좌표", "총 유닛", "사용 유닛", "가용 유닛",
                "상태", "전력 용량(W)", "현재 전력(W)", "제조사", "시리얼 번호"};

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(style);
        }
    }

    private void fillExportData(Sheet sheet, List<Rack> racks) {
        int rowNum = 1;
        for (Rack r : racks) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getRackName());
            row.createCell(1).setCellValue(r.getGridX() == null ? 0 : r.getGridX().doubleValue());
            row.createCell(2).setCellValue(r.getGridY() == null ? 0 : r.getGridY().doubleValue());
            row.createCell(3).setCellValue(r.getTotalUnits());
            row.createCell(4).setCellValue(r.getUsedUnits());
            row.createCell(5).setCellValue(r.getAvailableUnits());
            row.createCell(6).setCellValue(r.getStatus() == null ? "" : r.getStatus().name());
            row.createCell(7).setCellValue(r.getMaxPowerCapacity() == null ? 0 : r.getMaxPowerCapacity().doubleValue());
            row.createCell(8).setCellValue(r.getCurrentPowerUsage() == null ? 0 : r.getCurrentPowerUsage().doubleValue());
            row.createCell(9).setCellValue(r.getManufacturer() == null ? "" : r.getManufacturer());
            row.createCell(10).setCellValue(r.getSerialNumber() == null ? "" : r.getSerialNumber());
        }
    }

    private void createTemplateHeader(Workbook wb, Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] cols = {"랙 이름*", "X좌표", "Y좌표", "총 유닛*", "상태", "전력 용량(W)", "제조사", "시리얼 번호", "비고"};

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < cols.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(style);
            sheet.setColumnWidth(i, 4000);
        }
    }

    private void createTemplateExample(Sheet sheet) {
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("RACK-001");
        row.createCell(1).setCellValue("10.5");
        row.createCell(2).setCellValue("20.3");
        row.createCell(3).setCellValue(42);
        row.createCell(4).setCellValue("ACTIVE");
        row.createCell(5).setCellValue(5000);
        row.createCell(6).setCellValue("Dell");
        row.createCell(7).setCellValue("SN123456");
        row.createCell(8).setCellValue("테스트 랙");
    }

    private void autosizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) sheet.autoSizeColumn(i);
    }

    private String getString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private Integer getInteger(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try { yield Integer.parseInt(cell.getStringCellValue()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    private BigDecimal getBigDecimal(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try { yield new BigDecimal(cell.getStringCellValue()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }
}
