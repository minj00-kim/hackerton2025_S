// com.hackerton.hackerton2025.Service.PythonRunner
package com.hackerton.hackerton2025.Service;

import com.hackerton.hackerton2025.Config.PythonProps;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class PythonRunner {

    private final PythonProps props;
    public PythonRunner(PythonProps props) { this.props = props; }

    public record Result(int exitCode, String stdout, String stderr) {}

    public Result run(String scriptName,
                      List<String> args,
                      Map<String,String> extraEnv,
                      Duration timeout) throws Exception {

        String bin = props.getExec();
        String workDir = props.getWorkDir();

        if (bin == null || bin.isBlank())
            throw new IllegalStateException("Python 실행 경로(app.python.exec)가 비어있습니다.");
        if (scriptName == null || scriptName.isBlank())
            throw new IllegalArgumentException("실행할 스크립트명이 비어있습니다.");

        List<String> cmd = new ArrayList<>();
        cmd.add(bin);
        cmd.add(scriptName);
        if (args != null) {
            for (String a : args) {
                if (a != null) cmd.add(a);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workDir != null && !workDir.isBlank()) {
            pb.directory(new File(workDir));
        }
        if (extraEnv != null) pb.environment().putAll(extraEnv);
        pb.redirectErrorStream(false);

        System.out.println("[PythonRunner] workDir = " + pb.directory());
        System.out.println("[PythonRunner] CMD     = " + String.join(" ", cmd));

        Process p = pb.start();

        StringBuilder outBuf = new StringBuilder();
        StringBuilder errBuf = new StringBuilder();

        Thread tout = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                br.lines().forEach(l -> outBuf.append(l).append("\n"));
            } catch (IOException ignored) {}
        });
        Thread terr = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                br.lines().forEach(l -> errBuf.append(l).append("\n"));
            } catch (IOException ignored) {}
        });
        tout.start(); terr.start();

        boolean finished = p.waitFor(timeout == null ? 180_000 : timeout.toMillis(),
                java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("Python process timed out");
        }
        tout.join(); terr.join();

        return new Result(p.exitValue(), outBuf.toString(), errBuf.toString());
    }
}
