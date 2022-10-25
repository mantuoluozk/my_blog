# my_blog
码神之路博客代码
使用spring boot, mysql, redis, mybatis-plus的前后端分离的博客网站
# 项目使用技术 ：
**springboot + mybatisplus+redis+mysql+rocketmq**

1.jwt + redis
token令牌的登录方式，访问认证速度快，session共享，安全性
redis做了 令牌和 用户信息的对应管理，

进一步增加了安全性
登录用户做了缓存
灵活控制用户的过期（续期，踢掉线等）

2.threadLocal
使用了保存用户信息，请求的线程之内，可以随时获取登录的用户，做了线程隔离
在使用完ThreadLocal之后，做了value的删除，防止了内存泄漏

3.线程安全
update table set value = newValue where id=1 and value=oldValue
改为：文章浏览量和评论量改为使用redis自增+定时任务

4.线程池
对当前的主业务流程 无影响的操作，放入线程池执行，
对redis缓存的修改
记录日志

5.登录拦截器

6.统一日志记录，统一缓存处理，统一异常处理
统一日志记录、统一缓存处理都通过AOP实现

7.rocketmq
当文章修改的时候，发送给消息队列控制缓存
