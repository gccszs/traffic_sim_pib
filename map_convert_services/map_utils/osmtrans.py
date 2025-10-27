from collections import Counter
from xml.dom.minidom import parse, Element, Document
import os, json, traceback, math, sys
import numpy as np
import sympy
from gekko import GEKKO
from sympy import *
import time

#用于禁止print输出
import contextlib
import io

"""
2023-6-17 
ly
增加拟合曲线以及计算退距点的功能;
"""
"""
2023-8-23
ly
1.退距已实现
2.合路已实现
3.相位问题已解决
4.车道问题暂未实现，暂时想法为根据车道说明文档进行车道数目的设定
"""


# 解析单个文件

def save_dict_to_json(data, json_file='data.json'):
    with open(json_file, 'w') as f:
        json.dump(data, f, indent=4)


def show_data(d):
    import pprint
    pp = pprint.PrettyPrinter(indent=2)
    pp.pprint(d)


class deal_xml:
    def __init__(self, file, save_file) -> None:
        self.file = file
        self.save_file = save_file

    def parse_xml(self):
        global copy_way_nd_list, jx_point1, jx_point2
        result_txt = ''
        dom = parse(self.file)
        # 获取文档元素对象
        data = dom.documentElement
        # 获取所有的node节点
        sour_node_nd_list = data.getElementsByTagName("node")  # sour_node_nd_list是node的list
        self.node_id_dict = {}  # Key:node_id      Value:node_id对应的node节点
        for one_node_nd in sour_node_nd_list:
            node_id = one_node_nd.getAttribute('id')
            self.node_id_dict[node_id] = one_node_nd
        # 定义全局变量
        # way即为没有分段形成新路之前的路的别称，由一系列nd构成

        # k:way_id v:way_name (way的id与way的名字的字典)
        way_id_way_name_dict = {}

        # 相同名字way的集合 eg:[['1214545','4565456'], ['546456','456465']]
        same_name_way_id_list = []

        # 要删除的way的id合集，用于定义删除线和非删除线 eg:['1145120', '41665656']
        new_same_name_way_id_list = []

        # k:way_id v:way_nd_list (way的id和way的节点列表的字典)
        way_id_way_nd_dict = {}

        # k:way_id v:way_nd_list (同名way的id和way的节点列表的字典)
        more_than_two_way_id_way_nd_list_dict = {}

        # k:intersection_pont v:way_id eg:{'12345647':'45645456',,,,} (交点与所交的way的nd_list的字典)
        way_intersection_points_way_nd_list_dict = {}

        # 获取所有的way
        sour_way_nd_list = data.getElementsByTagName("way")

        # 保存最终合路所有操作之后完美的way_nd_list
        sour_true_way_nd_list = []

        # k:class v:way_id
        class_way_dict = {}

        # tag中k=“highway”的才是路
        for one_way_nd in sour_way_nd_list:
            one_way_tag_nd_list = one_way_nd.getElementsByTagName('tag')
            way_id = one_way_nd.getAttribute('id')
            is_way = False
            for one_tag in one_way_tag_nd_list:
                if one_tag.getAttribute('k') == 'highway':
                    is_way = True
                if is_way:
                    if one_tag.getAttribute('k') == 'name':
                        tag_v = one_tag.getAttribute('v')
                        way_id_way_name_dict[way_id] = tag_v
                    # # 不同等级的路段
                    # if one_tag.getAttribute('v') == 'primary':
                    #     class_way_dict.setdefault(['primary'], []).append(way_id)
                    # if one_tag.getAttribute('v') == 'residential':
                    #     class_way_dict.setdefault(['residential'], []).append(way_id)
                    # if one_tag.getAttribute('v') == 'tertiary':
                    #     class_way_dict.setdefault(['tertiary'], []).append(way_id)
                    # if one_tag.getAttribute('v') == 'pedestrian':
                    #     class_way_dict.setdefault(['pedestrian'], []).append(way_id)

                        break
            if is_way:
                # one_way_nd_nd_list保存一条way中的所有nd
                one_way_nd_nd_list = one_way_nd.getElementsByTagName('nd')
                way_nd_id_list = [i.getAttribute('ref') for i in one_way_nd_nd_list]

                # 记录了每条way的nd_list
                way_id_way_nd_dict[way_id] = way_nd_id_list

        print("未合并之前的整体way:", way_id_way_nd_dict, "\n")

        # 出循环，统计次数,找到需要合路的way，即名字相同的路
        counter = Counter(way_id_way_name_dict.values())
        for element, count in counter.items():
            if count >= 2:
                # 获得相同名字的way的nd_list eg:人民北路['12123','54654'],['65456','56465']
                same_name_way_id_list.append([k for k, v in way_id_way_name_dict.items() if v == element])

        # 分别用于存储字典中‘1’ 和 ‘2’ 对应way的id
        way_id_1_1 = ' '
        way_id_1_2 = ' '

        # 用于判断把第一个放进字典的数据id放入删除线备选列表中
        i_1 = 1
        i_2 = 1
        if len(same_name_way_id_list) > 0:
            print("需要合成为两个way的同名的way的id列表:", same_name_way_id_list, "\n")

            for idx, one_same_name_way_id_list in enumerate(same_name_way_id_list):

                print("本轮参与的是", one_same_name_way_id_list, "\n")

                # 找到需要进行操作的way
                nd_list = []
                for idx2, one_id in enumerate(one_same_name_way_id_list):
                    more_than_two_way_id_way_nd_list_dict[one_id] = way_id_way_nd_dict[one_id]
                    nd_list.append(more_than_two_way_id_way_nd_list_dict[one_id])

                print("本轮参与的是(节点序列)", nd_list, "\n")

                # 定义一个大小固定为3的字典，用于整理数据，使其合way，同名路永远合为两条way
                three_key_dict = {'1': nd_list[0], '2': [], '3': []}
                new_same_name_way_id_list.append([k for k, v in way_id_way_nd_dict.items() if v == three_key_dict['1']])

                print("整理合路字典最开始数据", three_key_dict, "\n")

                for one_nd_list in nd_list:
                    # nd_list[0]不参与
                    if one_nd_list == nd_list[0]:
                        continue
                    # 判断其第二个nd_list与第一位的值是否有交点
                    jc_point = (list(set(three_key_dict['1']).intersection(set(one_nd_list))))
                    if jc_point:
                        if i_1 == 1:
                            way_id_1_1 = [k for k, v in way_id_way_nd_dict.items() if v == three_key_dict['1']]
                            if way_id_1_1 not in new_same_name_way_id_list:
                                new_same_name_way_id_list.append(way_id_1_1)

                                print("删除后备序列本次添加:", way_id_1_1, "\n")

                            i_1 += 1
                        way_id_2 = [k for k, v in way_id_way_nd_dict.items() if v == one_nd_list]

                        print("当前找到的需要删除的way_id:", way_id_2, "\n")

                        # 合并调整操作
                        self.hb_to_two_way(jc_point, three_key_dict['1'], one_nd_list, way_id_way_nd_dict, way_id_1_1,
                                           way_id_2)
                    else:
                        if not three_key_dict['2']:
                            three_key_dict['2'] = one_nd_list
                            new_same_name_way_id_list.append(
                                [k for k, v in way_id_way_nd_dict.items() if v == one_nd_list])
                        else:
                            jc_point2 = (list(set(three_key_dict['2']).intersection(set(one_nd_list))))
                            if jc_point2:
                                if i_2 == 1:
                                    way_id_1_2 = [k for k, v in way_id_way_nd_dict.items() if v == three_key_dict['2']]
                                    if way_id_1_2 not in new_same_name_way_id_list:
                                        new_same_name_way_id_list.append(way_id_1_2)

                                        print("删除后备本次添加:", new_same_name_way_id_list, "\n")

                                    i_2 += 1
                                way_id_2 = [k for k, v in way_id_way_nd_dict.items() if v == one_nd_list]

                                print("当前找到的需要删除的way_id:", way_id_2, "\n")

                                self.hb_to_two_way(jc_point2, three_key_dict['2'], one_nd_list, way_id_way_nd_dict,
                                                   way_id_1_2,
                                                   way_id_2)
                            else:
                                three_key_dict['3'].append(one_nd_list)

                # 当‘3’中数据为空时，说明已经全部整理好了
                while three_key_dict['3']:
                    for v1 in three_key_dict['3']:
                        jc_point3 = list(set(three_key_dict['1']).intersection(set(v1)))
                        if jc_point3:
                            way_id_2 = [k for k, v in way_id_way_nd_dict.items() if v == v1]

                            print("当前找到的需要删除的way_id:", way_id_2, "\n")

                            self.hb_to_two_way(jc_point3, three_key_dict['1'], v1, way_id_way_nd_dict, way_id_1_1,
                                               way_id_2)
                        else:
                            jc_point4 = list(set(three_key_dict['2']).intersection(set(v1)))
                            if jc_point4:
                                way_id_2 = [k for k, v in way_id_way_nd_dict.items() if v == v1]
                                self.hb_to_two_way(jc_point3, three_key_dict['2'], v1, way_id_way_nd_dict, way_id_1_2,
                                                   way_id_2)

                print("最终字典数据:", three_key_dict, "\n")

                print("本组way完结\n")

            print("同名way序列:", new_same_name_way_id_list, "\n")

            print("最终要参与分段的way集合:", way_id_way_nd_dict, "\n")

            if len(new_same_name_way_id_list) > 1:
                # 求出所有线之间的交点
                jc_points_list = []
                way_nd_list = [v for k, v in way_id_way_nd_dict.items()]
                for idx, one_way_nd in enumerate(way_nd_list):
                    copy_new_way_nd_list = way_nd_list[idx + 1:]
                    for idx2, one_copy_way_nd in enumerate(copy_new_way_nd_list):
                        jc_point2 = list(set(one_way_nd).intersection(set(one_copy_way_nd)))
                        if jc_point2:
                            if jc_point2 not in jc_points_list:
                                jc_points_list.append(list(set(one_way_nd).intersection(set(one_copy_way_nd))))

                # 找到交点的对应线，即哪几条线交于这个点
                for idx, one_way_nd in way_id_way_nd_dict.items():
                    for k in range(0, len(jc_points_list)):
                        if jc_points_list[k][0] in one_way_nd:
                            way_intersection_points_way_nd_list_dict.setdefault(jc_points_list[k][0], []).append(
                                one_way_nd)

                # 存储人民北路这种穿过所有的交点的同名路

                delete_way_list_1 = []
                for one_id in new_same_name_way_id_list:
                    i = 0

                    print("本轮是线", one_id, "\n")

                    for one_point in jc_points_list:

                        print("本轮的点", one_point[0], "\n")

                        if one_point[0] == way_id_way_nd_dict[one_id[0]][0] or one_point[0] == \
                                way_id_way_nd_dict[one_id[0]][-1]:
                            print("是起始点\n")

                            i += 1
                    if i == 0:
                        print("添加!\n")

                        delete_way_list_1.append(one_id)

                # 定义删除线和非删除线
                delete_way_id = delete_way_list_1[0]
                not_delete_way_id = delete_way_list_1[1]

                print("暂定删除线:", delete_way_id, "\n")

                print("暂定非删除线:", not_delete_way_id, "\n")

                # 存放着真正要删除的way的id
                true_delete_way_id_list = []
                # 找到了交点所交的三条线的情况，并记录第一组，设为删去的组别
                if len(same_name_way_id_list) >= 3:
                    for one_point, one_nd_list in way_intersection_points_way_nd_list_dict.items():
                        if len(one_nd_list) == 3:
                            true_delete_way_id_list.append(one_nd_list[0])
                            true_delete_way_id_list.append(one_nd_list[1])
                            true_delete_way_id_list.append(one_nd_list[2])
                            break
                else:
                    true_delete_way_id_list.append(delete_way_id)

                # 调整删除线，让其与true_delete_way_id_list保持一致
                for v_delete in true_delete_way_id_list:
                    id_need = [k for k, v in way_id_way_nd_dict.items() if v == v_delete]
                    if delete_way_id == id_need or not_delete_way_id == id_need:
                        if delete_way_id != id_need:
                            print("进行删除线调换\n")

                            not_delete_way_id = delete_way_id
                            delete_way_id = id_need
                    else:
                        continue

                print("需要删去的way的nd_list:", true_delete_way_id_list, "\n")

                print("开始本轮删除线操作:\n")

                print("正式删除线:", delete_way_id, "\n")

                print("正式非删除线:", not_delete_way_id, "\n")

                print("所有交点:", jc_points_list, "\n")

                # 以每个点为循环
                for one_point in jc_points_list:

                    print("本轮循环的点为:", one_point[0], "\n")

                    # 如果不在删除线上
                    if one_point[0] not in way_id_way_nd_dict[delete_way_id[0]]:
                        if len(way_intersection_points_way_nd_list_dict[one_point[0]]) == 2:
                            point2 = ' '
                            for k in way_intersection_points_way_nd_list_dict[one_point[0]]:
                                if k != way_id_way_nd_dict[not_delete_way_id[0]]:
                                    # 位于此线的终点
                                    if one_point[0] == k[-1] or one_point[0] == k[0]:
                                        continue
                                    else:
                                        # 缩短
                                        is_j = list(set(way_id_way_nd_dict[delete_way_id[0]]).intersection(set(k)))
                                        if is_j:
                                            point2 = is_j[0]
                                        if point2 == k[-1]:

                                            print("缩短操作", "\n")

                                            # 先用值k求出键way_id,删除索引i以后的元素(nd)
                                            i = 0
                                            for k1 in k:
                                                i += 1
                                                if one_point[0] == k1:
                                                    break

                                            print("删除第几位", i)

                                            way_id = [k2 for k2, v in way_id_way_nd_dict.items() if v == k]

                                            print("删除前\n", k)

                                            del k[i - 1:]

                                            print("删除后\n", k)

                                            way_id_way_nd_dict[way_id[0]] = k
                                        else:
                                            i = 0
                                            for k1 in k:
                                                i += 1
                                                if one_point[0] == k1:
                                                    break

                                            print("删除第几位", i)

                                            way_id = [k2 for k2, v in way_id_way_nd_dict.items() if v == k]

                                            print("删除前\n", k)

                                            del k[:i - 1]

                                            print("删除后\n", k)

                                            way_id_way_nd_dict[way_id[0]] = k
                        # 有三条线交于此点
                        elif len(way_intersection_points_way_nd_list_dict[one_point[0]]) == 3:
                            continue
                    else:
                        if len(way_intersection_points_way_nd_list_dict[one_point[0]]) == 3:

                            print("延长操作:\n")

                            point = ' '
                            for k in way_intersection_points_way_nd_list_dict[one_point[0]]:
                                is_j = list(set(way_id_way_nd_dict[not_delete_way_id[0]]).intersection(set(k)))
                                print("值:", is_j)
                                if is_j:
                                    point = is_j[0]

                                    print("延长点为", point)

                                if k != way_id_way_nd_dict[delete_way_id[0]]:
                                    if not is_j:
                                        way_id2 = [k3 for k3, v in way_id_way_nd_dict.items() if v == k]
                                        is_j2 = list(set(way_id_way_nd_dict[delete_way_id[0]]).intersection(set(k)))
                                        if is_j2:
                                            # if one_point[0] != k[0] and one_point[0] != k[-1]:
                                            #     continue
                                            # else:
                                            if is_j2[0] == k[-1]:

                                                print("要延长的点为:", point, "\n")

                                                if point not in k and point != ' ':
                                                    way_id_way_nd_dict[way_id2[0]].append(point)
                                            if is_j2[0] == k[0]:
                                                if point not in k and point != ' ':
                                                    way_id_way_nd_dict[way_id2[0]].reverse()
                                                    way_id_way_nd_dict[way_id2[0]].append(point)
                        else:
                            # 找删除线上最近的点给非delete_way
                            for k in way_intersection_points_way_nd_list_dict[one_point[0]]:
                                # 是终点
                                if one_point[0] == k[-1]:
                                    for k2 in way_intersection_points_way_nd_list_dict[one_point[0]]:
                                        distance_list = []
                                        if k2 != way_id_way_nd_dict[delete_way_id[0]]:
                                            nd_list2 = way_id_way_nd_dict[not_delete_way_id[0]]
                                            for v in nd_list2:
                                                dis = self.get_distance(one_point[0], v)
                                                distance_list.append(dis)
                                                min_dis = min(distance_list)
                                                if dis == min_dis:
                                                    way_id3 = [k3 for k3, v in way_id_way_nd_dict.items() if v == k]
                                                    way_id_way_nd_dict[way_id3[0]].append(v)
                                else:
                                    continue

                print("本轮完成\n")

                # 同名路超过两组
                print(true_delete_way_id_list)
                print("1：", way_id_way_nd_dict)
                if len(same_name_way_id_list) > 2:
                    way_id_way_nd_dict.pop(
                        [k3 for k3, v in way_id_way_nd_dict.items() if v == true_delete_way_id_list[0]][0])
                    way_id_way_nd_dict.pop(
                        [k3 for k3, v in way_id_way_nd_dict.items() if v == true_delete_way_id_list[1]][0])
                    way_id_way_nd_dict.pop(
                        [k3 for k3, v in way_id_way_nd_dict.items() if v == true_delete_way_id_list[2]][0])
                else:
                    way_id_way_nd_dict.pop(delete_way_id[0])
        print("生成:", way_id_way_nd_dict)

        for new_id, way_nd_list in way_id_way_nd_dict.items():
            sour_true_way_nd_list.append(way_nd_list)

        print("最终参与分段的way:", sour_true_way_nd_list, "\n")

        # 计算出所有的cross id
        cross_id_way_idx_dict = {}  # k: cross_id  v ： sour_true_way_nd_list 中的索引值 ， 用来后期的分段
        for idx, one_way_nd_id_list in enumerate(sour_true_way_nd_list):
            dest_nested_list = sour_true_way_nd_list[idx + 1:]  # 创建sour_true_way_nd_list的副本
            for idx2, one_dest_list in enumerate(dest_nested_list):
                intersection = list(set(one_dest_list).intersection(set(one_way_nd_id_list)))  ## 查看两个way 是否存在 交叉点
                if intersection:  # ['6704947951', '1885342911'] 18 119
                    # print(intersection, idx, idx2 + idx + 1)
                    for k in intersection:  # k是cross_id
                        if k not in cross_id_way_idx_dict:
                            cross_id_way_idx_dict[k] = [idx, idx2 + idx + 1]  # idx,idx2+idx+1记录k是那两条路的交叉
                        else:
                            cross_id_way_idx_dict[k].extend([idx, idx2 + idx + 1])
                            cross_id_way_idx_dict[k] = list(set(cross_id_way_idx_dict[k]))

        true_way_idx_cross_id_dict = {}  # k: sour_true_way_idx  v ： cross_id_list ， 用来后期的分段
        for cross_id, true_way_idx_list in cross_id_way_idx_dict.items():
            for one_way_idx in true_way_idx_list:
                if one_way_idx in true_way_idx_cross_id_dict:
                    if cross_id not in true_way_idx_cross_id_dict[one_way_idx]:
                        true_way_idx_cross_id_dict[one_way_idx].append(cross_id)
                else:
                    true_way_idx_cross_id_dict[one_way_idx] = [cross_id]
        '''{ 0: ['6868728845', '5382831200', '1885342912'],
            1: ['6868728845'],
            2: ['1885342916'],
            3: ['1885342916', '5382831201'],
            4: ['5382831200', '5382831201'],
            5: ['1885342912']}'''

        # 4. 根据 cross_id 来给 way 分段
        road_nd_list = []  # 将路分段
        for true_way_idx, one_way_nd_id_list in enumerate(sour_true_way_nd_list):
            if true_way_idx not in true_way_idx_cross_id_dict:
                continue
            cross_id_list = true_way_idx_cross_id_dict[true_way_idx]  # 一条路所对应的多个cross_id
            cross_id_index_list = [one_way_nd_id_list.index(i) for i in cross_id_list]  # 上面这些cross_id的索引
            cross_id_index_sort = sorted(cross_id_index_list)  # 排序
            cur_start_index = 0  # 当前索引位置
            for one_cross_id_index in cross_id_index_sort:
                if one_cross_id_index == 0:
                    continue
                sub_road_nd_list = one_way_nd_id_list[cur_start_index:one_cross_id_index + 1]
                road_nd_list.append(sub_road_nd_list)
                cur_start_index = one_cross_id_index
            if cur_start_index != len(one_way_nd_id_list) - 1:  # 如果没有走到这条路的尽头
                sub_road_nd_list = one_way_nd_id_list[cur_start_index:]
                road_nd_list.append(sub_road_nd_list)

        print("最终形成的路:", road_nd_list, "\n")

        # 5. 生成road相关数据 -- txt1
        road_id_road_nd_cross_id_dict = {}
        for idx3, one_road_nd in enumerate(road_nd_list):
            # lon_1 = 0
            # lat_1 = 0
            # x1 = 0
            # y1 = 0
            # lon_2 = 0
            # lat_2 = 0
            # x2 = 0
            # y2 = 0
            road_id = idx3 + 1
            start_nd = one_road_nd[0]
            end_nd = one_road_nd[-1]
            p_1 = one_road_nd[0]
            p_2 = one_road_nd[1]
            p1 = self.get_node_pos(p_1)
            p2 = self.get_node_pos(p_2)
            if self.judge_two_node_left_right(start_nd, end_nd) and p1[0] < p2[0] and p1[1] < p2[1]:
                left_nd = end_nd
                right_nd = start_nd
            else:
                right_nd = end_nd
                left_nd = start_nd
            road_id_road_nd_cross_id_dict[road_id] = {'road_nd_list': one_road_nd, 'left_cross_id': '',
                                                      'right_cross_id': ''}  # 双字典
            if left_nd in cross_id_way_idx_dict:
                road_id_road_nd_cross_id_dict[road_id]['left_cross_id'] = left_nd
                # road_id_road_nd_cross_id_dict[road_id]['road_nd_list'].remove(left_nd)
            if right_nd in cross_id_way_idx_dict:
                road_id_road_nd_cross_id_dict[road_id]['right_cross_id'] = right_nd
                # road_id_road_nd_cross_id_dict[road_id]['road_nd_list'].remove(right_nd)

        cur_id_count = len(road_id_road_nd_cross_id_dict)
        self.txt_cross_id_dict = {k: cur_id_count + idx + 1 for idx, k in enumerate(cross_id_way_idx_dict)}
        # txt_cross_id_dict k:sour_true_way_nd_list中的索引值   v:txt输出的cross_id
        # cross_id_way_idx_dict k: cross_id  v ： sour_true_way_nd_list中的索引值 ， 用来后期的分段
        # {'6868728845': 12, '5382831200': 13, '1885342912': 14, '1885342916': 15, '5382831201': 16}

        # 6. 整理生成 cross 相关数据
        new_cross_id_road_data_dict = {}
        for road_id, road_data in road_id_road_nd_cross_id_dict.items():
            if road_data['left_cross_id']:
                l_cross_id = road_data['left_cross_id']
                if l_cross_id not in new_cross_id_road_data_dict:
                    new_cross_id_road_data_dict[l_cross_id] = [
                        {'road_id': road_id, 'road_nd_list': road_data['road_nd_list']}]
                else:
                    new_cross_id_road_data_dict[l_cross_id].append(
                        {'road_id': road_id, 'road_nd_list': road_data['road_nd_list']})

            if road_data['right_cross_id']:
                l_cross_id = road_data['right_cross_id']
                if l_cross_id not in new_cross_id_road_data_dict:
                    new_cross_id_road_data_dict[l_cross_id] = [
                        {'road_id': road_id, 'road_nd_list': road_data['road_nd_list']}]
                else:
                    new_cross_id_road_data_dict[l_cross_id].append(
                        {'road_id': road_id, 'road_nd_list': road_data['road_nd_list']})

        # ## 计算 东西南北 -- txt2
        # for cross_id,one_cross_data in new_cross_id_road_data_dict.items():
        #     direction_result = self.judge_cross_sub_nd_direction(cross_id,one_cross_data)
        #     print('cross_id:{}'.format(cross_id),direction_result)
        # print(1)

        # 计算出所有经纬度转成x,y之后x,y各自的最小值 用于坐标偏移
        x_list = []  # 存储x坐标
        y_list = []  # 存储y坐标
        for road_id, one_road_data_dict in road_id_road_nd_cross_id_dict.items():
            for one_nd in one_road_data_dict['road_nd_list']:
                (x, y) = self.get_node_pos(one_nd)
                x_list.append(x)
                y_list.append(y)
        for cross_id, one_cross_data in new_cross_id_road_data_dict.items():
            (x, y) = self.get_node_pos(cross_id)
            x_list.append(x)
            y_list.append(y)
        x_list = sorted(x_list)
        y_list = sorted(y_list)
        x_min = x_list[0] - 100
        y_min = y_list[0] - 100

        # 取出所有Cross的坐标 用于下面处理每条道路中与Cross坐标点重合的点 x||y-=40   2022/4/25
        cross_pos_list = []
        cross_id_pos_dict = {}  # k:cross_id v:cross_pos
        # k:cross_id v:tj_point
        cross_id_tj_point_dict = {}
        for cross_id, one_cross_data in new_cross_id_road_data_dict.items():
            (x, y) = self.get_node_pos(cross_id)
            # cross_pos_list.append((x, y))
            (sour_cross_x, sour_cross_y) = self.get_node_pos(cross_id)
            cross_x = sour_cross_x - x_min
            cross_y = sour_cross_y - y_min
            cross_pos_list.append((cross_x, cross_y))
            # 存储每个交叉口的交叉点,以cross_id为索引
            cross_id_pos_dict[self.get_txt_cross_id(cross_id)] = [cross_x, cross_y]


        # 整理所有road 数据
        # 大循环是以每条路(road)为循环，小循环是以路中节点(nd)为循环
        # k:road_id v:tj_points
        road_id_tj_points = {}
        for road_id, one_road_data_dict in road_id_road_nd_cross_id_dict.items():
            # R,2,14,12,5
            one_txt = '''R,{},{},{},{},'''.format(
                road_id,  # 第二位,road_id
                self.get_txt_cross_id(one_road_data_dict['left_cross_id']),  # 第三位,左交叉口的id
                self.get_txt_cross_id(one_road_data_dict['right_cross_id']),  # 第四位,右交叉口的id
                len(one_road_data_dict['road_nd_list']),  # 此条路中节点的数量
            )
            # 所有点的x,y坐标集合
            xy_points_list = []
            # 再分别存储的目的是满足拟合曲线的需要
            x_list = []  # 所有点的x坐标集合
            y_list = []  # 所有点的y坐标集合
            # id_left,id_right分别存储左右cross_id
            id_left = self.get_txt_cross_id(one_road_data_dict['left_cross_id'])
            id_right = self.get_txt_cross_id(one_road_data_dict['right_cross_id'])

            for one_nd in one_road_data_dict['road_nd_list']:
                (sour_road_x, sour_road_y) = self.get_node_pos(one_nd)
                # x_min和y_min是因为经纬度转成xy坐标后数值较大,用于缩小数值表示,过大不能在绘图工具中展示
                road_x = sour_road_x - x_min
                road_y = sour_road_y - y_min
                """
                2023-6-16
                ly
                注释以下部分，改变road_x,road_y的值会影响后续退距点的计算和寻找
                """
                # for one_cross_pos in cross_pos_list:
                #     if (road_x, road_y) == one_cross_pos:
                #          road_x -= 40
                #          road_y -= 40

                # 保存每一个点的坐标
                x_list.append(road_x)
                y_list.append(road_y)
                xy_list = [road_x, road_y]
                # xy_list.append(road_x,road_y)
                xy_points_list.append(xy_list)

            # 对每条路进行曲线拟合
            xs_list = self.Find_xs(x_list, y_list)

            # 求退距点的坐标
            tj_x1 = 0
            tj_y1 = 0
            tj_x2 = 0
            tj_y2 = 0

            # 此条路有左交叉口
            if id_left != ' ':
                cross_point1 = cross_id_pos_dict[id_left]  # 存储左交叉点
                # 定下初始点，用于计算退距点,Find_point会解出离初始点最近的那个点
                if xy_points_list[0] != cross_point1:
                    jx_point1 = xy_points_list[0]
                elif xy_points_list[0] == cross_point1:
                    if len(xy_points_list) >= 2:
                        jx_point1 = xy_points_list[1]
                    else:
                        jx_point1 = xy_points_list[0]
                # 退距点
                # Gekko法
                tj_point1 = self.Gekko(cross_point1, xs_list, jx_point1)  # 左退距点的坐标
                tj_x1 = int(tj_point1[0][0] + 0.5)  # 左退距点的x坐标
                tj_y1 = int(tj_point1[1][0] + 0.5)  # 左退距点的y坐标
                point_lon_lat = self.x_y_to_lon_lat(tj_x1, tj_y1)
                road_id_tj_points.setdefault(road_id, []).append(point_lon_lat)
                cross_id_tj_point_dict.setdefault(id_left, []).append(point_lon_lat)
                # solve法
                # tj_point1 = self.Slove(cross_point1, xs_list)  # 左退距点的坐标
                # tj_x1 = int(tj_point1[0]+0.5)  # 左退距点的x坐标
                # tj_y1 = int(tj_point1[1]+0.5)  # 左退距点的y坐标
                # 牛顿法
                #tj_point1 = self.Newton(jx_point1[0], xs_list, cross_point1)
                #x = int(tj_point1 + 0.5)
                #x = tj_point1
                #tj_x1 = x
                #tj_y1 = xs_list[0] * x * x * x * x * x * x + xs_list[1] * x * x * x * x * x + xs_list[2] * x * x * x * x\
                        #+ xs_list[3] * x * x * x + xs_list[4] * x * x + xs_list[5] * x + xs_list[6]
                #tj_y1 = int(tj_y1 + 0.5)
                #point_lon_lat = self.x_y_to_lon_lat(tj_x1, tj_y1)
                #road_id_tj_points.setdefault(road_id, []).append(point_lon_lat)
                #cross_id_tj_point_dict.setdefault(id_left, []).append(point_lon_lat)
                # 弦割法
                # tj_point1 = self.string_cut(xs_list, cross_point1, jx_point1)
                # tj_x1 = int(tj_point1[0] + 0.5)
                # tj_y1 = int(tj_point1[1] + 0.5)
                # # tj_point1 = self.new_way(xs_list, cross_point1, jx_point1)
                # # x = int(tj_point1 + 0.5)
                # # tj_x1 = x
                # # tj_y1 = xs_list[0] * x * x * x * x * x * x + xs_list[1] * x * x * x * x * x + xs_list[2] * x * x * x * x\
                # #         + xs_list[3] * x * x * x + xs_list[4] * x * x + xs_list[5] * x + xs_list[6]
                # # tj_y1 = int(tj_y1 + 0.5)
                # point_lon_lat = self.x_y_to_lon_lat(tj_x1, tj_y1)
                # road_id_tj_points.setdefault(road_id, []).append(point_lon_lat)
                # cross_id_tj_point_dict.setdefault(id_left, []).append(point_lon_lat)
                print("左交叉点为:", cross_point1, "\n生成的左退距点:", [tj_x1, tj_y1], "\n")

            # 此条路有右交叉口
            if id_right != ' ':
                cross_point2 = cross_id_pos_dict[id_right]
                if xy_points_list[0] != cross_point2:
                    jx_point2 = xy_points_list[0]
                elif xy_points_list[0] == cross_point2:
                    if len(xy_points_list) >= 2:
                        jx_point2 = xy_points_list[1]
                    else:
                        jx_point2 = xy_points_list[0]
                # Gekko法
                tj_point2 = self.Gekko(cross_point2, xs_list, jx_point2)  # 左退距点的坐标
                tj_x2 = int(tj_point2[0][0] + 0.5)  # 左退距点的x坐标
                tj_y2 = int(tj_point2[1][0] + 0.5)  # 左退距点的y坐标
                point_lon_lat = self.x_y_to_lon_lat(tj_x2, tj_y2)
                road_id_tj_points.setdefault(road_id, []).append(point_lon_lat)
                cross_id_tj_point_dict.setdefault(id_right, []).append(point_lon_lat)
                # solve法
                # tj_point2 = self.Slove(cross_point2, xs_list)  # 左退距点的坐标
                # tj_x2 = int(tj_point2[0]+0.5)  # 左退距点的x坐标
                # tj_y2 = int(tj_point2[1]+0.5)  # 左退距点的y坐标
                # 牛顿迭代法
                # tj_point2 = self.Newton(jx_point2[0], xs_list, cross_point2)
                # x = int(tj_point2 + 0.5)
                # #x = tj_point2
                # tj_x2 = x
                # tj_y2 = xs_list[0] * x * x * x * x * x * x + xs_list[1] * x * x * x * x * x + xs_list[2] * x * x * x * x\
                #         + xs_list[3] * x * x * x + xs_list[4] * x * x + xs_list[5] * x + xs_list[6]
                # tj_y2 = int(tj_y2 + 0.5)
                # point_lon_lat = self.x_y_to_lon_lat(tj_x2, tj_y2)
                # road_id_tj_points.setdefault(road_id, []).append(point_lon_lat)
                # cross_id_tj_point_dict.setdefault(id_right, []).append(point_lon_lat)
                # 弦割法
                # tj_point2 = self.string_cut(xs_list, cross_point2, jx_point2)
                # tj_x2 = int(tj_point2[0] + 0.5)
                # tj_y2 = int(tj_point2[1] + 0.5)
                # point_lon_lat = self.x_y_to_lon_lat(tj_x2, tj_y2)
                # road_id_tj_points.setdefault(road_id, []).append(point_lon_lat)
                # cross_id_tj_point_dict.setdefault(id_right, []).append(point_lon_lat)
                # tj_point2 = self.new_way(xs_list, cross_point2, jx_point2)
                # x = int(tj_point2 + 0.5)
                # tj_x2 = x
                # tj_y2 = xs_list[0] * x * x * x * x * x * x + xs_list[1] * x * x * x * x * x + xs_list[2] * x * x * x * x \
                #         + xs_list[3] * x * x * x + xs_list[4] * x * x + xs_list[5] * x + xs_list[6]
                # tj_y2 = int(tj_y2 + 0.5)
                print("右交叉点为:", cross_point2, "\n生成的右退距点:", [tj_x2, tj_y2], "\n")

            # 再做一次小循环，目的是format退距点和普通点(非交叉点不需要替换)
            for one_nd1 in one_road_data_dict['road_nd_list']:
                cross_point_left = []  # 对左交叉点初始化
                cross_point_right = []
                (sour_road_x, sour_road_y) = self.get_node_pos(one_nd1)
                road_x = sour_road_x - x_min
                road_y = sour_road_y - y_min

                if id_left != ' ':
                    cross_point_left = cross_id_pos_dict[id_left]
                if id_right != ' ':
                    cross_point_right = cross_id_pos_dict[id_right]
                # 若此节点为左交叉口点/右交叉口点则用相应退距点替换，普通点直接format
                if [road_x, road_y] == cross_point_left or [road_x, road_y] == cross_point_right:
                    if [road_x, road_y] == cross_point_left:
                        one_nd_pos_txt = '{},{},0,'.format(tj_x1, tj_y1)
                        one_txt += one_nd_pos_txt
                    else:
                        one_nd_pos_txt = '{},{},0,'.format(tj_x2, tj_y2)
                        one_txt += one_nd_pos_txt
                else:
                    one_nd_pos_txt = '{},{},0,'.format(road_x, road_y)
                    one_txt += one_nd_pos_txt
            print("交叉口与退距点对应:", cross_id_tj_point_dict)

            # 最后四个位置
            one_txt += '1,1,10,2'  # 两个方向的车道数;10为固定值，车道宽度;2为保留位，暂无意义
            result_txt += one_txt + '\n'

        # 整理所有 cross 数据
        # 计算 东西南北 -- txt2

        # 将5路口的‘3’改成‘0’，2和5路口都不能用
        cross_type_dict = {4: '0', 2: '1', 3: '1', 5: '0'}
        for cross_id, one_cross_data in new_cross_id_road_data_dict.items():
            direction_result = self.judge_cross_sub_nd_direction(cross_id, one_cross_data)
            # print('cross_id:{}'.format(cross_id),direction_result)
            # 经度从小到大
            lon_small_to_big = []
            # 纬度从小到大
            lat_small_to_big = []
            print("本次是交叉口", cross_id, "的循环")
            list1 = list(cross_id_tj_point_dict[self.get_txt_cross_id(cross_id)])
            lon_small_to_big = sorted(list1)
            # 经度从小到大排好
            for i in range(len(list1)):
                temp = list1[i][0]
                list1[i][0] = list1[i][1]
                list1[i][1] = temp
            lat_small_to_big = sorted(list1)
            for i in range(len(list1)):
                temp = lat_small_to_big[i][0]
                lat_small_to_big[i][0] = lat_small_to_big[i][1]
                lat_small_to_big[i][1] = temp
            print("经度:", lon_small_to_big, "\n")
            print("纬度:", lat_small_to_big, "\n")
            conect_1 = []
            conect_2 = []
            # 第一组两条要连的路
            conect_1.append(lon_small_to_big[-1])
            conect_1.append(lon_small_to_big[0])
            lon_small_to_big.remove(conect_1[0])
            lon_small_to_big.remove(conect_1[1])
            # 分情况讨论(十字路口还是T路口)
            # T路口
            print("删除第一组之后的经度列表：", lon_small_to_big)

            if len(lon_small_to_big) == 1:
                conect_2.append(lon_small_to_big[0])
            if len(lon_small_to_big) == 2:
                conect_2.append(lon_small_to_big[0])
                conect_2.append(lon_small_to_big[1])

            print("conect_1中的数据:", conect_1)
            print("conect_2中的数据:", conect_2)
            print("路与退距点之间的对应:", road_id_tj_points)
            new_conect_1 = []
            new_conect_1.append([k for k, v in road_id_tj_points.items() if conect_1[0] == v[0]][0])
            new_conect_1.append([k for k, v in road_id_tj_points.items() if conect_1[1] == v[0]][0])
            new_conect_2 = []
            value = 0
            if len(conect_2) == 2:
                for k, v in road_id_tj_points.items():
                    if len(v) == 1:
                        if conect_2[0] == v[0]:
                            new_conect_2.append(k)
                    if len(v) == 2:
                        if conect_2[0] == v[0] or conect_2[0] == v[1]:
                            new_conect_2.append(k)
                for k, v in road_id_tj_points.items():
                        if len(v) == 1:
                            if conect_2[1] == v[0]:
                                    new_conect_2.append(k)
                        if len(v) == 2:
                            if conect_2[1] == v[0] or conect_2[1] == v[1]:
                                new_conect_2.append(k)
                value = new_conect_2[1]
            elif len(conect_2) == 1:
                for k, v in road_id_tj_points.items():
                    if len(v) == 1:
                        if conect_2[0] == v[0]:
                            new_conect_2.append(k)
                    if len(v) == 2:
                        if conect_2[0] == v[0] or conect_2[0] == v[1]:
                            new_conect_2.append(k)
                value = 0
            one_cross_data_len = len(one_cross_data)
            one_txt = '''C,{},{},{},'''.format(
                self.get_txt_cross_id(cross_id),
                cross_type_dict[one_cross_data_len] if one_cross_data_len in cross_type_dict else '0',  # 'not define'
                '5'  # one_cross_data_len
            )
            # for v in direction_result.values():
            #     one_txt += f'{v},'
            # 第五、六位为一组链接道路，第七、八为一组链接道路
            print("新conect_1中的数据:", new_conect_1)
            print("新conect_2中的数据:", new_conect_2)

            one_txt += '''{},{},'''.format(
                new_conect_1[0],
                new_conect_1[1]
            )
            one_txt += '''{},{},'''.format(
                new_conect_2[0],
                value
            )

            # 是否五路口
            one_txt += '1,' if one_cross_data_len == 5 else f'0,'

            # 交叉点总数
            one_txt += '1,'
            (sour_cross_x, sour_cross_y) = self.get_node_pos(cross_id)
            cross_x = sour_cross_x - x_min
            cross_y = sour_cross_y - y_min
            one_txt += '{},{},0,'.format(cross_x, cross_y)
            one_txt += '16'
            # print(one_txt)
            result_txt += one_txt + '\n'

        print("路与对应退距点字典:", road_id_tj_points)

        # save_file = os.path.splitext(self.file)[0] + '_output.txt'
        with open(self.save_file, 'a', encoding="utf-8") as f:
            f.write(result_txt)

        print('输出文件已保存为:[{}]'.format(self.save_file))
        # return result_txt

    # 经纬度转x y坐标
    def lon_lat_to_x_y(self, lon, lat):
        # 将经纬度，以及海拔高度从度数转换成弧度
        lon = lon * math.pi / 180
        lat = lat * math.pi / 180
        B0 = 30 * math.pi / 180
        # 长半轴与短半轴
        a = 6378137
        b = 6356752.3142
        # 第一偏心率以及第二偏心率
        e = math.sqrt(1 - (b / a) * (b / a))
        e2 = math.sqrt((a / b) * (a / b) - 1)
        CosB0 = math.cos(B0)
        # 卯酉圈半径
        N = (a * a / b) / math.sqrt(1 + e2 * e2 * CosB0 * CosB0)

        K = N * CosB0
        Pi = math.pi
        SinB = math.sin(lat)
        tan = math.tan(Pi / 4 + lat / 2)
        E2 = math.pow((1 - e * SinB) / (1 + e * SinB), e / 2)
        xx = tan * E2
        xc = K * math.log(xx)
        yc = K * lon
        # xc = round(xc, 2)
        # yc = round(yc, 2)
        xc = int(xc)
        yc = int(yc)
        return (xc, yc)

    def get_node_pos(self, node_id):
        '''获取 nodeid 的 经纬度'''
        lat = float(self.node_id_dict[node_id].getAttribute('lat'))
        lon = float(self.node_id_dict[node_id].getAttribute('lon'))
        (lon, lat) = self.lon_lat_to_x_y(lon, lat)
        return (lon, lat)

    def get_distance(self, node_id1, node_id2):
        # 计算两点之间的距离
        lat1 = float(self.node_id_dict[node_id1].getAttribute('lat'))
        lon1 = float(self.node_id_dict[node_id1].getAttribute('lon'))
        (lon1, lat1) = self.lon_lat_to_x_y(lon1, lat1)
        lat2 = float(self.node_id_dict[node_id2].getAttribute('lat'))
        lon2 = float(self.node_id_dict[node_id2].getAttribute('lon'))
        (lon2, lat2) = self.lon_lat_to_x_y(lon2, lat2)
        distance = math.sqrt((lon1 - lon2) ** 2 + (lat1 - lat2) ** 2)
        return distance
    def x_y_to_lon_lat(self, x, y):
            lonlat_coordinate = []
            L = 6381372 * math.pi * 2
            W = L
            H = L / 2
            mill = 2.3
            lat = ((H / 2 - y) * 2 * mill) / (1.25 * H)
            lat = ((math.atan(math.exp(lat)) - 0.25 * math.pi) * 180) / (0.4 * math.pi)
            lon = (x - W / 2) * 360 / W
            # 最终需要确认经纬度保留小数点后几位
            return [round(lon, 15), round(lat, 15)]
    def judge_two_node_left_right(self, start_nd, end_nd):
        ## 经度小 是左边，经度大是右边
        ## start_nd 在左边 返回True
        ## start_nd 在右边 返回False
        start_pos = self.get_node_pos(start_nd)
        end_pos = self.get_node_pos(end_nd)
        # print(start_pos,end_pos)
        # start_pos与end_pos的值是x，y值

        if float(start_pos[0]) < float(end_pos[0]):
            return True
        return False

    def judge_cross_sub_nd_direction(self, cross_id, cross_data):
        '''[
            {'road_id': 1, 'road_nd_list': ['6704947951', '1885342911', '7037616086', '6228930804', '1885342912']}, 
            {'road_id': 2, 'road_nd_list': ['1885342912', '6228930794', '6228930795', '6228930797', '6868728845']}, 
            {'road_id': 11, 'road_nd_list': ['1885342912', '6341069013']}
        ]
        '''
        nd_count = len(cross_data)
        road_id_direction_nd_dict = {}
        for one_road_data in cross_data:  ## 得到交叉口连接的前一个点的 nd id
            if one_road_data['road_nd_list'][0] == cross_id:
                road_id_direction_nd_dict[one_road_data['road_id']] = one_road_data['road_nd_list'][1]
            else:
                road_id_direction_nd_dict[one_road_data['road_id']] = one_road_data['road_nd_list'][-2]
            # road_id_direction_nd_dict[one_road_data['road_id']] = self.get_node_pos(road_id_direction_nd_dict[one_road_data['road_id']])
        # print(road_id_direction_nd_dict)
        road_id_pos_dict = {}
        for road_id, one_nd in road_id_direction_nd_dict.items():
            road_id_pos_dict[road_id] = [float(i) for i in self.get_node_pos(one_nd)]
        # print(road_id_pos_dict)
        result = {k: '' for k in ['west', 'east', 'north', 'south']}
        if len(road_id_pos_dict) > 0:
            road_id = sorted(road_id_pos_dict.items(), key=lambda x: x[1][0])[0][0]  # 找出经度最小的road_id
            result['west'] = road_id
            road_id_pos_dict.pop(road_id)

        if len(road_id_pos_dict) > 0:
            road_id = sorted(road_id_pos_dict.items(), key=lambda x: x[1][0], reverse=True)[0][0]  # 找出经度最大的road_id
            result['east'] = road_id
            road_id_pos_dict.pop(road_id)

        if len(road_id_pos_dict) > 0:
            road_id = sorted(road_id_pos_dict.items(), key=lambda x: x[1][1], reverse=True)[0][0]  # 找出纬度最大的road_id
            result['north'] = road_id
            road_id_pos_dict.pop(road_id)

        # 2023/6/15 判断T路口出错问题，添加road_id_pos_dict是否为空判断
        if len(road_id_pos_dict) > 0:
            road_id = sorted(road_id_pos_dict.items(), key=lambda x: x[1][1])[0][0]  # 找出纬度最小的road_id
            result['south'] = road_id
            road_id_pos_dict.pop(road_id)
        else:
            result['south'] = 0
            # print(result)
        return result

    def get_txt_cross_id(self, cross_id):
        if not cross_id:
            return ' '
        return self.txt_cross_id_dict[cross_id]

    def hb_to_two_way(self, jc_point, one_nd_list1, one_nd_list, way_id_way_nd_dict, way_id_1, way_id_2):
        # 1.jc_point在way1的终点，在way2的起点
        if jc_point[0] == one_nd_list1[-1] and jc_point[0] == one_nd_list[0]:
            for idx5 in range(0, len(one_nd_list)):
                if one_nd_list[idx5] != jc_point[0]:
                    one_nd_list1.append(one_nd_list[idx5])
            way_id_way_nd_dict[way_id_1[0]] = one_nd_list1
            way_id_way_nd_dict.pop(way_id_2[0])
        # 2.jc_point在way1的终点，在way2的终点
        if jc_point[0] == one_nd_list1[-1] and jc_point[0] == one_nd_list[-1]:
            one_nd_list.reverse()
            for idx5 in range(0, len(one_nd_list)):
                if one_nd_list[idx5] != jc_point[0]:
                    one_nd_list1.append(one_nd_list[idx5])
            way_id_way_nd_dict[way_id_1[0]] = one_nd_list1
            way_id_way_nd_dict.pop(way_id_2[0])
        # 3.jc_point在way1的起点，在way2的起点
        if jc_point[0] == one_nd_list1[0] and jc_point[0] == one_nd_list[0]:
            one_nd_list1.reverse()
            for idx5 in range(0, len(one_nd_list)):
                if one_nd_list[idx5] != jc_point[0]:
                    one_nd_list1.append(one_nd_list[idx5])
            way_id_way_nd_dict[way_id_1[0]] = one_nd_list1
            way_id_way_nd_dict.pop(way_id_2[0])
        # 4.jc_point在way1的起点，在way2的终点
        if jc_point[0] == one_nd_list1[0] and jc_point[0] == one_nd_list[-1]:
            one_nd_list1.reverse()
            one_nd_list.reverse()
            for idx5 in range(0, len(one_nd_list)):
                if one_nd_list[idx5] != jc_point[0]:
                    one_nd_list1.append(one_nd_list[idx5])
            way_id_way_nd_dict[way_id_1[0]] = one_nd_list1
            way_id_way_nd_dict.pop(way_id_2[0])

    def Find_xs(self, x_list, y_list):
        # 曲线拟合
        res = np.polyfit(x_list, y_list, 6)
        # p1 = np.poly1d(res)
        # y_list_2 = p1(x_list)
        # plt.plot(x_list, y_list_2, 'blue', label="6阶拟合")
        # plt.scatter(x_list, y_list, color='black', label="原始数据")
        # plt.title("拟合曲线图")
        # plt.rcParams['font.sans-serif'] = ['SimHei']  # 用来正常显示中文标签
        # plt.rcParams['axes.unicode_minus'] = False  # 用来正常显示负号
        # plt.legend(loc=2)
        # plt.show()
        return res

    def Gekko(self, jc_point, xs_list, jx_point):
        # 根据路上的基点找到退距退的那个点
        # GEKKO解法,只能得到一种解，会得到距离初始值最近的那个值
        x0 = jc_point[0]  # 交叉点的x坐标
        y0 = jc_point[1]  # 交叉点的y坐标
        m = GEKKO(remote=False)
        x = m.Var(value=jx_point[0])  # 初始点
        y = m.Var(value=jx_point[1])
        # 第一个方程为拟合出的曲线，退距点应满足此曲线方程
        # 第二个方程为退距点与交叉点距离应为10
        m.Equations([xs_list[0] * x * x * x * x * x * x + xs_list[1] * x * x * x * x * x + xs_list[2] * x * x * x * x +
                     xs_list[3] * x * x * x + xs_list[4] * x * x + xs_list[5] * x + xs_list[6] == y,
                     (x - x0) ** 2 + (y - y0) ** 2 == 100])
        m.solve(disp=False)  # 不显示计算过程
        x1 = x.value
        y1 = y.value
        res_point = [x1, y1]  # 退距点
        return res_point

    def Slove(self, jc_point, xs_list):
        # slove法
        # 虽然快但是计算精度太差了，会导致退距失败
        x0 = jc_point[0]  # 交叉点的x坐标
        y0 = jc_point[1]  # 交叉点的y坐标
        x, y = symbols('x, y', real=True)
        eqs = [xs_list[0] * x * x * x + xs_list[1] * x * x + xs_list[2] * x + xs_list[3] - y,
               (x - x0) ** 2 + (y - y0) ** 2 - 100]
        res = nonlinsolve(eqs, [x, y])
        return list(res.intersection(S.Complexes))

    def fun1(self, x, xs_list, jc_point):
        # 原函数
        x0 = jc_point[0]
        y0 = jc_point[1]
        return (x - x0) ** 2 + (
                xs_list[0] * x * x * x * x * x * x + xs_list[1] * x * x * x * x * x + xs_list[2] * x * x * x * x +
                xs_list[3] * x * x * x + xs_list[4] * x * x + xs_list[5] * x + xs_list[6] - y0) ** 2 - 100

    def fun2(self, x, xs_list, jc_point):
        # 一阶导函数
        x0 = jc_point[0]
        y0 = jc_point[1]
        return 2 * (x - x0) + 2 * (
                xs_list[0] * x * x * x * x * x * x + xs_list[1] * x * x * x * x * x + xs_list[2] * x * x * x * x +
                xs_list[3] * x * x * x + xs_list[4] * x * x + xs_list[5] * x + xs_list[6] - y0) * (
                6 * xs_list[0] * x * x * x * x * x + 5 * xs_list[1] * x * x * x * x + 4 * xs_list[2] * x * x * x + 3 *
                xs_list[3] * x * x + 2 * xs_list[4] * x + xs_list[5])

    def fun3(self, x, xs_list, jc_point):
        # 二阶导函数
        x0 = jc_point[0]
        y0 = jc_point[1]
        return 2 + 2 * ((6 * xs_list[0] * x * x * x * x * x + 5 * xs_list[1] * x * x * x * x + 4 * xs_list[2] * x * x *
                         x + 3 * xs_list[3] * x * x + 2 * xs_list[4] * x + xs_list[5]) ** 2 + (xs_list[0] * x * x * x *
                        x * x * x + xs_list[1] * x * x * x * x * x + xs_list[2] * x * x * x * x + xs_list[3] * x * x * x
                        + xs_list[4] * x * x + xs_list[5] * x + xs_list[6] - y0) * (30 * xs_list[0] * x * x * x * x + 20
                        * xs_list[1] * x * x * x + 12 * xs_list[2] * x * x + 6 * xs_list[3] * x + 2 * xs_list[4]))

    def Newton(self, x0, xs_list, jc_point):
        # 牛顿迭代法(能用)
        start = time.perf_counter()
        p0 = x0 * 1.0
        max_iter = 10000
        tol = 1e-7
        p = 0
        for i in range(0, max_iter):
            p = p0 - self.fun1(p0, xs_list, jc_point)/self.fun2(p0, xs_list, jc_point)
            if abs(p - p0) < tol:
                # print("误差为:", abs(p - p0))
                # end = time.perf_counter()
                # runTime = end - start
                # runTime_ms = runTime * 1000
                # print("运行时间：", runTime_ms, "毫秒")
                return p
            p0 = p
    def Norm2(self, p):
        sum_of_p = sum([i ** 2 for i in p])
        return sum_of_p ** 0.5

    def string_cut(self, xs_list, jc_point, jx_point):
        # 弦割法
        start = time.perf_counter()
        x0 = jc_point[0]
        y0 = jc_point[1]
        x, y = sympy.symbols("x y")
        args = sympy.Matrix([x, y])
        A = [xs_list[0] * x * x * x * x * x * x + xs_list[1] * x * x * x * x * x + xs_list[2] * x * x * x * x +
                xs_list[3] * x * x * x + xs_list[4] * x * x + xs_list[5] * x + xs_list[6] - y,
             (x - x0) ** 2 + (y - y0) ** 2 - 100
        ]
        x_0 = [jx_point[0], jx_point[1]]
        prec = 0.0000001
        n = 10000
        funcs = sympy.Matrix(A)
        h = 0.1
        e = np.eye(len(A)) * h
        # pprint.pprint(e)
        x_0 = sympy.Matrix(x_0)
        x = args

        x_pre = x_0
        c = []
        for k in range(0, n):
            for p in range(len(A)):
                c.append([A[p].subs(zip(x, (x_pre + sympy.Matrix(e[j]))))
                          for j in range(0, len(A))])
            fij = sympy.Matrix(c)  # 计算fij【k】
            # pprint.pprint(fij)
            b = funcs.subs(zip(x, x_pre))  # 计算fi【k】
            z = fij.inv() * b  # 求解z
            deltx = h * z / (sum(z) - 1)  # 计算deltx
            x_new = x_pre + deltx  # 更新x
            if self.Norm2(funcs.subs(zip(x, x_new))) < prec:  # 满足精度要求则返回
                print("误差为:", self.Norm2(funcs.subs(zip(x, x_new))))
                end = time.perf_counter()
                runTime = end - start
                runTime_ms = runTime * 1000
                print("运行时间：", runTime_ms, "毫秒")
                return x_new
            x_pre = x_new
            c = []
    def new_way(self, xs_list, jc_point, jx_point):
        k = 0
        max_iter = 1000
        x = [0]* max_iter
        x[1] = jx_point[0]
        x[0] = jc_point[0]
        prec = 0.000001

        # 黄金分割点
        for i in range(0, max_iter):
            xp = x[k] + (math.sqrt(5) - 1) * (x[k + 1] - x[k]) / 2
            f_k = self.fun1(x[k], xs_list, jc_point)
            f_k_jia_1 = self.fun1(x[k+1], xs_list, jc_point)
            f_k_p = self.fun1(xp, xs_list, jc_point)
            while k >= 1:
                x[k+1] = x[k]-(x[k]-x[k-1])*f_k/((math.sqrt(5) - 1)/2*f_k_jia_1-(2+math.sqrt(5)) * f_k_p+(5+math.sqrt(5))/2*f_k)
                while abs(x[k+1]-x[k]) < prec:
                    return x[k+1]
            k += 1

