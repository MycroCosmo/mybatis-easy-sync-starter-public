package com.thenoah.dev.mybatis_easy_starter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumerCompilationTest {

    @Test
    void coreJarDoesNotBreakConsumerCompilation(@TempDir Path tempDir) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK is required for consumer compilation");
        String classpath = System.getProperty("consumer.classpath");
        assertNotNull(classpath, "Use the core JAR and its runtime dependencies");

        Path source = tempDir.resolve("Consumer.java");
        Files.writeString(source, """
                import com.thenoah.dev.mybatis_easy_starter.core.mapper.BaseMapper;
                interface Consumer extends BaseMapper<Object, Long> {}
                """, StandardCharsets.UTF_8);
        Path classes = Files.createDirectory(tempDir.resolve("classes"));
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int exitCode = compiler.run(null, null, errors,
                "-classpath", classpath, "-d", classes.toString(), source.toString());

        assertEquals(0, exitCode, errors.toString(StandardCharsets.UTF_8));
        assertTrue(Files.exists(classes.resolve("Consumer.class")));
    }
}
