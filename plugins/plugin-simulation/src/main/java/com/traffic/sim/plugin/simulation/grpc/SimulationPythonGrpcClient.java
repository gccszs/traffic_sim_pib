package com.traffic.sim.plugin.simulation.grpc;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.dto.CreateSimulationRequest;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.plugin.simulation.grpc.proto.ControlView;
import com.traffic.sim.plugin.simulation.grpc.proto.CreateSimengRequest;
import com.traffic.sim.plugin.simulation.grpc.proto.Destination;
import com.traffic.sim.plugin.simulation.grpc.proto.FixedOD;
import com.traffic.sim.plugin.simulation.grpc.proto.GreenRatioControlRequest;
import com.traffic.sim.plugin.simulation.grpc.proto.OriginOD;
import com.traffic.sim.plugin.simulation.grpc.proto.PythonServiceGrpc;
import com.traffic.sim.plugin.simulation.grpc.proto.SignalGroup;
import com.traffic.sim.plugin.simulation.grpc.proto.SimInfo;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Python服务gRPC客户端
 * 支持容错机制：当gRPC服务不可用时返回兜底数据
 * 
 * 注意：即使 gRPC 服务不可用，这个 Bean 也会被创建，但会在运行时返回兜底数据
 * 
 * @author traffic-sim
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "grpc.client.python-service.enabled", havingValue = "true", matchIfMissing = true)
public class SimulationPythonGrpcClient {
    
    @Value("${grpc.client.python-service.enabled:true}")
    private boolean grpcEnabled;
    
    // 使用 @Lazy 延迟初始化，避免启动时失败
    // 如果 gRPC 服务不可用，字段将为 null，我们会在方法中处理
    @Lazy
    @GrpcClient("python-service")
    private PythonServiceGrpc.PythonServiceBlockingStub blockingStub;
    
    /**
     * 初始化时检查gRPC客户端是否可用
     * 注意：由于使用了 @Lazy，如果 gRPC 服务不可用，这里可能会抛出异常
     * 我们捕获异常并标记为不可用
     */
    @PostConstruct
    public void init() {
        if (!grpcEnabled) {
            log.info("gRPC client is disabled by configuration, will use fallback responses");
            return;
        }
        
        try {
            // 尝试访问 blockingStub（触发延迟初始化）
            // 如果 gRPC 服务不可用，这里可能会抛出异常
            if (blockingStub != null) {
                log.info("gRPC client for python-service is available");
            } else {
                log.warn("gRPC client for python-service is null, will use fallback responses");
            }
        } catch (Exception e) {
            log.warn("gRPC client initialization check failed, will use fallback responses: {}", e.getMessage());
            // 不抛出异常，允许应用继续启动
        }
    }
    
