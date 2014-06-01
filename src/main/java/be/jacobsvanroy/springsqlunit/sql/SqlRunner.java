package be.jacobsvanroy.springsqlunit.sql;

import be.jacobsvanroy.springsqlunit.comparator.FileComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (C) 2014  Davy Van Roy
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */


public class SqlRunner {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final SqlExecutor sqlExecutor;

    public static SqlRunner of(SqlExecutor sqlExecutor) {
        return new SqlRunner(sqlExecutor);
    }

    private SqlRunner(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    public void runSqlFiles(String[] files, DataSource dataSource) {
        for (String file : files) {
            runSqlFile(file, dataSource);
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
        sqlExecutor.executeSqlScript(dataSource, new FileSystemResource(file));
    }

    private void executeDirectory(DataSource dataSource, File directory) {
        List<File> files = getSortedFiles(directory.listFiles());
        for (File file : files) {
            runSqlFile(
                    file
                    , dataSource);
        }
    }

}
