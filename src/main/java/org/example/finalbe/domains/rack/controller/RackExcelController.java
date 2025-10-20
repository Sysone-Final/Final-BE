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

    /**
     * 특정 데이터센터의 랙 목록을 Excel 파일로 내보내는 기능
     * 데이터센터의 모든 랙 정보를 Excel 파일로 다운로드
     * 백업이나 보고서 작성 등에 활용
     * 권한: 모든 사용자 접근 가능
     */
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

    /**
     * 랙 일괄 등록용 Excel 템플릿 파일을 다운로드하는 기능
     * 미리 정의된 양식의 빈 Excel 파일을 제공
     * 사용자는 이 템플릿에 데이터를 입력해서 일괄 업로드에 사용
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<byte[]> downloadBulkUploadTemplate() {
        byte[] templateData = rackExcelService.generateBulkUploadTemplate();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=rack_upload_template.xlsx")
                .body(templateData);
    }

    /**
     * 랙 일괄 등록 전에 미리보기를 제공하는 기능
     * Excel 파일을 업로드하면 실제 등록 전에 어떤 데이터가 들어갈지 확인 가능
     * 오류가 있는 항목이나 중복 데이터 등을 미리 체크해서 알려줌
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
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

    /**
     * Excel 파일로 여러 개의 랙을 한 번에 등록하는 기능
     * 미리보기 확인 후 실제로 데이터베이스에 저장
     * 성공/실패한 항목 개수와 상세 결과를 반환
     * 여러 랙을 수동으로 하나씩 등록하는 것보다 훨씬 효율적
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
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