# 只导出osm_to_txt函数
__all__ = ['osm_to_txt']

# 定义一个静音的上下文管理器来禁止输出
def osmtrans_suppress_output():
    # 创建一个空的 StringIO 对象，用作 stdout 的重定向目标
    return contextlib.redirect_stdout(io.StringIO())

def _inter_osm_to_txt(osm_file_path: str, txt_file_path: str) -> bool:
    """将osm格式转为绘图txt格式

    Args:
        osm_file_path (str): osm文件路径
        txt_file_path (str): 输出的txt格式文件路径

    Returns:
        bool: 是否转换成功
    """
    result = False
    try:
        class_deal_xml = deal_xml(osm_file_path, txt_file_path)
        class_deal_xml.parse_xml()
        result = True
    except Exception as e:
        result = False
    return result

def osm_to_txt(osm_file_path: str, txt_file_path: str) -> bool:
    result = None
    with osmtrans_suppress_output():
        result = _inter_osm_to_txt(osm_file_path, txt_file_path)
    return result

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("使用方法: python3 osmtrans.py ***.osm ***.txt")
        sys.exit(1)
    # sour_file = 'map.osm'
    sour_osm, out_txt = sys.argv[1:3]
    a = deal_xml(sour_osm, out_txt)
    try:
        a.parse_xml()
    except Exception as e:
        pass
        print(traceback.format_exc())
        raise
    input('执行结束!')

