package com.thenoah.dev.mybatis_easy_starter.tool.generator;

import com.thenoah.dev.mybatis_easy_starter.config.MybatisEasyAutoConfiguration;
import com.thenoah.dev.mybatis_easy_starter.config.MybatisEasyProperties;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ResourceLock("user.dir")
class EntityGeneratorCompilationTest {

    @TempDir
    Path tempDir;

    @Test
    void generatedEntityCompilesWithoutLombok() throws Exception {
        generate(new EntityGenerator(), "id", "display_name");
        compileConsumer("entity.setId(1L); entity.setDisplayName(\"alice\"); "
                + "Long id = entity.getId(); String name = entity.getDisplayName();");
        generate(new EntityGenerator(), "id", "display_name", "email");
        compileConsumer("entity.setDisplayName(\"alice\"); entity.setEmail(\"a@example.com\"); "
                + "String email = entity.getEmail();");
    }

    @Test
    void synchronizedEntityCompilesWithNewAccessorsAndStaysUnchangedOnRepeat() throws Exception {
        Files.createDirectories(entitySource().getParent());
        Files.writeString(entitySource(), """
                package example.generated;
                public class SampleUser {
                    private Long id;
                    public Long getId() { return id; }
                    public void setId(Long id) { this.id = id; }
                    @Override public String toString() { return "custom"; }
                }
                """);
        generate(new EntityGenerator(), "id", "email");
        compileConsumer("entity.setId(1L); entity.setEmail(\"a@example.com\"); "
                + "String email = entity.getEmail();");

        String firstSync = Files.readString(entitySource());
        generate(new EntityGenerator(), "id", "email");
        assertEquals(firstSync, Files.readString(entitySource()));
    }

    @Test
    void configuredLombokOptionRetainsDataGeneration() throws Exception {
        var properties = new Binder(new MapConfigurationPropertySource(
                Map.of("mybatis-easy.generator.use-lombok", "true")))
                .bind("mybatis-easy", Bindable.of(MybatisEasyProperties.class)).get();
        var configuration = new MybatisEasyAutoConfiguration(mock(ApplicationContext.class),
                mock(BeanFactory.class), new MockEnvironment());
        EntityGenerator generator = configuration.entityGenerator(dataSource("id"), properties);
        generate(generator, "id", "display_name");

        assertThat(Files.readString(entitySource()))
                .contains("import lombok.Data;", "@Data", "private String displayName;")
                .doesNotContain("getDisplayName(", "setDisplayName(");
    }

    @ParameterizedTest
    @ValueSource(strings = {"@Data", "@lombok.Data"})
    void existingLombokEntitiesKeepTheirAccessorStyle(String annotation) throws Exception {
        Files.createDirectories(entitySource().getParent());
        Files.writeString(entitySource(), "package example.generated;\nimport lombok.Data;\n"
                + annotation + "\npublic class SampleUser {\n    private Long id;\n}\n");
        generate(new EntityGenerator(), "id", "email");

        assertThat(Files.readString(entitySource()))
                .contains(annotation, "private String email;")
                .doesNotContain("getEmail(", "setEmail(");
    }

    private Path entitySource() {
        return tempDir.resolve("java/example/generated/SampleUser.java");
    }

    private void generate(EntityGenerator generator, String... columns) throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            generator.generate(dataSource(columns), "example.generated", false, false, "",
                    Map.of(), "misc", tempDir.resolve("java"), tempDir.resolve("mapper"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    private void compileConsumer(String calls) throws Exception {
        Path consumer = tempDir.resolve("Consumer.java");
        Files.writeString(consumer, "import example.generated.SampleUser; "
                + "class Consumer { void use() { SampleUser entity = new SampleUser(); " + calls + " } }");
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        String classpath = System.getProperty("consumer.classpath");
        assertNotNull(classpath);
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        Path classes = Files.createDirectories(tempDir.resolve("classes"));
        int exitCode = compiler.run(null, null, errors, "-classpath", classpath,
                "-d", classes.toString(), entitySource().toString(), consumer.toString());
        assertEquals(0, exitCode, errors.toString(StandardCharsets.UTF_8));
    }

    private DataSource dataSource(String... columns) throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        ResultSet fields = mock(ResultSet.class);
        AtomicInteger row = new AtomicInteger(-1);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metadata.getTables(null, null, null, new String[]{"TABLE"})).thenReturn(tables);
        when(tables.next()).thenReturn(true, false);
        when(tables.getString("TABLE_NAME")).thenReturn("sample_user");
        when(metadata.getColumns(null, null, "sample_user", null)).thenReturn(fields);
        when(fields.next()).thenAnswer(invocation -> row.incrementAndGet() < columns.length);
        when(fields.getString("COLUMN_NAME")).thenAnswer(invocation -> columns[row.get()]);
        when(fields.getInt("DATA_TYPE")).thenAnswer(invocation ->
                columns[row.get()].equals("id") ? Types.BIGINT : Types.VARCHAR);
        return source;
    }
}
