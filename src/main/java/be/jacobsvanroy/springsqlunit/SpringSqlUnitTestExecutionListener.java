package be.jacobsvanroy.springsqlunit;

import be.jacobsvanroy.springsqlunit.annotation.SqlSetUp;
import be.jacobsvanroy.springsqlunit.sql.SqlExecutor;
import be.jacobsvanroy.springsqlunit.sql.SqlRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
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

    private SqlRunner sqlRunner;

    public SpringSqlUnitTestExecutionListener() {
        this.sqlRunner = SqlRunner.of(new SqlExecutor());
    }

    @Override
    public void beforeTestClass(final TestContext testContext) throws Exception {
        super.beforeTestClass(testContext);
        logger.trace("Running before test class");
        runSqlBefore(testContext.getTestClass().getDeclaredAnnotations(), testContext.getApplicationContext());
    }

    @Override
    public void afterTestClass(final TestContext testContext) throws Exception {
        super.afterTestClass(testContext);
        logger.trace("Running after test class");
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        super.beforeTestMethod(testContext);
        logger.trace("Running before test method");
        runSqlBefore(testContext.getTestMethod().getDeclaredAnnotations(), testContext.getApplicationContext());

    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        super.afterTestMethod(testContext);
        logger.trace("Running after test method");
    }

    private void runSqlBefore(Annotation[] declaredAnnotations, ApplicationContext appContext) {
        List<Annotation> annotations = getAnnotation(declaredAnnotations, SqlSetUp.class);
        for (Annotation annotation : annotations) {
            SqlSetUp sqlBeforeAnnotation = (SqlSetUp) annotation;
            sqlRunner.runSqlFiles(
                    sqlBeforeAnnotation.files(),
                    getDataSource(appContext, sqlBeforeAnnotation.dataSource()));
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


}
