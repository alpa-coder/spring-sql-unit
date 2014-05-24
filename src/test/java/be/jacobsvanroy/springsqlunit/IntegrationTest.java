package be.jacobsvanroy.springsqlunit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext.xml"
})
@TransactionConfiguration
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        SpringSqlUnitTestExecutionListener.class

})
@Transactional
@DirtiesContext
public class IntegrationTest {

    private static DataSource DATA_SOURCE;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        DATA_SOURCE = dataSource;
    }

    @Test
    public void testHasDataSource() throws Exception {
        assertThat(DATA_SOURCE).isNotNull();

    }

    public static Long getCount() {
        return new JdbcTemplate(DATA_SOURCE).queryForObject("select count(*) from spring_sql_test", Long.class);
    }

    public static List<String> getResults() {
        return new JdbcTemplate(DATA_SOURCE).queryForList("select col from spring_sql_test", String.class);
    }

}
