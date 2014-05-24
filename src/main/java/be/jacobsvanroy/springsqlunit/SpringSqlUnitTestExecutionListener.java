package be.jacobsvanroy.springsqlunit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
 *  Copyright (C) 2014  Davy Van Roy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        if (resource.exists()) {
            if (isDirectory(resource)) {
                executeDirectory(dataSource, resource);
            } else {
                executeSqlScript(dataSource, resource);
            }
        } else {
            throw new RuntimeException("File " + sqlFile + " does not exist");
        }
    }

    private void executeDirectory(DataSource dataSource, ClassPathResource resource) {
        List<File> files = Arrays.asList(getFile(resource).listFiles());
        Collections.sort(files, new FileComparator());
        for (File file : files) {
            String classPathLocation = getClassPathLocation(resource, file);
            runSqlFile(
                    classPathLocation
                    , dataSource);
        }
    }

    private void executeSqlScript(DataSource dataSource, Resource resource) {
        logger.debug("Running sql file: " + resource.getFilename());
        ScriptUtils.executeSqlScript(dataSource, resource);
    }

    private String getClassPathLocation(ClassPathResource resource, File f) {
        int index = f.getPath().indexOf(resource.getPath());
        return f.getPath().substring(index);
    }

    private boolean isDirectory(Resource resource) {
        return getFile(resource).isDirectory();
    }

    private File getFile(Resource resource) {
        try {
            return resource.getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
