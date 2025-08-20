package com.hackerton.hackerton2025.Config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.python")

public class PythonProps {

    private String bin;
    private String workDir;
    public String bin() { return bin; }
    public void setBin(String bin) { this.bin = bin; }
    public String workDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }


}
