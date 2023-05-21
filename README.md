#### 黑马点评

#### 项目架构


#### 项目启动
1. 在resource目录下有sql文件，导入sql
2. 配置文件中的数据库密码和redis地址改成自己的
3. 前端资源采用nginx的反向代理，进入nginx文件夹输入start nginx.exe启动
4. 启动springboot

#### 项目特色
1. 整个项目使用redis来进行数据的优化，提高了项目的访问速度
2. 在存储redis时使用了一个RedisConstants来保存RedisKey的常量，提高了复用率
3. 采用逻辑过期和互斥锁两种方式解决了缓存击穿的问题