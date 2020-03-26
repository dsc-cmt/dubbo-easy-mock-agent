## 这是啥
一个用于对dubbo接口进行mock的agent

## 支持版本
动态配置相关类改动比较大,目前仅支持apache dubbo 2.7.5

## 原理
通过javassist对dubbo框架中的MockClusterInvoker植入mock逻辑(可以避免No Provider)，植入的逻辑如下

添加field
```
private static final org.apache.dubbo.common.config.Configuration CONFIGURATION =  org.apache.dubbo.rpc.model.ApplicationModel.getEnvironment().getConfiguration();

private static final boolean IS_MOCK = Boolean.parseBoolean(getConfig("easymock.enable","false"));

```
添加方法
```
private static String getConfig(String key, String defaultValue) {
    String value;
    org.apache.dubbo.common.config.configcenter.DynamicConfiguration dynamicConfiguration = org.apache.dubbo.common.config.configcenter.DynamicConfiguration.getDynamicConfiguration();
    if ((value = dynamicConfiguration.getConfig(key, "easymock")) != null) {
        return value;
    }
    return CONFIGURATION.getString(key, defaultValue);
}

```
invoke方法前植入mock逻辑
```
if(IS_MOCK){
    String mockValue= getConfig("easymock."+invocation.getServiceName()+"#"+invocation.getMethodName(),null);
    if(mockValue!=null&&mockValue.length()>0){
        java.lang.reflect.Type[] returnTypes = io.github.cmt.dema.util.ClassHelper.getReturnType($1.getServiceName(), $1.getMethodName(), $1.getParameterTypes());
        return new org.apache.dubbo.rpc.AppResponse(io.github.cmt.dema.MockValueResolver.resolve(mockValue, returnTypes[0], returnTypes.length > 1 ? returnTypes[1] : null));
    }
}
```

与[dubbo-easy-mock](https://github.com/dsc-cmt/dubbo-easy-mock)项目不同不是，这边不会将请求转发到外部的Http Mock服务器，而是使用到了Dubbo2.7新增的配置中心特性，会优先从外部配置中心(比如apollo)读取配置。

## 使用方式
0. 打包得到`dubbo-easy-mock-agent.jar`
1. 你的应用jvm参数增加`-javaagent:/{path}/dubbo-easy-mock-agent.jar`
3. 进行mock配置

## mock配置方式

0. 配置格式
不管是配置中心还是系统参数，或者properties文件，配置格式如下
```
## 开关
easymock.enable=true
## 针对具体方法的mock配置
easymock.io.github.shengchaojie.demo.DemoService#returnString={"data":"7758258"}
```

优先级从高到低如下

1. 外部配置，最新版dubbo支持apollo,consul,etcd,nacos,zookeeper
以常用的apollo为例，在apollo新建一个namespace=easymock,格式见0

2. 系统参数
通过jvm参数—D进行的配置
```
-Deasymock.enable=true
-Deasymock.io.github.shengchaojie.demo.DemoService#returnString={"data":"7758258"}
```

3.本地配置文件
见0

## 推荐测试demo
