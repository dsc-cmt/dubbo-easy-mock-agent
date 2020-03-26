package io.github.cmt.dema;

import com.alibaba.fastjson.JSON;
import io.github.cmt.dema.util.ClassHelper;

import java.lang.reflect.Type;

/**
 * @author shengchaojie
 * @date 2020-02-09
 **/
public class MockValueResolver {

    public static Object resolve(String mockValue, Type type,Type genericType) {
        if (Void.TYPE.isAssignableFrom((Class<?>) type)) {
            return null;
        }

        Object value = null;
        if (ClassHelper.isPrimitive((Class<?>) type)) {
            //处理内置类型
            //解决easymock不支持基本类型返回的问题
            PrimitiveWrapper primitiveWrapper = JSON.parseObject(mockValue, PrimitiveWrapper.class);
            mockValue = primitiveWrapper.getData().toString();
            if(type == String.class){
                mockValue = "\""+ mockValue + "\"";
            }
            value = JSON.parseObject(mockValue,type);
        } else if (mockValue.startsWith("{") || mockValue.startsWith("[")) {
            //处理普通对象
            value = JSON.parseObject(mockValue, genericType!=null?genericType:type);
        } else {
            value = mockValue;
        }

        return value;
    }

    public static void main(String[] args) {
        PrimitiveWrapper primitiveWrapper = JSON.parseObject("{\"data\":\"123\"}", PrimitiveWrapper.class);
        System.out.println(primitiveWrapper.getData());
    }

}
