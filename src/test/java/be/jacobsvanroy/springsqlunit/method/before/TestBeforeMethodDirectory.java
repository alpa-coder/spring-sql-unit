package be.jacobsvanroy.springsqlunit.method.before;

import be.jacobsvanroy.springsqlunit.IntegrationTest;
import be.jacobsvanroy.springsqlunit.SqlBefore;
import org.junit.BeforeClass;
import org.junit.Test;

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
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

public class TestBeforeMethodDirectory extends IntegrationTest {

    @Test
    @SqlBefore(files = "scripts/test_directory")
    public void testBeforeDirectory() throws Exception {
        assertThat(getCount()).isEqualTo(2);
    }
}
