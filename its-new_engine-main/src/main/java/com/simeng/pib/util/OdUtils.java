package com.simeng.pib.util;

import java.util.Map;

/**
 * @author ChonghaoGao
 * @date 2025/9/13 22:18)
 */
public class OdUtils {
    /**
     * 替换OD文件中的字段名
     */
    public static String replaceOdFieldNames(String odContent) {
        Map<String, String> replaceDict = Map.of(
                "road_num>", "roadNum>",
                "lane_num>", "laneNum>",
                "controller_num>", "controllerNum>",
                "follow_model>", "vehicleFollowModelNum>",
                "change_lane_model>", "vehicleChangeLaneModelNum>",
                "flows>", "flow>",
                "road_id>", "roadID>",
                "od>", "OD>",
                "</ObjectNode>", ""
        );

        Map<String, String> replaceDict2 = Map.of(
                "orgin_id>", "orginID>",
                "sg>", "SG>",
                "cross_id>", "crossID>",
                "cycle_time>", "cycleTime>",
                "ew_left>", "ewLeft>",
                "ew_straight>", "ewStraight>",
                "sn_left>", "snLeft>",
                "sn_straight>", "snStraight>",
                "<ObjectNode>", ""
        );

        for (Map.Entry<String, String> entry : replaceDict.entrySet()) {
            odContent = odContent.replace(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : replaceDict2.entrySet()) {
            odContent = odContent.replace(entry.getKey(), entry.getValue());
        }

        return odContent;
    }
}
