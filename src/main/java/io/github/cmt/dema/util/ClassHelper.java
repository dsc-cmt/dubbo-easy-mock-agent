package io.github.cmt.dema.util;


import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author shengchaojie
 * @date 2019-03-04
 **/
public class ClassHelper {

    public static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Character.class
                || type == Boolean.class
                || type == Byte.class
                || type == Short.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class
                || type == Object.class;
    }

    public static Type[] getReturnType(String interfaceName,String methodName,Class<?>[] parameterTypes){
        Class<?> cls = ReflectUtils.forName(interfaceName);
        Method method = null;
        try {
            method = cls.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
        return new Type[]{method.getReturnType(), method.getGenericReturnType()};
    }

    public static ClassLoader getClassLoader(){
        return getClassLoader(ClassHelper.class);
    }

    public static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = clazz.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }

        return cl;
    }

}