    /**
     * 检查gRPC是否可用
     * 注意：由于使用了 @Lazy，blockingStub 可能还未初始化
     * 我们通过尝试访问它来检查，如果抛出异常则不可用
     */
    private boolean isGrpcAvailable() {
        if (!grpcEnabled) {
            return false;
        }
        
        try {
            // 尝试访问 blockingStub（如果是 @Lazy，这里会触发初始化）
            // 如果 gRPC 服务不可用，这里可能会抛出异常
            return blockingStub != null;
        } catch (Exception e) {
            log.debug("gRPC client not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建仿真引擎
     * 
     * @param request 创建仿真请求
     * @param sessionId 会话ID（taskId），引擎将使用此ID连接WebSocket
     * @return API响应
     */
    public ApiResponse createSimeng(CreateSimulationRequest request, String sessionId) {
        // 如果gRPC未启用或不可用，返回兜底数据
        if (!isGrpcAvailable()) {
            log.warn("gRPC service is not available (enabled={}, stub={}), returning fallback response for createSimeng", 
                grpcEnabled, blockingStub != null);
            return createFallbackResponse("Simulation engine creation skipped (gRPC unavailable)", 
                "Please ensure Python gRPC service is running at localhost:50051");
        }
        
        try {
            CreateSimengRequest grpcRequest = convertToGrpcRequest(request, sessionId);
            
            return convertFromGrpcResponse(blockingStub.createSimeng(grpcRequest));
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for createSimeng, returning fallback response", e);
            return createFallbackResponse("Simulation engine creation failed: " + e.getMessage(), 
                "gRPC service error: " + e.getStatus().getCode());
        } catch (Exception e) {
            log.error("Unexpected error in createSimeng, returning fallback response", e);
            return createFallbackResponse("Simulation engine creation error: " + e.getMessage(), 
                "Unexpected error occurred");
        }
    }
    
    /**
     * 绿信比控制
     * 
     * @param greenRatio 绿信比值（0-100）
     * @return API响应
     */
    public ApiResponse controlGreenRatio(int greenRatio) {
        // 如果gRPC未启用或不可用，返回兜底数据
        if (!isGrpcAvailable()) {
            log.warn("gRPC service is not available (enabled={}, stub={}), returning fallback response for controlGreenRatio", 
                grpcEnabled, blockingStub != null);
            return createFallbackResponse("Green ratio control skipped (gRPC unavailable)", 
                "Please ensure Python gRPC service is running at localhost:50051");
        }
        
        try {
            GreenRatioControlRequest request = 
                GreenRatioControlRequest.newBuilder()
                    .setGreenRatio(greenRatio)
                    .build();
            
            return convertFromGrpcResponse(blockingStub.controlGreenRatio(request));
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for controlGreenRatio, returning fallback response", e);
            return createFallbackResponse("Green ratio control failed: " + e.getMessage(), 
                "gRPC service error: " + e.getStatus().getCode());
        } catch (Exception e) {
            log.error("Unexpected error in controlGreenRatio, returning fallback response", e);
            return createFallbackResponse("Green ratio control error: " + e.getMessage(), 
                "Unexpected error occurred");
        }
    }
    
    /**
     * 创建兜底响应
     * 
     * @param message 消息
     * @param data 数据
     * @return API响应
     */
    private ApiResponse createFallbackResponse(String message, String data) {
        ApiResponse response = new ApiResponse();
        response.setRes(ErrorCode.ERR_OK); // 返回成功，但提示gRPC不可用
        response.setMsg(message);
        response.setData(data);
        return response;
    }
    
    /**
     * 转换为gRPC请求
     * 
     * @param request 创建仿真请求
     * @param sessionId 会话ID（taskId），将作为userId字段传递给Python引擎
     */
    private CreateSimengRequest convertToGrpcRequest(
            CreateSimulationRequest request, String sessionId) {
        
        // 构建SimInfo
        CreateSimulationRequest.SimInfoDTO simInfoDTO = request.getSimInfo();
        SimInfo.Builder simInfoBuilder = SimInfo.newBuilder()
            .setName(simInfoDTO.getName())
            .setMapXmlName(simInfoDTO.getMapXmlName())
            .setMapXmlPath(simInfoDTO.getMapXmlPath());
        
        // 构建FixedOD
        if (simInfoDTO.getFixedOd() != null) {
            FixedOD.Builder fixedOdBuilder = FixedOD.newBuilder();
            
            CreateSimulationRequest.FixedODDTO fixedOdDTO = simInfoDTO.getFixedOd();
            
            // 设置基本字段
            if (fixedOdDTO.getRoadNum() != null) {
                fixedOdBuilder.setRoadNum(fixedOdDTO.getRoadNum());
            }
            if (fixedOdDTO.getLaneNum() != null) {
                fixedOdBuilder.setLaneNum(fixedOdDTO.getLaneNum());
            }
            if (fixedOdDTO.getControllerNum() != null) {
                fixedOdBuilder.setControllerNum(fixedOdDTO.getControllerNum());
            }
            if (fixedOdDTO.getFollowModel() != null) {
                fixedOdBuilder.setFollowModel(fixedOdDTO.getFollowModel());
            }
            if (fixedOdDTO.getChangeLaneModel() != null) {
                fixedOdBuilder.setChangeLaneModel(fixedOdDTO.getChangeLaneModel());
            }
            
            // 构建 flows 列表 - List<Map<String, Object>>
            if (fixedOdDTO.getFlows() != null) {
                log.info("Processing flows: count={}", fixedOdDTO.getFlows().size());
                for (Map<String, Object> flowMap : fixedOdDTO.getFlows()) {
                    com.traffic.sim.plugin.simulation.grpc.proto.Flow.Builder flowBuilder = 
                        com.traffic.sim.plugin.simulation.grpc.proto.Flow.newBuilder();
                    
                    if (flowMap.containsKey("roadId")) {
                        Object roadIdObj = flowMap.get("roadId");
                        flowBuilder.setRoadId(roadIdObj instanceof Number ? 
                            ((Number) roadIdObj).intValue() : 0);
                    } else if (flowMap.containsKey("road_id")) {
                        Object roadIdObj = flowMap.get("road_id");
                        flowBuilder.setRoadId(roadIdObj instanceof Number ? 
                            ((Number) roadIdObj).intValue() : 0);
                    }
                    
                    if (flowMap.containsKey("policy")) {
                        Object policyObj = flowMap.get("policy");
                        flowBuilder.setPolicy(policyObj instanceof Number ? 
                            ((Number) policyObj).intValue() : 0);
                    }
                    
                    if (flowMap.containsKey("demand")) {
                        Object demandObj = flowMap.get("demand");
                        flowBuilder.setDemand(demandObj instanceof Number ? 
                            ((Number) demandObj).intValue() : 0);
                    }
                    
                    if (flowMap.containsKey("extra")) {
                        Object extraObj = flowMap.get("extra");
                        flowBuilder.setExtra(extraObj instanceof Number ? 
                            ((Number) extraObj).intValue() : 0);
                    }
                    
                    fixedOdBuilder.addFlows(flowBuilder.build());
                }
            }
            
            // 构建OD列表 - 现在是 List<Map<String, Object>>
            if (fixedOdDTO.getOd() != null) {
                for (Map<String, Object> originODMap : fixedOdDTO.getOd()) {
                    OriginOD.Builder originBuilder = OriginOD.newBuilder();
                    
                    // 从 Map 中提取 originId（支持多种命名格式）
                    String originId = null;
                    if (originODMap.containsKey("originId")) {
                        originId = String.valueOf(originODMap.get("originId"));
                    } else if (originODMap.containsKey("orginId")) {
                        originId = String.valueOf(originODMap.get("orginId"));
                    } else if (originODMap.containsKey("orgin_id")) {
                        originId = String.valueOf(originODMap.get("orgin_id"));
                    }
                    
                    if (originId != null && !originId.equals("null")) {
                        originBuilder.setOriginId(originId);
                        log.info("Processing origin: originId={}", originId);
                    } else {
                        log.warn("Origin ID is null or missing in map: {}", originODMap.keySet());
                    }
                    
                    // 处理 dist 列表
                    Object distObj = originODMap.get("dist");
                    if (distObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> distList = (List<Map<String, Object>>) distObj;
                        for (Map<String, Object> destMap : distList) {
                            Destination.Builder destBuilder = Destination.newBuilder();
                            
                            if (destMap.containsKey("destId")) {
                                destBuilder.setDestId(String.valueOf(destMap.get("destId")));
                            } else if (destMap.containsKey("dest")) {
                                destBuilder.setDestId(String.valueOf(destMap.get("dest")));
                            }
                            
                            if (destMap.containsKey("rate")) {
                                Object rateObj = destMap.get("rate");
                                destBuilder.setRate(rateObj instanceof Number ? 
                                    ((Number) rateObj).doubleValue() : 0.0);
                            } else if (destMap.containsKey("percent")) {
                                Object percentObj = destMap.get("percent");
                                destBuilder.setRate(percentObj instanceof Number ? 
                                    ((Number) percentObj).doubleValue() : 0.0);
                            }
                            
                            originBuilder.addDist(destBuilder.build());
                        }
                    }
                    
                    fixedOdBuilder.addOd(originBuilder.build());
                }
            }
            
            // 构建信号灯组列表 - 现在是 List<Map<String, Object>>
            if (fixedOdDTO.getSg() != null) {
                for (Map<String, Object> sgMap : fixedOdDTO.getSg()) {
                    SignalGroup.Builder sgBuilder = SignalGroup.newBuilder();
                    
                    if (sgMap.containsKey("crossId")) {
                        Object crossIdObj = sgMap.get("crossId");
                        sgBuilder.setCrossId(crossIdObj instanceof Number ? 
                            ((Number) crossIdObj).intValue() : 0);
                    } else if (sgMap.containsKey("cross_id")) {
                        Object crossIdObj = sgMap.get("cross_id");
                        sgBuilder.setCrossId(crossIdObj instanceof Number ? 
                            ((Number) crossIdObj).intValue() : 0);
                    }
                    
                    if (sgMap.containsKey("cycleTime")) {
                        Object cycleTimeObj = sgMap.get("cycleTime");
                        sgBuilder.setCycleTime(cycleTimeObj instanceof Number ? 
                            ((Number) cycleTimeObj).intValue() : 0);
                    } else if (sgMap.containsKey("cycle_time")) {
                        Object cycleTimeObj = sgMap.get("cycle_time");
                        sgBuilder.setCycleTime(cycleTimeObj instanceof Number ? 
                            ((Number) cycleTimeObj).intValue() : 0);
                    }
                    
                    if (sgMap.containsKey("ewStraight")) {
                        Object ewStraightObj = sgMap.get("ewStraight");
                        sgBuilder.setEwStraight(ewStraightObj instanceof Number ? 
                            ((Number) ewStraightObj).intValue() : 0);
                    } else if (sgMap.containsKey("ew_straight")) {
                        Object ewStraightObj = sgMap.get("ew_straight");
                        sgBuilder.setEwStraight(ewStraightObj instanceof Number ? 
                            ((Number) ewStraightObj).intValue() : 0);
                    }
                    
                    if (sgMap.containsKey("snStraight")) {
                        Object snStraightObj = sgMap.get("snStraight");
                        sgBuilder.setSnStraight(snStraightObj instanceof Number ? 
                            ((Number) snStraightObj).intValue() : 0);
                    } else if (sgMap.containsKey("sn_straight")) {
                        Object snStraightObj = sgMap.get("sn_straight");
                        sgBuilder.setSnStraight(snStraightObj instanceof Number ? 
                            ((Number) snStraightObj).intValue() : 0);
                    }
                    
                    if (sgMap.containsKey("snLeft")) {
                        Object snLeftObj = sgMap.get("snLeft");
                        sgBuilder.setSnLeft(snLeftObj instanceof Number ? 
                            ((Number) snLeftObj).intValue() : 0);
                    } else if (sgMap.containsKey("sn_left")) {
                        Object snLeftObj = sgMap.get("sn_left");
                        sgBuilder.setSnLeft(snLeftObj instanceof Number ? 
                            ((Number) snLeftObj).intValue() : 0);
                    }
                    
                    // 添加 ewLeft 字段支持
                    if (sgMap.containsKey("ewLeft")) {
                        Object ewLeftObj = sgMap.get("ewLeft");
                        sgBuilder.setEwLeft(ewLeftObj instanceof Number ? 
                            ((Number) ewLeftObj).intValue() : 0);
                    } else if (sgMap.containsKey("ew_left")) {
                        Object ewLeftObj = sgMap.get("ew_left");
                        sgBuilder.setEwLeft(ewLeftObj instanceof Number ? 
                            ((Number) ewLeftObj).intValue() : 0);
                    }
                    
                    fixedOdBuilder.addSg(sgBuilder.build());
                }
            }
            
            simInfoBuilder.setFixedOd(fixedOdBuilder.build());
        }
        
        // 构建ControlViews
        List<ControlView> controlViews = new ArrayList<>();
        if (request.getControlViews() != null) {
            for (CreateSimulationRequest.ControlViewDTO cv : request.getControlViews()) {
                ControlView controlView = 
                    ControlView.newBuilder()
                        .setUsePlugin(cv.getUsePlugin() != null ? cv.getUsePlugin() : false)
                        .setActivePlugin(cv.getActivePlugin() != null ? cv.getActivePlugin() : "")
                        .build();
                controlViews.add(controlView);
            }
        }
        
        // 构建完整请求
        return CreateSimengRequest.newBuilder()
            .setSimInfo(simInfoBuilder.build())
            .addAllControlViews(controlViews)
            .setUserId(sessionId)  // 注意：proto字段名是userId，但实际传递的是sessionId（taskId）
            .build();
    }
    
    /**
     * 从gRPC响应转换
     */
    private ApiResponse convertFromGrpcResponse(com.traffic.sim.plugin.simulation.grpc.proto.ApiResponse grpcResponse) {
        ApiResponse response = new ApiResponse();
        response.setRes(grpcResponse.getRes());
        response.setMsg(grpcResponse.getMsg());
        response.setData(grpcResponse.getData());
        return response;
    }
}

