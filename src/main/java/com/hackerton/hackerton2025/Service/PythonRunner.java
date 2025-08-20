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

    public Result run(String scriptName, List<String> args, Map<String,String> extraEnv, Duration timeout) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(props.bin());
        cmd.add(scriptName);
        if (args != null) cmd.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(props.workDir()));
        pb.redirectErrorStream(false);
        if (extraEnv != null) pb.environment().putAll(extraEnv);

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

        boolean finished = p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("Python process timed out");
        }
        tout.join(); terr.join();

        return new Result(p.exitValue(), outBuf.toString(), errBuf.toString());
    }

}
