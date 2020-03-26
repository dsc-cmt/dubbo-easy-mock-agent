## 这是啥
一个用于对dubbo接口进行mock的agent

## 支持版本
apache dubbo 2.7.5

## 原理
通过javassist对dubbo框架中的InvokerInvocationHandler植入mock逻辑，生成新的InvokerInvocationHandler大致如下
```
public class InvokerInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);
    private final Invoker<?> invoker;

    //植入逻辑
    private static final Configuration CONFIGURATION =  Environment.getInstance().getConfiguration("easymock",null);
    //植入逻辑
    private static final boolean IS_MOCK = Boolean.parseBoolean(CONFIGURATION.getString("enable","false"));

    public InvokerInvocationHandler(Invoker<?> handler) {
        this.invoker = handler;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(invoker, args);
        }
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return invoker.toString();
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return invoker.hashCode();
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return invoker.equals(args[0]);
        }

        //植入逻辑
        if(IS_MOCK){
            String mockValue= CONFIGURATION.getString(invoker.getInterface().getName()+"#"+method.getName());
            if(mockValue!=null&&mockValue.length()>0){
                Type[] returnTypes = ClassHelper.getReturnType(invoker.getInterface().getName(), method.getName(), method.getParameterTypes());
                return MockValueResolver.resolve(mockValue, returnTypes[0], returnTypes.length > 1 ? returnTypes[1] : null);
            }
        }

        return invoker.invoke(createInvocation(method, args)).recreate();
    }

    private RpcInvocation createInvocation(Method method, Object[] args) {
        RpcInvocation invocation = new RpcInvocation(method, args);
        if (RpcUtils.hasFutureReturnType(method)) {
            invocation.setAttachment(Constants.FUTURE_RETURNTYPE_KEY, "true");
            invocation.setAttachment(Constants.ASYNC_KEY, "true");
        }
        return invocation;
    }

}

```
与Dubbo-easy-mock项目不同不是，这边不会将请求转发到外部的Http Mock服务器，而是使用到了Dubbo2.7新增的配置中心特性。会优先从配置中心读取配置，然后再从本地系统变量,以及dubbo.properties读取。

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
