package com.hackerton.hackerton2025.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.python")
public class PythonProps {

    /** python 실행 파일 경로 (예: C:\...\python.exe) */
    private String exec = "python";
    /** 스크립트가 있는 작업 디렉터리 */
    private String workDir = ".";

    public String getExec() { return exec; }
    public void setExec(String exec) { this.exec = exec; }

    public String getWorkDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }
}
