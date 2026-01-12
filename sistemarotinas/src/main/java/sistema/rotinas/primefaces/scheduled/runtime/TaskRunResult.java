package sistema.rotinas.primefaces.scheduled.runtime;

import java.util.Locale;

public class TaskRunResult {

    public static final String OK = "OK";
    public static final String SKIPPED = "SKIPPED";
    public static final String FAIL = "FAIL";

    private static final int MAX_MESSAGE = 500; // bate com sua coluna last_message
    private static final int MAX_ERROR = 8000;  // LOB, mas ainda assim é bom limitar

    private final String status;   // OK | SKIPPED | FAIL
    private final String message;  // mensagem curta para persistir
    private final String error;    // opcional

    public TaskRunResult(String status, String message, String error) {
        this.status = normalizeStatus(status);
        this.message = limit(message, MAX_MESSAGE);
        this.error = limit(error, MAX_ERROR);
    }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getError() { return error; }

    public static TaskRunResult ok(String message) {
        return new TaskRunResult(OK, message, null);
    }

    public static TaskRunResult skipped(String message) {
        return new TaskRunResult(SKIPPED, message, null);
    }

    public static TaskRunResult fail(String message) {
        return new TaskRunResult(FAIL, message, null);
    }

    public static TaskRunResult fail(String message, String error) {
        return new TaskRunResult(FAIL, message, error);
    }

    public static TaskRunResult fail(Throwable t) {
        String msg = (t != null ? t.getMessage() : "Erro desconhecido");
        return new TaskRunResult(FAIL, "Exception", msg);
    }

    private static String normalizeStatus(String s) {
        if (s == null) return FAIL;
        String x = s.trim().toUpperCase(Locale.ROOT);
        if (OK.equals(x) || SKIPPED.equals(x) || FAIL.equals(x)) return x;
        return FAIL;
    }

    private static String limit(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() <= max) return t;
        return t.substring(0, max - 3) + "...";
    }
}