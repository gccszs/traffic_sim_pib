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
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
     * @param userId 用户ID
     * @return API响应
     */
    public ApiResponse createSimeng(CreateSimulationRequest request, String userId) {
        // 如果gRPC未启用或不可用，返回兜底数据
        if (!isGrpcAvailable()) {
            log.warn("gRPC service is not available (enabled={}, stub={}), returning fallback response for createSimeng", 
                grpcEnabled, blockingStub != null);
            return createFallbackResponse("Simulation engine creation skipped (gRPC unavailable)", 
                "Please ensure Python gRPC service is running at localhost:50051");
        }
        
        try {
            CreateSimengRequest grpcRequest = convertToGrpcRequest(request, userId);
            
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
     */
    private CreateSimengRequest convertToGrpcRequest(
            CreateSimulationRequest request, String userId) {
        
        // 构建SimInfo
        CreateSimulationRequest.SimInfoDTO simInfoDTO = request.getSimInfo();
        SimInfo.Builder simInfoBuilder = SimInfo.newBuilder()
            .setName(simInfoDTO.getName())
            .setMapXmlName(simInfoDTO.getMapXmlName())
            .setMapXmlPath(simInfoDTO.getMapXmlPath());
        
        // 构建FixedOD
        if (simInfoDTO.getFixedOd() != null) {
            FixedOD.Builder fixedOdBuilder = FixedOD.newBuilder();
            
            // 构建OD列表
            if (simInfoDTO.getFixedOd().getOd() != null) {
                for (CreateSimulationRequest.OriginODDTO originOD : simInfoDTO.getFixedOd().getOd()) {
                    OriginOD.Builder originBuilder = 
                        OriginOD.newBuilder()
                            .setOriginId(originOD.getOriginId());
                    
                    if (originOD.getDist() != null) {
                        for (CreateSimulationRequest.DestinationDTO dest : originOD.getDist()) {
                            Destination destination = 
                                Destination.newBuilder()
                                    .setDestId(dest.getDestId())
                                    .setRate(dest.getRate() != null ? dest.getRate() : 0.0)
                                    .build();
                            originBuilder.addDist(destination);
                        }
                    }
                    
                    fixedOdBuilder.addOd(originBuilder.build());
                }
            }
            
            // 构建信号灯组列表
            if (simInfoDTO.getFixedOd().getSg() != null) {
                for (CreateSimulationRequest.SignalGroupDTO sg : simInfoDTO.getFixedOd().getSg()) {
                    SignalGroup signalGroup = 
                        SignalGroup.newBuilder()
                            .setCrossId(sg.getCrossId() != null ? sg.getCrossId() : 0)
                            .setCycleTime(sg.getCycleTime() != null ? sg.getCycleTime() : 0)
                            .setEwStraight(sg.getEwStraight() != null ? sg.getEwStraight() : 0)
                            .setSnStraight(sg.getSnStraight() != null ? sg.getSnStraight() : 0)
                            .setSnLeft(sg.getSnLeft() != null ? sg.getSnLeft() : 0)
                            .build();
                    fixedOdBuilder.addSg(signalGroup);
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
            .setUserId(userId)
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

