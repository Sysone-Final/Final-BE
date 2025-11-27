/**
 * 작성자: 황요한
 * 랙 Excel 내보내기 및 일괄 업로드 기능을 제공하는 컨트롤러
 */
package org.example.finalbe.domains.rack.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.rack.dto.RackBulkUploadPreviewResponse;
import org.example.finalbe.domains.rack.dto.RackBulkUploadResultResponse;
import org.example.finalbe.domains.rack.service.RackExcelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
@Validated
public class RackExcelController {

    private final RackExcelService rackExcelService;

    // 랙 목록 Excel 내보내기
    @GetMapping("/serverroom/{serverRoomId}/export")
    public ResponseEntity<byte[]> exportRacksToExcel(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId) {

        byte[] excelData = rackExcelService.exportRacksToExcel(serverRoomId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition",
                        "attachment; filename=racks_serverroom_" + serverRoomId + ".xlsx")
                .body(excelData);
    }

    // 일괄 업로드 템플릿 다운로드
    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<byte[]> downloadBulkUploadTemplate() {
        byte[] templateData = rackExcelService.generateBulkUploadTemplate();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=rack_upload_template.xlsx")
                .body(templateData);
    }

    // 일괄 업로드 미리보기
    @PostMapping("/bulk-upload/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> previewBulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId) {

        RackBulkUploadPreviewResponse preview = rackExcelService.previewBulkUpload(file, serverRoomId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "일괄 등록 미리보기 완료", preview));
    }

    // 일괄 업로드 실행
    @PostMapping("/bulk-upload/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> executeBulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId) {

        RackBulkUploadResultResponse result = rackExcelService.executeBulkUpload(file, serverRoomId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "랙 일괄 등록 완료", result));
    }
}
