package com.itwake.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2017/8/19 1:03
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> READONLY_HOLDER = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, List<String>> MASTER_SLAVES = new ConcurrentHashMap<>();

    public DynamicDataSource(){

    }

    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        if(READONLY_HOLDER.get()!=null){
            connection.setReadOnly(READONLY_HOLDER.get());
        }
        return connection;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return getDataSource();
    }

    public static void setDataSource(String dataSource) {
        CONTEXT_HOLDER.set(dataSource);
        READONLY_HOLDER.set(false);
    }

    public static void setDataSource(String dataSource, boolean readonly) {
        CONTEXT_HOLDER.set(dataSource);
        READONLY_HOLDER.set(readonly);
    }

    public static String getDataSource() {
        return CONTEXT_HOLDER.get();
    }

    public static void clearDataSource() {
        CONTEXT_HOLDER.remove();
        if(READONLY_HOLDER.get()!=null) {
            READONLY_HOLDER.remove();
        }
    }

    public static boolean hasDataSource() {
        return CONTEXT_HOLDER.get() != null;
    }

    /**
     * 设置主从数据源关系
     * @param masterSlaves
     */
    public void setMasterSlaves(LinkedHashMap<String, List<String>> masterSlaves){
        MASTER_SLAVES.clear();
        MASTER_SLAVES.putAll(masterSlaves);
    }

    /**
     * 设置可用数据源，并指定默认数据源
     * @param dataSources
     */
    public void setDataSources(Map<Object, Object> dataSources) {
        super.setTargetDataSources(dataSources);
        //取得默认数据源
        Object defaultDataSource = dataSources.get("default");
        if(defaultDataSource == null && !dataSources.isEmpty()){
            Object dataSourceName = (dataSources.keySet()).toArray()[0];
            defaultDataSource = dataSources.get(dataSourceName);
            if(defaultDataSource!=null){
                logger.info("设置默认数据源为：" + dataSourceName);
            }
        }
        if(defaultDataSource == null){
            logger.warn("数据源为空");
        }
        setDefaultTargetDataSource(defaultDataSource);
    }

    /**
     * 获取从节点数据源
     * @param master 主节点名称
     * @return 从节点名称
     */
    public static String getSlaveDataSource(String master){
        List<String> slaveList = MASTER_SLAVES.get(master);
        //从节点为空，当前数据源
        if(slaveList==null || slaveList.isEmpty()){
            return master;
        }
        String slave = slaveList.get(0);
        //取第一个从节点
        if(slaveList.size()>1){
            synchronized (master){
                slaveList.remove(0);
                slaveList.add(slave);
            }
        }
        return slave;
    }

}
