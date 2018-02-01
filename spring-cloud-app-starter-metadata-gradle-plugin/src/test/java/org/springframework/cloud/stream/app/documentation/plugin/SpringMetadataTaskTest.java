package org.springframework.cloud.stream.app.documentation.plugin;


import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.Assert.*;

public class SpringMetadataTaskTest {

    @Test
    public void simpleTest() throws Exception {

        //run build
        File testProjectDir = new File(getClass().getResource("/testProject/build.gradle").toURI()).getParentFile();

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .withArguments("clean", "build", "-S")
                .build();

        result.getTasks()
                .forEach(t -> assertThat(t.getOutcome(), CoreMatchers.not(CoreMatchers.is(TaskOutcome.FAILED))));


        // get the generated md jar
        File generatedMdFile = new File(getClass().getResource("/testProject/build/libs/testProject-metadata.jar").toURI());
        ZipFile generatedMdZipFile = new ZipFile(generatedMdFile);


        JsonMarshaller jsonMarshaller = new JsonMarshaller();


        // assert that  spring-configuration-metadata.json contains custom node and nodes from spring boot
        ZipEntry configMdEntry = generatedMdZipFile.getEntry("META-INF/spring-configuration-metadata.json");
        Assert.assertNotNull(configMdEntry);

        try (InputStream is = generatedMdZipFile.getInputStream(configMdEntry)) {
            ConfigurationMetadata configurationMetadata = jsonMarshaller.read(is);
            List<ItemMetadata> items = configurationMetadata.getItems();

            //expect the  metadata to be aggregated with spring boot dependency
            Assert.assertTrue(1<items.size());
            Optional<ItemMetadata> customItem = items
                    .stream()
                    .filter(i -> "my.custom".equals(i.getName()))
                    .findFirst();

            //expect the  metadata to be aggregated with owned items
            Assert.assertTrue(customItem.isPresent());
            Assert.assertEquals("java.lang.String",customItem.get().getType());
            Assert.assertEquals("Custom test description",customItem.get().getDescription());

        }



        // assert that  spring-configuration-metadata-whitelist.properties exists
        ZipEntry whiteListedPropsEntry= generatedMdZipFile.getEntry("META-INF/spring-configuration-metadata-whitelist.properties");
        Assert.assertNotNull(whiteListedPropsEntry);
        try (InputStream is = generatedMdZipFile.getInputStream(whiteListedPropsEntry)) {
            Properties wlProps = new Properties();
            wlProps.load(is);
            Assert.assertEquals("com.custom.Properties",wlProps.getProperty("configuration-properties.classes"));

        }



    }
}