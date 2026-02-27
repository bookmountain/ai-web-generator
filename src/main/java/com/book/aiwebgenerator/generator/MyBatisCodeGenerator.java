package com.book.aiwebgenerator.generator;


import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

public class MyBatisCodeGenerator {
    // Table names to generate code for, if not set, all tables will be generated
    private static final String[] TABLE_NAMES = {"user"};

    public static void main(String[] args) {
        // Get database connection information from application.yml
        Dict dict = YamlUtil.loadByPath("application.yml");
        Map<String, Object> dataSourceConfig = dict.getByPath("spring.datasource");
        String url = String.valueOf(dataSourceConfig.get("url"));
        String username = String.valueOf(dataSourceConfig.get("username"));
        String password = String.valueOf(dataSourceConfig.get("password"));
        // Create datasource
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // Create global configuration
        GlobalConfig globalConfig = createGlobalConfig();

        // Create code generator
        Generator generator = new Generator(dataSource, globalConfig);

        generator.generate();
    }

    // Read https://mybatis-flex.com/zh/others/codegen.html
    public static GlobalConfig createGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();

        globalConfig.getPackageConfig()
                .setBasePackage("com.book.aiwebgenerator.genresult");
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)
                .setLogicDeleteColumn("isDelete");

        // Generator supports generating entity, mapper, service, controller, etc.
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        globalConfig.enableMapper();
        globalConfig.enableMapperXml();
        globalConfig.enableService();
        globalConfig.enableServiceImpl();
        globalConfig.enableController();

        globalConfig.getJavadocConfig()
                .setAuthor(null)
                .setSince("");

        return globalConfig;
    }
}
