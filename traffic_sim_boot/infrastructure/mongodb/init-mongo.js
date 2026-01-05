// MongoDB 初始化脚本
// 创建数据库和用户

db = db.getSiblingDB('traffic_sim');

// 创建用户（如果需要）
db.createUser({
  user: 'traffic_sim',
  pwd: 'traffic_sim',
  roles: [
    {
      role: 'readWrite',
      db: 'traffic_sim'
    }
  ]
});

// 创建集合（可选，MongoDB会自动创建）
db.createCollection('maps');
db.createCollection('simulation_data');
db.createCollection('replay_data');

print('MongoDB initialized successfully');

