Spring SQL Unit
=====================
This projects aims to facilitate integration tests of [Spring][1] projects. When running an integration tests, often some data is expected to be inside a database. Using annotations, you will be able to instrument your test method with data using SQL scripts.

Usage
---------
To start with, create an integration test with the correct context configurations. Example: 

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration(locations = {
            "classpath:/applicationContext.xml"
    })
    @TransactionConfiguration
    @TestExecutionListeners({
            DependencyInjectionTestExecutionListener.class,
            TransactionalTestExecutionListener.class
    
    })
    @Transactional
    public class IntegrationTest {
    
        @Test
        public void testExample() throws Exception {
            assertThat(true).isTrue();
        }
    
    }

As in many projects, this integration test is configured using:

 - SpringJunit4ClassRunner
 - Context configuration loaded from an xml file (applicationContext.xml). This xml file contains the basic Spring configuration like a datasource
 - TransactionConfiguration, using the defaults as datasource "dataSource " and defaultRollback "true"
 - 2 Test execution listeners for performing the dependencyinjection and the transactional
 - Finally, by default everything is transactional

Imagine now that we have a table inside our database called "Example". In this test method, we expect that there is 1 entry (and only that entry) inside the database.

First, create the sql file (example_script.sql):

    DELETE FROM example;
    INSERT INTO example (id, name) VALUES (1, 'Davy Van Roy');

Save this file inside your test resources folder, for example in a directory called "scripts". If you use maven, your structure will ook like this:

    - projectName
      - src
        - main 
        - test
          - java
          - resources
            - scripts
              - example_script.sql
            - ...
            
Now we can alter our existing test to execute this SQL script before the test method starts:

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration(locations = {
            "classpath:/applicationContext.xml"
    })
    @TransactionConfiguration
    @TestExecutionListeners({
            DependencyInjectionTestExecutionListener.class,
            TransactionalTestExecutionListener.class,
            SpringSqlUnitTestExecutionListener.class
    })
    @Transactional
    public class IntegrationTest {
    @Test
    @SqlSetUp(files = "scripts/example_script.sql")
    public void testExample() throws Exception {
        assertThat(true).isTrue();
    }

}


**Congratiulations**! You have now instrumented your test with your sql script!


DataSource
---------
In the example above, a SQL script was executed. But against which database? Let's look at our annotation:

    public @interface SqlSetUp {
        String dataSource() default "dataSource";
    
        String[] files() default {};
    
    }

So by default as dataSource, the bean with name "dataSource" will be used. If you want to use a different datasource, provide a different bean name here in the annotation (and make sure the bean exists within the test context).
Convention over configuration is the idea here.

Transactions
---------
By default, if you have configured a transaction manager in your test and your method is transactional, the SQL script will be executed within this transaction. 

So in the example above, everything will be rolled back because the transactions are configured as default rollback.

Important: if you supply a commit/rollback inside your script, this will be executed. So if you provide a commit, the data that was executed up till that commit will be committed to the database (same for rollback). 

Directories
---------
It is also possible to use directories as "sql files". When a directory is encountered, the directory will be recursively scanned for all sql files and they will be executed in an alphatical order.

    @SqlSetUp(files = "scripts")
Will execute all sql files inside the scripts folder. For example, this could come in handy for reference data. You can split all your sql files to keep manage the data (divide and conquer) but you won't have the hassle of specifiying each sql file in the annotation (and reunite to rule).


  [1]: http://projects.spring.io/spring-framework/
