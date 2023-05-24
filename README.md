# 黑马点评

#### 项目介绍
一个类似于美团优选的软件，可以推送笔记，实现好友的关注，核心在于对redis的使用

#### 项目架构
前端资源采用nginx的反向代理，后端使用Springboot以及RestFUL的架构实现解耦

#### 项目启动
1. 在resource目录下有sql文件，导入sql
2. 在resource中有nginx的文件，进入该文件输入start nginx.exe启动
3. 配置文件中的数据库密码和redis地址改成自己的地址
4. 启动redis服务器
5. 启动springboot

#### 项目特色
1. 整个项目使用redis来进行数据的优化，提高了项目的访问速度
2. 在存储redis时使用了一个RedisConstants来保存RedisKey的常量，提高了复用率
3. 采用逻辑过期和互斥锁两种方式解决了缓存击穿的问题
4. 采用分布式锁解决多个JVM使用不同锁监视器而导致多卖的问题
5. 采用feed流实现了好友共同关注的功能
6. 采用redis的GEO实现搜索附近的商铺
