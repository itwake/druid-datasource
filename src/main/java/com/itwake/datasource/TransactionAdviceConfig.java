package com.itwake.datasource;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class TransactionAdviceConfig {

    /**
     * 配置切面
     */
    public static final String AOP_POINTCUT_EXPRESSION = "execution(* com..*ServiceImpl.*(..)) || execution(* com.baomidou.mybatisplus.service.impl.ServiceImpl.*(..)) || execution(* com.baomidou.mybatisplus.mapper.BaseMapper.*(..))";

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Bean
    public TransactionInterceptor txAdvice() {
        DefaultTransactionAttribute REQUIRED_READONLY = new DefaultTransactionAttribute();
        REQUIRED_READONLY.setReadOnly(true);

        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
        for(String methodName : readonlyMethodNames){
            source.addTransactionalMethod(methodName + "*", REQUIRED_READONLY);
        }
        return new TransactionInterceptor(transactionManager, source);
    }

    @Bean
    public Advisor txAdviceAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(AOP_POINTCUT_EXPRESSION);
        return new DefaultPointcutAdvisor(pointcut, txAdvice());
    }

    /**
     * 只读的方法名称
     */
    private static String []readonlyMethodNames;
    @Value("${transaction.readonly-method:select,get,query,find,list,count}")
    public void setReadonlyMethodNames(String readonlyMethod){
        List<String> list = new ArrayList<>();
        if(!StringUtils.isEmpty(readonlyMethod)){
            for(String name : readonlyMethod.split(",")){
                list.add(name);
            }
        }
        readonlyMethodNames = new String[list.size()];
        readonlyMethodNames = list.toArray(readonlyMethodNames);
    }

    /**
     * 判断是否只读方法
     * @param method
     * @return
     */
    public static boolean isReadOnlyMethod(Method method){
        for(String name : readonlyMethodNames){
            if(method.getName().startsWith(name)){
                return true;
            }
        }
        return false;
    }
}