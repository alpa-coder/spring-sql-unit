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

But how did it know how to execute and what about transactions. 

For the first question, we can find the answer if we look at the annotation:

    public @interface SqlSetUp {
        String dataSource() default "dataSource";
    
        String[] files() default {};
    
    }

By default "dataSource" will be used. As many applications use "dataSource" as the bean name for the datasource, this is chosen by default (convention over configuration).  You can of course override this by given it a different name when annotating your method.  
Explaining the transaction management is easy, whatever you configured will be used. Just make sure transaction configuration is defined and your method (or class) is annotated with @Transactional. In the example above we did this, so every sql file will be rolled back after each test method. This gives us a clean start every step of the way. **So avoid using commits/rollbacks in your sql script. It doesn't make any sense.**

Directories
---------
It is also possible to use directories as "sql files". When a directory is encountered, the directory will be recursively scanned for all sql files and they will be executed in an alphatical order.

    @SqlSetUp(files = "scripts")
Will execute all sql files inside the scripts folder. For example, this could come in handy for reference data. You can split all your sql files to keep manage the data (divide and conquer) but you won't have the hassle of specifiying each sql file in the annotation (and reunite to rule).


  [1]: http://projects.spring.io/spring-framework/
