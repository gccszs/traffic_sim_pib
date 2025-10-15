package edu.uestc.iscssl.itsbackend.controller;

import edu.uestc.iscssl.common.common.SIMULATION_STATUS;
import edu.uestc.iscssl.itsbackend.service.ExperimentService;
import edu.uestc.iscssl.itsbackend.service.SimulationService;
import edu.uestc.iscssl.itsbackend.service.UserService;
import edu.uestc.iscssl.itsbackend.utils.R;
import edu.uestc.iscssl.itsbackend.utils.UserUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@CrossOrigin
@RestController
@Api(value = "登录页、首页统计人数的相关API",description = "登录页、首页统计人数的相关API")
public class StatisticController {
    @Autowired
    UserService userService;
    @Autowired
    SimulationService simulationService;
    @Autowired
    ExperimentService experimentService;

    @ApiOperation(value = "登录页查看所有注册用户的人数、正在仿真的人数、已停止的人数",notes = "无参数，不需要权限")
    @RequestMapping(value = "/getStatistic",method = RequestMethod.GET)
    public R getStatistic() throws IOException {
        Map<String,Object> result=new HashMap<>();
        result.put("totalUserNumber",userService.getTotalUserNumber());
//        result.put("running",simulationService.getSimulationNumberByStatus(SIMULATION_STATUS.RUNNING));
//        result.put("stop",simulationService.getSimulationNumberByStatus(SIMULATION_STATUS.STOP));
        return R.ok(result);
    }
    @ApiOperation(value = "首页查看当前用户的仿真次数和仿真时间(步数)",notes = "无参数，需要管理员权限，携带token")
    @RequestMapping(value = "/getUserStatistic",method = RequestMethod.GET)
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public R getUserStatistic(){
        long userId = UserUtils.getUserId();
        Map<String,Object> result=new HashMap<>();
        result.put("simulationNumber",experimentService.getExperimentNumberByUserId(userId));
        result.put("simulationTime",simulationService.getSimulationStepByUserId(userId));
        return R.ok(result);
    }

    @ApiOperation(value = "统计各种实验类型的次数")
    @RequestMapping(value = "/getTypeTimes",method = RequestMethod.GET)
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public R getTypeTimes(){
        long id  = UserUtils.getUserId();
        long roleId = UserUtils.getRoleId();
        Map<String, Object> result = new HashMap<>();
        List<Map<Integer, Integer>> temp = new ArrayList<>();
        if (1 == roleId) {
            temp = experimentService.countTypeByUserId(id);
        }
        else if (2 == roleId){
            temp = experimentService.countTypes();
        }
        for (Map<Integer, Integer> map : temp) {
            if(map.get("type") == null){
                continue;
            }
            if (map.get("type") <= 9)
                result.put(String.valueOf(map.get("type")), map.get("num"));
            else {
                result.put(String.valueOf(map.get("type") / 10), map.get("num"));
            }
        }
        for (int i = 0; i <= 9; i++) {
            if (result.get(String.valueOf(i)) == null)
                result.put(String.valueOf(i), 0);
        }
        return R.ok(result);
    }
}
