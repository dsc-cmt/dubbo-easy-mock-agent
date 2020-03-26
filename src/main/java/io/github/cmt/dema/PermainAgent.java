package io.github.cmt.dema;

import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * @author shengchaojie
 * @date 2020-03-24
 **/
public class PermainAgent {

    private static Instrumentation INST;

    public static void premain(String agentArgs, Instrumentation inst) {
        INST = inst;
        process();
    }

    private static void process() {
        INST.addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className,
                                    Class<?> clazz,
                                    ProtectionDomain protectionDomain,
                                    byte[] byteCode) {
                if ("org.apache.dubbo.rpc.cluster.support.wrapper.MockClusterInvoker".replace(".", "/").equals(className)) {
                    ClassPool pool = ClassPool.getDefault();
                    CtClass cc = null;
                    try {
                        cc = pool.makeClass(new ByteArrayInputStream(byteCode));
                        CtField ctField = CtField.make("private static final org.apache.dubbo.common.config.Configuration CONFIGURATION =  org.apache.dubbo.rpc.model.ApplicationModel.getEnvironment().getConfiguration();", cc);
                        cc.addField(ctField);

                        ctField = CtField.make("private static final boolean IS_MOCK = Boolean.parseBoolean(getConfig(\"easymock.enable\",\"false\"));", cc);
                        cc.addField(ctField);

                        //获取配置方法
                        StringBuilder getConfigCode = new StringBuilder();
                        getConfigCode.append("private static String getConfig(String key, String defaultValue) {");
                        getConfigCode.append("String value;");
                        getConfigCode.append("org.apache.dubbo.common.config.configcenter.DynamicConfiguration dynamicConfiguration = org.apache.dubbo.common.config.configcenter.DynamicConfiguration.getDynamicConfiguration();");
                        getConfigCode.append("if ((value = dynamicConfiguration.getConfig(key, \"easymock\")) != null) {");
//                        getConfigCode.append("System.out.println(value);");
                        getConfigCode.append("return value;");
                        getConfigCode.append("}");
                        getConfigCode.append("return CONFIGURATION.getString(key, defaultValue);");
                        getConfigCode.append("}");
                        CtMethod getConfigMethod = CtMethod.make(getConfigCode.toString(),cc);
                        cc.addMethod(getConfigMethod);


                        //InvokerInvocationHandler的invoke方法植入mock逻辑
                        CtMethod method = cc.getDeclaredMethod("invoke");
                        StringBuilder code = new StringBuilder();
                        code.append("{");//#0
//                        code.append("System.out.println(\"=========\");");
                        code.append("System.out.println(IS_MOCK);");
                        code.append("if(IS_MOCK){");//#1
                        code.append("String mockValue= getConfig(\"easymock.\"+$1.getServiceName()+\"#\"+$1.getMethodName(),null);");
//                        code.append("System.out.println(\"easymock.\"+$1.getServiceName()+\"#\"+$1.getMethodName());");
//                        code.append("System.out.println(mockValue);");
                        code.append("if(mockValue!=null&&mockValue.length()>0){");//#2

                        code.append("java.lang.reflect.Type[] returnTypes = io.github.cmt.dema.util.ClassHelper.getReturnType($1.getServiceName(), $1.getMethodName(), $1.getParameterTypes());");
                        code.append("return new org.apache.dubbo.rpc.AppResponse(io.github.cmt.dema.MockValueResolver.resolve(mockValue, returnTypes[0], returnTypes.length > 1 ? returnTypes[1] : null));");

                        code.append("}");//#2

                        code.append("}");//#1

                        code.append("}");//#0

                        method.insertBefore(code.toString());
                        return cc.toBytecode();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    } catch (CannotCompileException e) {
                        e.printStackTrace();
                    } finally {
                        if (cc != null) {
                            cc.detach();
                        }
                    }
                }

                return byteCode;
            }
        });
    }

}
