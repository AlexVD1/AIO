package com.kidsanim.api.video.ffmpeg;

public class FFmpegExecutionException extends RuntimeException {

    private final int exitCode;
    private final String errorOutput;

    public FFmpegExecutionException(String message, int exitCode, String errorOutput) {
        super(message + " (Exit code: " + exitCode + ")\nOutput: " + errorOutput);
        this.exitCode = exitCode;
        this.errorOutput = errorOutput != null ? errorOutput : "";
    }

    public FFmpegExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.exitCode = -1;
        this.errorOutput = cause != null ? cause.getMessage() : "";
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getErrorOutput() {
        return errorOutput;
    }
}
