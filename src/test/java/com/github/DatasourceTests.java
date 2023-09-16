package com.github;


import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.github.pagehelper.PageInterceptor;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * 不用启动spring boot也可以进行单元测试
 * 但是需要会用Mockito框架
 */
public class DatasourceTests {
    protected MybatisConfiguration configuration;
    protected SqlSession sqlSession;
    protected SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    public void setup() throws Exception { // 设置mybatis-plus
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        Class aclass = Class.forName("com.mysql.cj.jdbc.Driver");
        dataSource.setDriverClass(aclass);
        dataSource.setUrl("jdbc:mysql://localhost:3306/iassign?useSSL=false&characterEncoding=utf8&serverTimeZone=Asia/Shanghai");
        dataSource.setUsername("root");
        dataSource.setPassword("123456");
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        configuration = new MybatisConfiguration();
        configuration.setEnvironment(environment);
        configuration.setLazyLoadingEnabled(true);
        configuration.addMappers("com.github.iassign.mapper");
        configuration.addInterceptor(new PageInterceptor());
        MybatisSqlSessionFactoryBean sqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sqlSessionFactoryBean.setConfiguration(configuration);
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactory = sqlSessionFactoryBean.getObject();
        sqlSession = sqlSessionFactory.openSession(true);
    }

    public <T> T getMapper(Class<T> mapper) {
        return configuration.getMapper(mapper, sqlSession);
    }

    @Test
    public void testExpressRunner() throws Exception {
        ExpressRunner runner = new ExpressRunner();
        DefaultContext<String, Object> context = new DefaultContext<String, Object>();
        context.put("a", "1000000");
        context.put("b", "A");
        context.put("c", 3);
        String express = "Integer.parseInt(a)>=2";
        Object r = runner.execute(express, context, null, false, false);
        System.out.println(r);
    }

}
