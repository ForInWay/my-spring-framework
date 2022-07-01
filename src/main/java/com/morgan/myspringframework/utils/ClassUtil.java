package com.morgan.myspringframework.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 类操作工具类
 * @author chenb14
 * @date 2022/6/30 20:40
 */
@Slf4j
public class ClassUtil {

    /**
     * 资源协议类型
     */
    private static final String FILE_PROTOCOL = "file";

    /**
     * 加载的类后缀
     */
    private static final String CLASS_SUFFIX = ".class";

    /**
     * 扫描包，加载Class对象
     * @param packageName 包名
     * @return class对象集合
     */
    public static Set<Class<?>> extractPackageClass(String packageName) {
        // 获取类加载器
        ClassLoader classLoader = getClassLoader();
        // 通过类加载器加载资源(包名)
        URL url = classLoader.getResource(packageName.replace(".", "/"));
        if (null == url){
            log.warn("unable to retrieve anything from package: {}", packageName);
            return Collections.emptySet();
        }
        Set<Class<?>> classSet = new HashSet<>();
        // 判断资源类型，现在的资源类型是file开头
        if (FILE_PROTOCOL.equalsIgnoreCase(url.getProtocol())){
            // 获取文件(文件包含文件和文件夹)，遍历文件夹
            File file = new File(url.getPath());
            extractClassFile(classSet,file, packageName);
        }
        // 其他类型的资源的加载
        return classSet;
    }

    /**
     * 解析具体的文件及文件夹
     * @param classSet class类对象集合
     * @param file 文件或文件夹
     * @param packageName 包名
     */
    public static void extractClassFile(Set<Class<?>> classSet, File file, String packageName) {
        // 如果文件类型为非文件夹，则直接返回
        if (!file.isDirectory()){
            return;
        }
        // 如果文件类型为文件夹，则遍历该文件夹下的所有文件与文件夹，文件的话，生成class对象，放入class对象集合；文件夹的话，则返回
        File[] childFiles = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File childFile) {
                // 如果为目录，则直接返回
                if (childFile.isDirectory()){
                    return true;
                }
                // 获取文件的绝对路径
                String absolutePath = childFile.getAbsolutePath();
                // 如果以.class结尾，则可以放入class对象集合
                if (absolutePath.endsWith(CLASS_SUFFIX)){
                    addToClassSet(absolutePath);
                }
                return false;
            }

            public void addToClassSet(String absolutePath) {
                //1.从class文件的绝对值路径里提取出包含了package的类名
                //如/Users/baidu/imooc/springframework/sampleframework/target/classes/com/imooc/entity/dto/MainPageInfoDTO.class
                //需要弄成com.imooc.entity.dto.MainPageInfoDTO
                String fullPackageName = absolutePath.substring(absolutePath.indexOf(packageName)).replace(File.separator, ".");
                String className = fullPackageName.substring(0, fullPackageName.indexOf(CLASS_SUFFIX));
                // 根据文件名生成Class对象
                Class<?> clazz = ClassUtil.loadClass(className);
                classSet.add(clazz);
            }
        });
        // 遍历返回的文件夹集合，递归调用extractClassFile方法
        if (null != childFiles){
            for (File childFile: childFiles) {
                extractClassFile(classSet,childFile,packageName);
            }
        }
    }

    /**
     * 根据class包路径名创建class对象
     * @param className 包路径名
     * @return class对象
     */
    public static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error("class not found, className: {}",className);
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据class创建类实例对象
     * @param clazz class对象
     * @param accessible 是否支持创建出私有class对象的实例
     * @return class实例化对象
     */
    public static <T> T newInstance(Class<?> clazz, boolean accessible) {
        try {
            Constructor<?> declaredConstructor = clazz.getDeclaredConstructor(clazz);
            declaredConstructor.setAccessible(accessible);
            return (T) declaredConstructor.newInstance();
        } catch (Exception e) {
            log.error("create new instance fail，class：{}, error: {}", clazz, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取当前线程的类加载器
     * @return 类加载器对象
     */
    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
