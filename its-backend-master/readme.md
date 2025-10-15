功能说明:  
用户部分：  
登录。		/login		参数：userName，password  
认证授权测试。	/testLogin，/require_auth，/require_role，/require_permission  
查看个人信息。	/select_user  
修改用户。		/update_user	参数：userId，userName，password  
删除用户。		/delete_user	参数：id  
增加用户（注册）。	/register	参数：userId，roleId，userName，password  
登出			/logout  

# 部署步骤

#### 一、文件安装

1、MongoDB安装、Python3安装、JDK1.8  32位和64位的安装、MySQL8.0的安装

2、配置好JDK64位和Python的环境变量

3、MongoDB设置密码、MySQL设置密码（与its-backend中的配置文件一致，默认为root，可自行修改）

4、导入数据库文件（its-backend目录下的its.sql）

#### 二、文件拷贝

1、its-backend-0.0.1-SNAPSHOT、its-common-1.0-SNAPSHOT、its-engine、its-enginmanager-0.0.1-SNAPSHOT  四个jar包的拷贝，拷贝到同一个目录下

2、拷贝kafka和zookeeper

#### 三、项目启动

1、MongoDB启动：mongod --dbpath C:\MongoDB\server\4.0\data

2、zookeeper启动：0_zookeeper.bat脚本的运行

3、kafka启动：1_kafka.bat脚本的运行

4、kackend项目运行：java -jar its-backend-0.0.1-SNAPSHOT  >  ********.txt

5、enginmanager项目运行：java -jar its-enginmanager-0.0.1-SNAPSHOT  >  *********.txt

（****一般以日期命名，保存的是两个项目的日志文件）