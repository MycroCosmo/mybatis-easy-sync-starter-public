package com.thenoah.dev.mybatis_easy_starter.autosql;

import com.thenoah.dev.mybatis_easy_starter.config.MybatisEasyAutoConfiguration;
import com.thenoah.dev.mybatis_easy_starter.config.MybatisEasyProperties;
import com.thenoah.dev.mybatis_easy_starter.support.ColumnAnalyzer;
import com.thenoah.dev.mybatis_easy_starter.support.naming.NamingStrategy;
import com.thenoah.dev.mybatis_easy_starter.support.naming.NamingStrategyHolder;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoSqlFailurePropagationTest {

    @Test
    void mapperConfigurationPropagatesMetadataFailure() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getClassLoader()).thenReturn(getClass().getClassLoader());
        when(context.getBean(DataSource.class)).thenReturn(new DriverManagerDataSource("jdbc:h2:mem:failure"));
        MybatisProperties mybatis = new MybatisProperties();
        mybatis.setMapperLocations(new String[]{"classpath:mapper/MemberMapper.xml"});
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("mybatisProperties", mybatis);
        MybatisEasyProperties props = new MybatisEasyProperties();
        props.getAutoSql().setEnabled(true);
        var configuration = new MybatisEasyAutoConfiguration(context, beans, new MockEnvironment());
        var customizer = configuration.mybatisEasySqlSessionFactoryBeanCustomizer(props,
                beans.getBeanProvider(MybatisProperties.class));
        NamingStrategy invalidNaming = new NamingStrategy() {
            @Override public String tableName(Class<?> type) { return "members"; }
            @Override public String columnName(String fieldName) {
                throw new IllegalArgumentException("Unsupported column: " + fieldName);
            }
        };

        ColumnAnalyzer.clearCache();
        try (var ignored = NamingStrategyHolder.withStrategy(invalidNaming)) {
            assertThatThrownBy(() -> customizer.customize(new SqlSessionFactoryBean()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(Member.class.getName())
                    .hasRootCauseMessage("Unsupported column: name");
        } finally {
            ColumnAnalyzer.clearCache();
        }
    }
}
