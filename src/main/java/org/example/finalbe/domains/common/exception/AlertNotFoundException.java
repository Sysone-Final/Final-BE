/**
 * 작성자: 황요한
 * 알림(Alert)을 찾을 수 없을 때 발생하는 예외
 */
package org.example.finalbe.domains.common.exception;

public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(Long alertId) {
        super("알림을 찾을 수 없습니다. ID: " + alertId);
    }

    public AlertNotFoundException(String message) {
        super(message);
    }
}
