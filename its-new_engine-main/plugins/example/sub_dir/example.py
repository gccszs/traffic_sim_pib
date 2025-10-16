import sim_plugin

import json

def ProduceVeh(sim_arg):
	sim_plugin.log("[Python] call ProduceVeh " + sim_arg);
	arg_json = json.loads(sim_arg)

	# 解析参数
	origin_link_id = arg_json['link_id']
	origin_id = arg_json['origin_id']
	cell_id = arg_json['cell_id']

	# 返回数据
	ret_data = {
    	'produce' : False,
    	'veh_type' : 'car',
    	'lane_id' : 0,
    	'cell_id' : 0,
    	'route' : '0'
	}

	can_gen_flag = sim_plugin.determine_veh_gen(origin_link_id, origin_id)
	if (can_gen_flag == True):
		random_lane = sim_plugin.get_random_lane_id(origin_link_id, origin_id);
		if (random_lane != -1):
			able_flag = sim_plugin.have_space_in_target_lane(origin_link_id, random_lane)
			if (able_flag == True):
				# 获取最短路径并转为字符串
				spi = sim_plugin.get_shortest_path(origin_link_id, 0, origin_id, True)
				lst_tmp = list(map(lambda x:str(x), spi))
				spi_str = ' '.join(lst_tmp)
	
				# 设置返回数据
				ret_data['produce'] = True
				ret_data['lane_id'] = random_lane
				ret_data['route'] = spi_str


	ret_str = json.dumps(ret_data)
	return ret_str

def CarFollowing(sim_arg):
	sim_plugin.log("[Python] call CarFollowing " + sim_arg);
	arg_json = json.loads(sim_arg)

	# 解析参数
	cur_cell_id = arg_json['cur_cell_id']
	cur_speed = arg_json['cur_speed']
	front_cell_id = arg_json['front_cell_id']
	front_speed = arg_json['front_speed']
	max_speed = arg_json['max_speed']

	# ... 进行一些算法操作 ...

	# 返回数据
	ret_data = {
    	'new_cell_id' : 0,
    	'new_speed' : 0
	}

	ret_data['new_speed'] = cur_speed
	ret_data['new_cell_id'] = int(cur_speed + cur_cell_id)


	ret_str = json.dumps(ret_data)
	return ret_str

def CarChangeLane(sim_arg):
	sim_plugin.log("[Python] call CarChangeLane " + sim_arg);
	arg_json = json.loads(sim_arg)

	# 解析参数
	link_id = arg_json['link_id']
	change_to_lane_id = arg_json['change_to_lane_id']
	cur_cell_id = arg_json['cur_cell_id']
	cur_speed = arg_json['cur_speed']
	cf_cell_id = arg_json['cf_cell_id']
	cf_speed = arg_json['cf_speed']

	# ... 进行一些算法操作 ...

	# 返回数据
	ret_data = {
    	'new_speed' : 0
	}

	ret_data['new_speed'] = cur_speed

	ret_str = json.dumps(ret_data)
	return ret_str