/*
 * Copyright 2004-2014 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.framework.unit;

import jp.sourceforge.hotchpotch.s2junit.S2TestRuleTest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.seasar.framework.unit.annotation.PostBindFields;
import org.seasar.framework.unit.annotation.PreUnbindFields;

import junit.framework.TestCase;

public class Seasar2AdditionalTest extends TestCase {

    private static String log;

    @Override
    public void setUp() {
        log = "";
        Seasar2.configure();
    }

    @Override
    public void tearDown() {
        Seasar2.dispose();
        S2TestMethodRunner.s2junit4Path = S2TestMethodRunner.DEFAULT_S2JUNIT4_PATH;
    }

    // 追加分
    @RunWith(Seasar2.class)
    public static class ExtendsTestParent {

        @Test
        public void aaa() {
            log += "a";
        }

        @Before
        public void bbb1() {
            log += "b";
        }

        @After
        public void eee1() {
            log += "e";
        }

        @PostBindFields
        public void ggg1() {
            log += "g";
        }

        @PreUnbindFields
        public void hhh1() {
            log += "h";
        }

        @BeforeClass
        public static void kkk1() {
            log += "k";
        }

        @AfterClass
        public static void lll1() {
            log += "l";
        }

    }

    public static class ExtendsTest extends ExtendsTestParent {

        @Test
        public void aaa() {
            log += "A";
        }

        @Before
        public void bbb2() {
            log += "B";
        }

        @After
        public void eee2() {
            log += "E";
        }

        @PostBindFields
        public void ggg2() {
            log += "G";
        }

        @PreUnbindFields
        public void hhh2() {
            log += "H";
        }

        @BeforeClass
        public static void kkk2() {
            log += "K";
        }

        @AfterClass
        public static void lll2() {
            log += "L";
        }

    }

    // 追加分
    public void testExtendsTestClass() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(ExtendsTest.class);
        S2TestRuleTest.printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        /*
         * @Before,@BeforeClassは親クラスが先に実行される。@After,@AfterClassは子クラスが先に実行される
         * 一方で、@PostBindFieldsは子クラスが先に実行される。@PreUnbindFieldsも子クラスが先のようだ。
         */
        assertEquals("kKbBGgAHhEeLl", log);
    }

}
