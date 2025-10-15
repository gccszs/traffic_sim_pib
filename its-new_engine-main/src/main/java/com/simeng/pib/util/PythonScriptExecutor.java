package com.simeng.pib.util;

import com.simeng.pib.config.PythonEnvConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Python脚本执行器
 */
@Slf4j
@Component
public class PythonScriptExecutor {

    @Autowired
    PythonEnvConfig pythonEnvConfig;

    /**
     * 构建pyenv环境的命令
     */
    private List<String> buildPyenvCommand(String scriptPath, List<String> args) {
        List<String> command = new ArrayList<>();

        // 检测操作系统
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows系统：使用pyenv-win的python命令
            Path pyenvPythonPath = Paths.get(pythonEnvConfig.getPyenvPath(), "versions", pythonEnvConfig.getPythonVersion(), "python.exe");
            if (Files.exists(pyenvPythonPath)) {
                command.add(pyenvPythonPath.toString());
            } else {
                // 如果找不到pyenv，尝试使用相对路径的Python
                command.add(Paths.get(pythonEnvConfig.getPyenvPath(), "shims", "python").toString());
            }
        } else {
            // Linux/Mac系统：使用pyenv的python命令
            command.add(Paths.get(pythonEnvConfig.getPyenvPath(), "versions", pythonEnvConfig.getPythonVersion(), "bin", "python").toString());
        }

        command.add(scriptPath);
        command.addAll(args);

        return command;
    }
    /**
     * 执行Python脚本
     * 
     * @param scriptPath Python脚本路径
     * @param args 脚本参数
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public ScriptExecutionResult executePythonScript(String scriptPath, List<String> args, int timeoutSeconds) {
        List<String> command = buildPyenvCommand(scriptPath,args);
        
        return executeCommand(command, timeoutSeconds);
    }
    
    /**
     * 执行系统命令
     */
    private ScriptExecutionResult executeCommand(List<String> command, int timeoutSeconds) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        try {
            log.debug("Executing command: {}", String.join(" ", command));
            Process process = processBuilder.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 等待进程完成
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return new ScriptExecutionResult(false, "Script execution timeout", "");
            }
            
            int exitCode = process.exitValue();
            String outputString = output.toString();
            
            log.debug("Script execution completed with exit code: {}", exitCode);
            log.debug("Script output: {}", outputString);
            
            return new ScriptExecutionResult(exitCode == 0, 
                exitCode == 0 ? "Script executed successfully" : "Script execution failed with exit code: " + exitCode,
                outputString);
                
        } catch (IOException | InterruptedException e) {
            log.error("Error executing script", e);
            return new ScriptExecutionResult(false, "Script execution error: " + e.getMessage(), "");
        }
    }
    
    /**
     * 脚本执行结果
     */
    @Data
    public static class ScriptExecutionResult {
        private final boolean success;
        private final String message;
        private final String output;
    }
}
