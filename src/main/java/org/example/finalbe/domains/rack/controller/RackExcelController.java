package org.example.finalbe.domains.rack.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.rack.dto.RackBulkUploadPreviewResponse;
import org.example.finalbe.domains.rack.dto.RackBulkUploadResultResponse;
import org.example.finalbe.domains.rack.service.RackExcelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 랙 Excel 관리 컨트롤러
 * Excel 내보내기 및 일괄 등록
 */
@RestController
@RequestMapping("/racks")
@RequiredArgsConstructor
public class RackExcelController {

    private final RackExcelService rackExcelService;

    @GetMapping("/datacenter/{dataCenterId}/export")
    public ResponseEntity<byte[]> exportRacksToExcel(@PathVariable Long dataCenterId) {
        if (dataCenterId == null || dataCenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        byte[] excelData = rackExcelService.exportRacksToExcel(dataCenterId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition",
                        "attachment; filename=racks_datacenter_" + dataCenterId + ".xlsx")
                .body(excelData);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<byte[]> downloadBulkUploadTemplate() {
        byte[] templateData = rackExcelService.generateBulkUploadTemplate();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=rack_upload_template.xlsx")
                .body(templateData);
    }

    @PostMapping("/bulk-upload/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> previewBulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long dataCenterId) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일을 업로드해주세요.");
        }
        if (dataCenterId == null || dataCenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new IllegalArgumentException("Excel 파일만 업로드 가능합니다. (.xlsx, .xls)");
        }

        RackBulkUploadPreviewResponse preview = rackExcelService.previewBulkUpload(file, dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "일괄 등록 미리보기 완료", preview));
    }

    @PostMapping("/bulk-upload/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> executeBulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long dataCenterId) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일을 업로드해주세요.");
        }
        if (dataCenterId == null || dataCenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new IllegalArgumentException("Excel 파일만 업로드 가능합니다. (.xlsx, .xls)");
        }

        RackBulkUploadResultResponse result = rackExcelService.executeBulkUpload(file, dataCenterId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "랙 일괄 등록 완료", result));
    }
}