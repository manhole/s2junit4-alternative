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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class Seasar2AdditionalTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(Seasar2AdditionalTest.class);
    private static List<String> seq = new ArrayList<String>();

    @Override
    public void setUp() {
        seq.clear();
        Seasar2.configure();
    }

    @Override
    public void tearDown() {
        Seasar2.dispose();
        S2TestMethodRunner.s2junit4Path = S2TestMethodRunner.DEFAULT_S2JUNIT4_PATH;
    }

    static void seq(final String s) {
        seq.add(s);
        logger.debug("seq: {}", s);
    }

    // 追加分
    @RunWith(Seasar2.class)
    public static class ExtendsTestParent {

        @Test
        public void aaa() {
            seq("@Test-p");
        }

        @Before
        public void bbb1() {
            seq("@Before-p");
        }

        @After
        public void eee1() {
            seq("@After-p");
        }

        @PostBindFields
        public void ggg1() {
            seq("@PostBindFields-p");
        }

        @PreUnbindFields
        public void hhh1() {
            seq("@PreUnbindFields-p");
        }

        @BeforeClass
        public static void kkk1() {
            seq("@BeforeClass-p");
        }

        @AfterClass
        public static void lll1() {
            seq("@AfterClass-p");
        }

    }

    public static class ExtendsTest extends ExtendsTestParent {

        @Test
        public void aaa() {
            seq("@Test-c");
        }

        @Before
        public void bbb2() {
            seq("@Before-c");
        }

        @After
        public void eee2() {
            seq("@After-c");
        }

        @PostBindFields
        public void ggg2() {
            seq("@PostBindFields-c");
        }

        @PreUnbindFields
        public void hhh2() {
            seq("@PreUnbindFields-c");
        }

        @BeforeClass
        public static void kkk2() {
            seq("@BeforeClass-c");
        }

        @AfterClass
        public static void lll2() {
            seq("@AfterClass-c");
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
        assertThat(seq, is(Arrays.asList(
                "@BeforeClass-p", "@BeforeClass-c",
                "@Before-p", "@Before-c",
                "@PostBindFields-c", "@PostBindFields-p",
                "@Test-c",
                "@PreUnbindFields-c", "@PreUnbindFields-p",
                "@After-c", "@After-p",
                "@AfterClass-c", "@AfterClass-p")));
    }

}
