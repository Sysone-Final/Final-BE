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
import org.example.finalbe.domains.rack.dto.RackBulkUploadPreviewResponse;
import org.example.finalbe.domains.rack.dto.RackBulkUploadResultResponse;
import org.example.finalbe.domains.rack.dto.RackCreateRequest;
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

/**
 * 랙 Excel 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackExcelService {

    private final RackRepository rackRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final RackService rackService;

    /**
     * 랙 목록 Excel 내보내기
     */
    public byte[] exportRacksToExcel(Long serverRoomId) {
        log.info("Exporting racks to Excel for serverRoom: {}", serverRoomId);

        List<Rack> racks = rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Racks");

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"랙 이름", "X좌표", "Y좌표", "총 유닛", "사용 유닛", "가용 유닛",
                    "상태", "전력 용량(W)", "현재 전력(W)", "제조사", "시리얼 번호"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 행 생성
            int rowNum = 1;
            for (Rack rack : racks) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rack.getRackName());
                row.createCell(1).setCellValue(rack.getGridX() != null ? rack.getGridX().doubleValue() : 0);
                row.createCell(2).setCellValue(rack.getGridY() != null ? rack.getGridY().doubleValue() : 0);
                row.createCell(3).setCellValue(rack.getTotalUnits());
                row.createCell(4).setCellValue(rack.getUsedUnits());
                row.createCell(5).setCellValue(rack.getAvailableUnits());
                row.createCell(6).setCellValue(rack.getStatus() != null ? rack.getStatus().name() : "");
                row.createCell(7).setCellValue(rack.getMaxPowerCapacity() != null ? rack.getMaxPowerCapacity().doubleValue() : 0);
                row.createCell(8).setCellValue(rack.getCurrentPowerUsage() != null ? rack.getCurrentPowerUsage().doubleValue() : 0);
                row.createCell(9).setCellValue(rack.getManufacturer() != null ? rack.getManufacturer() : "");
                row.createCell(10).setCellValue(rack.getSerialNumber() != null ? rack.getSerialNumber() : "");
            }

            // 열 너비 자동 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error exporting racks to Excel", e);
            throw new BusinessException("Excel 파일 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * 일괄 업로드 템플릿 생성
     */
    public byte[] generateBulkUploadTemplate() {
        log.info("Generating bulk upload template");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Rack Template");

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"랙 이름*", "X좌표", "Y좌표", "총 유닛*", "상태", "전력 용량(W)", "제조사", "시리얼 번호", "비고"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 예시 데이터 추가
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("RACK-001");
            exampleRow.createCell(1).setCellValue("10.5");
            exampleRow.createCell(2).setCellValue("20.3");
            exampleRow.createCell(3).setCellValue(42);
            exampleRow.createCell(4).setCellValue("ACTIVE");
            exampleRow.createCell(5).setCellValue(5000);
            exampleRow.createCell(6).setCellValue("Dell");
            exampleRow.createCell(7).setCellValue("SN123456");
            exampleRow.createCell(8).setCellValue("테스트 랙");

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error generating template", e);
            throw new BusinessException("템플릿 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * 일괄 업로드 미리보기
     */
    public RackBulkUploadPreviewResponse previewBulkUpload(MultipartFile file, Long serverRoomId) {
        log.info("Previewing bulk upload for serverRoom: {}", serverRoomId);

        // 서버실 존재 확인
        serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<RackBulkUploadPreviewResponse.PreviewRow> previewData = new ArrayList<>();
        List<RackBulkUploadPreviewResponse.ValidationError> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            int validRows = 0;
            int invalidRows = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String rackName = getCellValueAsString(row.getCell(0));
                String gridX = getCellValueAsString(row.getCell(1));
                String gridY = getCellValueAsString(row.getCell(2));
                Integer totalUnits = getCellValueAsInteger(row.getCell(3));
                String status = getCellValueAsString(row.getCell(4));

                boolean isValid = true;
                StringBuilder errorMsg = new StringBuilder();

                // 검증
                if (rackName == null || rackName.trim().isEmpty()) {
                    isValid = false;
                    errorMsg.append("랙 이름은 필수입니다. ");
                }

                if (totalUnits == null || totalUnits < 1 || totalUnits > 100) {
                    isValid = false;
                    errorMsg.append("총 유닛은 1~100 사이여야 합니다. ");
                }

                if (isValid) {
                    validRows++;
                } else {
                    invalidRows++;
                }

                previewData.add(RackBulkUploadPreviewResponse.PreviewRow.builder()
                        .rowNumber(i)
                        .rackName(rackName)
                        .gridX(gridX)
                        .gridY(gridY)
                        .totalUnits(totalUnits)
                        .status(status)
                        .isValid(isValid)
                        .errorMessage(errorMsg.toString())
                        .build());
            }

            return RackBulkUploadPreviewResponse.builder()
                    .totalRows(sheet.getLastRowNum())
                    .validRows(validRows)
                    .invalidRows(invalidRows)
                    .previewData(previewData)
                    .errors(errors)
                    .build();

        } catch (IOException e) {
            log.error("Error previewing bulk upload", e);
            throw new BusinessException("파일 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 일괄 업로드 실행
     */
    @Transactional
    public RackBulkUploadResultResponse executeBulkUpload(MultipartFile file, Long serverRoomId) {
        log.info("Executing bulk upload for serverRoom: {}", serverRoomId);

        ServerRoom serverRoom = serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<RackBulkUploadResultResponse.UploadResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String rackName = getCellValueAsString(row.getCell(0));
                    BigDecimal gridX = getCellValueAsBigDecimal(row.getCell(1));
                    BigDecimal gridY = getCellValueAsBigDecimal(row.getCell(2));
                    Integer totalUnits = getCellValueAsInteger(row.getCell(3));
                    String statusStr = getCellValueAsString(row.getCell(4));
                    BigDecimal maxPower = getCellValueAsBigDecimal(row.getCell(5));
                    String manufacturer = getCellValueAsString(row.getCell(6));
                    String serialNumber = getCellValueAsString(row.getCell(7));
                    String notes = getCellValueAsString(row.getCell(8));

                    RackStatus status = statusStr != null ? RackStatus.valueOf(statusStr) : RackStatus.ACTIVE;

                    RackCreateRequest request = RackCreateRequest.builder()
                            .rackName(rackName)
                            .gridX(gridX)
                            .gridY(gridY)
                            .totalUnits(totalUnits)
                            .status(status)
                            .maxPowerCapacity(maxPower)
                            .manufacturer(manufacturer)
                            .serialNumber(serialNumber)
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
                    successCount++;

                } catch (Exception e) {
                    results.add(RackBulkUploadResultResponse.UploadResult.builder()
                            .rowNumber(i)
                            .rackName(getCellValueAsString(row.getCell(0)))
                            .success(false)
                            .message(e.getMessage())
                            .build());
                    failCount++;
                }
            }

            return RackBulkUploadResultResponse.builder()
                    .totalRows(sheet.getLastRowNum())
                    .successCount(successCount)
                    .failCount(failCount)
                    .results(results)
                    .build();

        } catch (IOException e) {
            log.error("Error executing bulk upload", e);
            throw new BusinessException("파일 처리 중 오류가 발생했습니다.");
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try {
                    yield new BigDecimal(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }
}