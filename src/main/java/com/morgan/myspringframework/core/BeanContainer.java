package com.morgan.myspringframework.core;

import com.morgan.myspringframework.core.annotation.Component;
import com.morgan.myspringframework.core.annotation.Controller;
import com.morgan.myspringframework.core.annotation.Repository;
import com.morgan.myspringframework.core.annotation.Service;
import com.morgan.myspringframework.utils.ClassUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean容器
 * @author chenb14
 * @date 2022/6/30 16:31
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanContainer {

    /**
     * 容器存储Bean实例的载体
     * k: Class对象能唯一标识一个类
     * v：Object能容纳所有类型的对象
     */
    private Map<Class<?>,Object> beanMap = new ConcurrentHashMap<>();

    /**
     * 定义被加载的Bean的修饰注解类型
     */
    private List<Class<? extends Annotation>> beanAnnotations = Arrays.asList(Controller.class, Service.class, Component.class, Repository.class);

    /**
     * 是否被加载过
     */
    private volatile boolean isLoaded = false;

    /**
     * 需要保证绝对的单例Bean，一般的单例模式不能防止反射和序列化的攻击，使用枚举类进行单例的初始化能够防止反射和序列化的构建更加。
     * 一个类标记为枚举则表示继承Enum类，而Enum的构造方法为带参数的，即使反射使用带参数的方式去获取构造方法，底层也会判断是否是枚举类型，不能够实例化
     */
    public enum BeanContainerHolderEnum {
        /**
         * 持有者
         */
        Holder;
        /**
         * 单例容器Bean
         */
        private BeanContainer beanContainer;

        BeanContainerHolderEnum(){
            beanContainer = new BeanContainer();
        }
    }

    /**
     * 公共获取容器方法
     */
    public static BeanContainer getInstance(){
        return BeanContainerHolderEnum.Holder.beanContainer;
    }

    /**
     * 是否加载过Bean
     */
    public boolean isLoaded(){
        return isLoaded;
    }

    /**
     * Bean的数量
     */
    public int size(){
        return beanMap.size();
    }

    /**
     * 扫描加载所有的Bean实例
     */
    public synchronized void loadBeans(String scanBasePackage){
        // 包名是否为空
        if (!StringUtils.hasLength(scanBasePackage)){
            log.warn("scanBasePackage must not null, scanBasePackage: {}", scanBasePackage);
            return;
        }
        // 判断是否已经加载过
        if (isLoaded()){
            log.warn("BeanContainer has been loaded");
            return;
        }
        // 扫描包，加载Class对象
        Set<Class<?>> classSet = ClassUtil.extractPackageClass(scanBasePackage);
        // 判断集合是否为空
        if (CollectionUtils.isEmpty(classSet)){
            log.warn("extract nothing from packageName: {}", scanBasePackage);
            return;
        }
        // 把被指定注解修饰的Class对象放入BeanMap
        classSet.forEach(clazz -> {
            beanAnnotations.forEach(beanAnnotation -> {
                if (clazz.isAnnotationPresent(beanAnnotation) && Objects.isNull(beanMap.get(clazz))){
                    beanMap.put(clazz,ClassUtil.newInstance(clazz,true));
                }
            });
        });
    }

    /**
     * Bean常规操作(设置、获取、移除Bean)
     */
}
