package com.simeng.pib.controller;

import com.simeng.pib.model.SimInfo;
import com.simeng.pib.model.dto.ApiResponse;
import com.simeng.pib.service.PluginService;
import com.simeng.pib.service.impl.SessionServiceImpl;
import com.simeng.pib.service.impl.SimulationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 仿真控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SimulationController {


    private final SessionServiceImpl sessionServiceImpl;

    private final SimulationServiceImpl simulationService;

    private final PluginService pluginService;


    @Value("${simeng.simeng-dir}")
    private String simengDir;

    @Value("${simeng.use-engin-logs}")
    private String logsRedirect;

    /**
     * 创建仿真引擎实例
     */
    @PostMapping("/create_simeng")
    public ResponseEntity<ApiResponse<String>> createSimeng(
            @RequestBody Map<String, Object> simData,
            @CookieValue(value = "id", required = false) String id) {
        //判断用户有无拿到令牌
        if (id == null || !sessionServiceImpl.sessionExists(id)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ERR_AUTH", "unable to get id"));
        }

        SimInfo simInfo = sessionServiceImpl.getSessionInfo(id);
        //检查同一个用户是否只能创建一个引擎连接————用户单例
        if (simInfo.getSimeng_connection() != null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ERR_EXIST", "sim eng already exist"));
        }

        try {

            Map<String, Object> simInfoData = (Map<String, Object>) simData.get("sim_info");
            List<Map<String, Object>> controlViews = (List<Map<String, Object>>) simData.get("control_views");
            // 数据丢失
            if (simInfoData == null || controlViews == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("ERR_ARG", "missing simulation data"));
            }

            String simName = (String) simInfoData.get("name");
            simInfo.setName(simName != null ? simName : "test");
            simInfo.setSim_info(simInfoData);
            simInfo.setControl_views(controlViews);

            // 创建仿真插件目录
            Path simFilesDir = Paths.get(simengDir, id);
            Path simPluginDir = simFilesDir.resolve("plugins");
            simInfo.setSim_dir(simFilesDir.toString());
            Files.createDirectories(simFilesDir);
            Files.createDirectories(simPluginDir);
            // 复制路网文件
            simulationService.copyRoadNetFile(simInfo, simFilesDir);
            // 创建OD文件
            simulationService.createOdFile(simFilesDir, simInfoData);

            // 处理插件
            List<String> usePlugins = extractUsedPluginsPath(controlViews);
            if (!usePlugins.isEmpty()) {
                boolean copied = pluginService.copyPlugins(usePlugins, simPluginDir.toString());
                if (!copied) {
                    return ResponseEntity.internalServerError()
                            .body(ApiResponse.error("ERR_FILE", "copy plugin files error"));
                }
            }

            // 启动仿真引擎
            startSimulationEngine(id, simName, usePlugins);

            sessionServiceImpl.updateSessionInfo(id, simInfo);

            return ResponseEntity.ok(ApiResponse.success("ok"));

        } catch (Exception e) {
            log.error("Failed to create simulation engine", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("ERR_CREATE", "failed to create simulation engine"));
        }
    }



    /**
     * 提取使用的插件列表 提取插件 等待未来实现 ChonghaoGao仿制Tianyi Guo的后端所写 2025-9-12
     */
    private List<String> extractUsedPluginsPath(List<Map<String, Object>> controlViews) {
        List<String> usePluginPaths = new ArrayList<>();
        for (Map<String, Object> controlView : controlViews) {
            if ((boolean) controlView.get("use_plugin")) {
                usePluginPaths.addAll((List<String>) controlView.get("plugin_names"));
            }
        }
        // 这里简化处理，实际应该根据controlViews的结构来提取
        // 原Python代码中有复杂的插件提取逻辑

        return usePluginPaths;
    }

    private boolean useRedirect() {
        return Boolean.parseBoolean(this.logsRedirect);
    }

    /**
     * 启动仿真引擎
     */
    private void startSimulationEngine(String sessionId, String simName, List<String> usePlugins) {
        try {
            List<String> command = new ArrayList<>();
            //启动引擎前首先将引擎环境变量进行相应的配置，引擎采用自带的一套pyenv环境
//            boolean isSimEngPIEnvSuccess = initSimEngPIEnv();
            SimInfo simInfo = sessionServiceImpl.getSessionInfo(sessionId);
            String relativeRoadPath = getRoadPathName(simInfo);
            log.info("相对路径——{}",relativeRoadPath);
            if (!useRedirect()) {
                command.add("cmd.exe");
                command.add("/c");
                command.add("start");
            }
            log.info("出bug了{}",command);
            Path enginePath = Paths.get(simengDir, "SimulationEngine.exe");
            command.add(enginePath.toAbsolutePath().toString());
            command.add("--log=0");
            command.add("--sid=" + simName);
            command.add("--sfile=" + sessionId);
            command.add("--road=" + relativeRoadPath);
            // 添加插件参数
            if (usePlugins.isEmpty()) {
                command.add("--noplugin");
            } else {
                String pyHome = Paths.get(System.getProperty("user.dir"), "pyenv").toString();
                command.add("--pyhome=\"" + pyHome + "\"");
            }

            command.add("--ip=127.0.0.1");
            command.add("--port=3822");

            log.info("Starting simulation engine with command: {}", String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (useRedirect()) {
                //采用重定向的逻辑 当然这里需要配置一个全局线程池以便更为合法的控制任务的输出，这里为了省事儿就直接创建野生线程
                outPutRedirectToEngLogs(sessionId, processBuilder);
            } else {
                //没有重定向直接输出
                processBuilder.start();
            }
        } catch (Exception e) {
            log.error("Failed to start simulation engine", e);
        }
    }

    private void outPutRedirectToEngLogs(String sessionId, ProcessBuilder processBuilder) throws IOException {
        // 重定向输出到文件和控制台
        String logsDir = Paths.get(simengDir).toAbsolutePath() + "\\logs";
        File file = new File(logsDir);
        if (!file.exists()) file.mkdirs();
        File outputFile = new File(logsDir + "\\" + sessionId + "engine_output.log");
        processBuilder.redirectOutput(ProcessBuilder.Redirect.to(outputFile));
        processBuilder.redirectError(ProcessBuilder.Redirect.to(outputFile));

        Process process = processBuilder.start();

        // 实时读取输出 这里new 线程的方法十分不推荐，这样会导致线程可能脱离控制，无法统一集中管理，出现突发异常也无法知晓线程的工作状态
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("ENGINE: {}", line);
                    // 特别关注错误和异常信息
                    if (line.toLowerCase().contains("error") ||
                            line.toLowerCase().contains("exception") ||
                            line.toLowerCase().contains("fail")) {
                        log.error("ENGINE ERROR: {}", line);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading engine output", e);
            }
        }).start();
    }

    private static String getRoadPathName(SimInfo simInfo) {
        int pos = simInfo.getMap_xml_path().lastIndexOf("\\");
        String fileName = simInfo.getMap_xml_path().substring(pos + 1);
        return fileName;
    }

}
