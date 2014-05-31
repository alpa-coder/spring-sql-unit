package be.jacobsvanroy.springsqlunit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (C) 2014  Davy Van Roy
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */


public class SpringSqlUnitTestExecutionListener extends AbstractTestExecutionListener {

    private final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void beforeTestClass(final TestContext testContext) throws Exception {
        super.beforeTestClass(testContext);
        logger.debug("Running before test class");
        runSqlBefore(testContext.getTestClass().getDeclaredAnnotations(), testContext.getApplicationContext());
    }

    @Override
    public void afterTestClass(final TestContext testContext) throws Exception {
        super.afterTestClass(testContext);
        logger.debug("Running after test class");
        runSqlAfter(testContext.getTestClass().getDeclaredAnnotations(), testContext.getApplicationContext());
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        super.beforeTestMethod(testContext);
        logger.debug("Running before test method");
        runSqlBefore(testContext.getTestMethod().getDeclaredAnnotations(), testContext.getApplicationContext());

    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        super.afterTestMethod(testContext);
        logger.debug("Running after test method");
        runSqlAfter(testContext.getTestMethod().getDeclaredAnnotations(), testContext.getApplicationContext());
    }

    private void runSqlAfter(Annotation[] declaredAnnotations, ApplicationContext appContext) {
        List<Annotation> annotations = getAnnotation(declaredAnnotations, SqlAfter.class);
        for (Annotation annotation : annotations) {
            SqlAfter sqlAfterAnnotation = (SqlAfter) annotation;
            runSqlFiles(appContext, sqlAfterAnnotation.files(), sqlAfterAnnotation.dataSource());
        }
    }

    private void runSqlBefore(Annotation[] declaredAnnotations, ApplicationContext appContext) {
        List<Annotation> annotations = getAnnotation(declaredAnnotations, SqlBefore.class);
        for (Annotation annotation : annotations) {
            SqlBefore sqlBeforeAnnotation = (SqlBefore) annotation;
            runSqlFiles(appContext, sqlBeforeAnnotation.files(), sqlBeforeAnnotation.dataSource());
        }
    }

    private List<Annotation> getAnnotation(Annotation[] declaredAnnotations, Class<? extends Annotation> annotationType) {
        List<Annotation> result = new ArrayList<Annotation>();
        for (Annotation declaredAnnotation : declaredAnnotations) {
            if (declaredAnnotation.annotationType().equals(annotationType)) {
                result.add(declaredAnnotation);
            }
        }
        return result;
    }

    private DataSource getDataSource(ApplicationContext appContext, String dataSource) {
        return (DataSource) appContext.getBean(dataSource);
    }

    private void runSqlFiles(ApplicationContext appContext, String[] files, String dataSource) {
        for (String file : files) {
            runSqlFile(file, getDataSource(appContext, dataSource));
        }
    }

    private void runSqlFile(String sqlFile, DataSource dataSource) {
        ClassPathResource resource = new ClassPathResource(sqlFile);
        File scriptFile = getFile(resource);
        runSqlFile(scriptFile, dataSource);
    }

    private void runSqlFile(File scriptFile, DataSource dataSource) {
        if (scriptFile.exists()) {
            if (scriptFile.isDirectory()) {
                executeDirectory(dataSource, scriptFile);
            } else {
                executeSqlScript(dataSource, scriptFile);
            }
        } else {
            throw new RuntimeException("File " + scriptFile.getPath() + " does not exist");
        }
    }

    private File getFile(ClassPathResource resource) {
        File scriptFile;
        try {
            scriptFile = resource.getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return scriptFile;
    }

    private void executeDirectory(DataSource dataSource, File directory) {
        List<File> files = getSortedFiles(directory.listFiles());
        for (File file : files) {
            runSqlFile(
                    file
                    , dataSource);
        }
    }

    private List<File> getSortedFiles(File[] files) {
        if (files == null) {
            return new ArrayList<File>();
        }
        List<File> result = Arrays.asList(files);
        Collections.sort(result, new FileComparator());
        return result;
    }

    private void executeSqlScript(DataSource dataSource, File file) {
        logger.debug("Running sql file: " + file.getName());
        ScriptUtils.executeSqlScript(dataSource, new FileSystemResource(file));
    }

}
