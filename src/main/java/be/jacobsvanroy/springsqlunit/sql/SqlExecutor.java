package be.jacobsvanroy.springsqlunit.sql;

import be.jacobsvanroy.springsqlunit.util.ScriptUtils;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;

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

public class SqlExecutor {

    public void executeSqlScript(DataSource dataSource, Resource resource) {
        ScriptUtils.executeSqlScript(dataSource, resource);
    }
}
