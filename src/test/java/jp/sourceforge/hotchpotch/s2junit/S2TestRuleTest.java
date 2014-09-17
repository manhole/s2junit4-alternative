package jp.sourceforge.hotchpotch.s2junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Comparator;
import java.util.List;

import javax.transaction.TransactionManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.seasar.framework.unit.Seasar2;
import org.seasar.framework.unit.Seasar2Test;
import org.seasar.framework.unit.annotation.PostBindFields;
import org.seasar.framework.unit.annotation.PreUnbindFields;
import org.seasar.framework.unit.annotation.TxBehavior;
import org.seasar.framework.unit.annotation.TxBehaviorType;
import org.seasar.framework.util.TransactionManagerUtil;

/**
 * @author manhole
 */
public class S2TestRuleTest {

    private static String log;

    private static int count;

    private static boolean txActive;

    @Before
    public void setUp() {
        log = "";
        count = 0;
        txActive = false;
        //Seasar2.configure();
    }

    @After
    public void tearDown() {
        //Seasar2.dispose();
        // TODO packageが異なるので呼べない...
        //S2TestMethodRunner.s2junit4Path = S2TestMethodRunner.DEFAULT_S2JUNIT4_PATH;
    }

    public static class FilterTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        // S2JUnitでの命名規約によるテストメソッド認識はサポートしない。
        // @Testを付けること。
        @Test
        public void aaa() {
            log += "a";
        }

        @Test
        public void bbb() {
            log += "b";
        }

    }

    @Test
    public void testFilter() throws Exception {
        JUnitCore core = new JUnitCore();
        Result result = core.run(Request.method(FilterTest.class, "bbb"));
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertFalse(log.contains("a"));
        assertTrue(log.contains("b"));
    }

    public static class SortTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        @Test
        public void aaa() {
            log += "a";
        }

        @Test
        public void bbb() {
            log += "b";
        }

        @Test
        public void ccc() {
            log += "c";
        }

    }

    @Test
    public void testSort() throws Exception {
        JUnitCore core = new JUnitCore();
        Request req = Request.aClass(SortTest.class).sortWith(
                new Comparator<Description>() {

                    public int compare(Description o1, Description o2) {
                        return -1
                                * (o1.getDisplayName().compareTo(o2
                                .getDisplayName()));
                    }
                });
        Result result = core.run(req);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals("cba", log);
    }

    public static class AnnotationTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private Seasar2Test.Hello hello;

        @BeforeClass
        public static void aaa() {
            log += "a";
        }

        @AfterClass
        public static void bbb() {
            log += "b";
        }

        @Before
        public void ccc() {
            log += "c";
        }

        @After
        public void ddd() {
            log += "d";
        }

        @Test
        public void eee() {
            log += "e";
        }

        @Ignore
        @Test
        public void fff() {
            log = "f";
        }

        @PostBindFields
        public void ggg() {
            assertNotNull(hello);
            log += "g";
        }

        @PreUnbindFields
        public void hhh() {
            assertNotNull(hello);
            log += "h";
        }
    }

    @Test
    public void testAnnotationTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(AnnotationTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        /*
         * TestRuleで実装すると、この順序で実行されるようだ。
         * @PostBindFields → @Before → @Test → @After → @PreUnbindFields
         *
         * S2JUnit4では:
         * @Before → @PostBindFields → @Test → @PreUnbindFields → @After
         */
        //assertEquals("acgehdb", log);
        assertEquals("agcedhb", log);
    }

    @Ignore
    public static class IgnoreAnnotationForClassTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        @Test
        public void aaa() {
            log += "a";
        }
    }

    @Test
    public void testIgnoreAnnotationForClassTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(IgnoreAnnotationForClassTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, result.getIgnoreCount());
        assertEquals(0, log.length());
    }

    public static class ConventionTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private Seasar2Test.Hello hello;

        /**
         *
         */
        public static void beforeClass() {
            log += "a";
        }

        /**
         *
         */
        public static void afterClass() {
            log += "b";
        }

        /**
         *
         */
        public void before() {
            log += "c";
        }

        /**
         *
         */
        public void after() {
            log += "d";
        }

        /**
         *
         */
        public void aaa() {
            log += "e";
        }

        /**
         *
         */
        @Ignore
        public void bbb() {
            log = "f";
        }

        /**
         *
         */
        public void postBindFields() {
            assertNotNull(hello);
            log += "g";
        }

        /**
         *
         */
        public void preUnbindFields() {
            assertNotNull(hello);
            log += "h";
        }
    }

    @Ignore
    @Test
    public void testConventionTest() {
        // サポートしないのでIgnoreする
        assumeTrue(false);
        JUnitCore core = new JUnitCore();
        Result result = core.run(ConventionTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals("acgehdb", log);
    }

    public static class InvalidMethodsTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        public void aaa() {
            log += "a";
        }

        public static void bbb() {
            log += "b";
        }

        @SuppressWarnings("unused")
        private void ccc() {
            log += "c";
        }

        public String ddd() {
            log += "d";
            return null;
        }

        public void eee(@SuppressWarnings("unused") String a) {
            log += "e";
        }

        @SuppressWarnings("unused")
        @Ignore
        private void fff() {
            log += "f";
        }
    }

    /*
     * 命名規約でのテストメソッド認識をテストしているのでIgnore
     */
    @Ignore
    @Test
    public void testInvalidMethodsTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(InvalidMethodsTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals("a", log);
    }

    public static class TransactionBehaviorDefaultTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        TransactionManager tm;

        @Test
        public void bbb() {
            count++;
            txActive = TransactionManagerUtil.isActive(tm);
        }
    }

    @Test
    public void testTransactionBehaviorDefaultTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(TransactionBehaviorDefaultTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, count);
        assertEquals(true, txActive);
    }

    @RunWith(Seasar2.class)
    @TxBehavior(TxBehaviorType.COMMIT)
    public static class TransactionBehaviorNoneTest {

        TransactionManager tm;

        /**
         *
         */
        @TxBehavior(TxBehaviorType.NONE)
        public void bbb() {
            count++;
            txActive = TransactionManagerUtil.isActive(tm);
        }
    }

    /**
     *
     */
    public void testTransactionBehaviorNoneTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(TransactionBehaviorNoneTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, count);
        assertEquals(false, txActive);
    }

    private void printFailures(List<Failure> failures) {
        for (final Failure failure : failures) {
            System.out.println(">>> failure >>>");
            System.out.println(failure.getTestHeader());
            System.out.println(failure.getMessage());
            System.out.println(failure.getTrace());
            System.out.println("<<< failure <<<");
        }
    }

}
