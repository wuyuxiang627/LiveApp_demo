# 视频云直播工程 导读

视频云直播工程集成网易视频云 SDK，展示了直播的推流与拉流的应用场景。

## <span id="源码导读"> 源码导读</span>

### 工程结构说明

####源码结构：

- app
    - activity：所有 Activity
	- base: 基类
	- fragment: 包含fragment与部分聊天室UI模块
	- liveStreaming：推流相关方法
	- liveplayer: 拉流相关方法
	- nim：云信im通信相关类（聊天室功能等）
	- server：应用服务器的请求封装类
	- util：工具类
	- widget：界面相关控件
- uikit
	- 云信im通信的UI组件库

### 重点类说明
- LoginActivity: 注册登录页面，包含应用服务器登录与云信im服务器登录等。
- MainActivity：主界面，选择进入主播设置页还是观众设置页。
- EnterLiveActivity：主播设置页，进行直播参数配置，处理创建房间逻辑等。
- EnterAudienceActivity: 观众设置页，进行房间与拉流地址参数配置。
- LiveRoomActivity: 直播页，使用fragment容器来装载 推流、聊天室、房间信息等模块。
- CaptureFragment：主播推流Fragment，处理直播预览UI逻辑。
- AudienceFragment：观众拉流Fragment，处理播放UI逻辑。
- ChatRoomMessageFragment: 聊天室Fragment, 包含聊天列表与输入框。
- CapturePreviewController：推流控制器，包含推流初始化，开始，暂停，重启，重置等操作，监听推流的状态。
- NEVideoController：拉流控制器，包含拉流初始化，推流开始，暂停等操作，监听拉流的状态。
- NimController: 聊天室控制器，包含聊天室初始化，踢人、禁言等逻辑的实现。

