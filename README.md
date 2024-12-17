# JAVA基于TCP以及Swing编程的简单在线聊天室实现

## 使用说明

本项目有使用mysql数据库，本地部署请知悉。

运行步骤如下：

1. 启动serverProcess.java   开启服务器端
2. 启动Main  开启客户端
   + 这里可以开启任意个客户端
3. 软件启动完毕

## 效果演示

bilibili：https://www.bilibili.com/video/BV13UkLYyEGo/?vd_source=6318b6cc24c5f2d477da738b88b6ef9e

## 功能一览

+ 好友间消息交互
+ 好友间文件传输
+ 添加好友
+ 服务器信息检测

## 扩展说明

+ 群聊功能

  可以通过修改Util中messageRenderer，使其能够显示区分不同用户后，在原有私聊基础上加一个向多个用户发送消息方法即可。

+ 删除好友功能~都加了为什么要删呢（）~

  修改本地数据库后，加入修改界面方法

+ 表情传送

  要修改messagelist中泛型message类，同时界面可以通过cardlayout实现消息与表情的区分。

## 远程部署与更新意向

没空啦！☆~
