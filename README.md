# DHUCourseSelecter

![StillCoding](https://img.shields.io/badge/Still-Coding-green.svg)
![License](https://img.shields.io/badge/License-GPLv2-brightgreen.svg)
![build passing](https://img.shields.io/travis/rust-lang/rust.svg)
![Author](https://img.shields.io/badge/Author-DCMMC-blue.svg)
![WeChatID](https://img.shields.io/badge/WeChat-Kevin--0220-red.svg)


东华大学教务处选课助手 ver. 0.1 Alpha

> Still coding...

> CLI界面简单的实现了队列选课, GUI只完成了主界面, 设置界面, 关于界面(Partial), 查看选课界面, 修改选课界面正在施工...

# Changelog

> Still coding...

# Features

* 已选课程课表预览
* 自定义选课请求的间隔时间和最大选课次数
* 多线程选课, 并且可以使用Listener
* 从教务处选课首页导入当前学期推荐选择的课程
* 搜索课程
* 封装了大类课程, 课程, 班级的各类信息
* 查看已选课程
* 删除课程

# Screenshot

> Still coding...

# Dependent Libraries

* [HttpClient](https://hc.apache.org/httpclient-3.x/)
* [Jsoup](https://jsoup.org/)
* Still coding...

# Build

本项目用Gradle构建, 要运行GUI主程序, 请在项目根目录执行: 

Windows下:

```
$ gradlew run
```

*nix下:

```
$ ./gradlew.sh run
```

其他`tasks`请执行`gradlew tasks`查看.

# Author

DCMMC

# License

[GPL v2](./LICENSE)
