package jp.sourceforge.hotchpotch.s2junit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.junit.runners.Parameterized;
import org.seasar.extension.dataset.DataSet;
import org.seasar.extension.dataset.DataTable;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.unit.DataAccessor;
import org.seasar.framework.unit.InternalTestContext;
import org.seasar.framework.unit.PreparationType;
import org.seasar.framework.unit.Seasar2;
import org.seasar.framework.unit.Seasar2Test;
import org.seasar.framework.unit.TestContext;
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

    @TxBehavior(TxBehaviorType.COMMIT)
    public static class TransactionBehaviorNoneTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        TransactionManager tm;

        @Test
        @TxBehavior(TxBehaviorType.NONE)
        public void bbb() {
            count++;
            txActive = TransactionManagerUtil.isActive(tm);
        }
    }

    @Test
    public void testTransactionBehaviorNoneTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(TransactionBehaviorNoneTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, count);
        assertEquals(false, txActive);
    }

    @TxBehavior(TxBehaviorType.NONE)
    public static class TransactionBehaviorCommitTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        TransactionManager tm;

        @Test
        @TxBehavior(TxBehaviorType.COMMIT)
        public void bbb() {
            count++;
            txActive = TransactionManagerUtil.isActive(tm);
        }
    }

    @Test
    public void testTransactionBehaviorCommitTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(TransactionBehaviorCommitTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, count);
        assertEquals(true, txActive);
        // TODO commitすることをassertしたい
    }

    @TxBehavior(TxBehaviorType.NONE)
    public static class TransactionBehaviorRollbackTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        TransactionManager tm;

        @Test
        @TxBehavior(TxBehaviorType.ROLLBACK)
        public void bbb() {
            count++;
            txActive = TransactionManagerUtil.isActive(tm);
        }

    }

    @Test
    public void testTransactionBehaviorRollbackTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(TransactionBehaviorRollbackTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, count);
        assertEquals(true, txActive);
        // TODO rollbackすることをassertしたい
    }

    // S2JUnit4とは異なり、Parameterizedを使う場合はRunWithに指定する必要がある。
    @RunWith(Parameterized.class)
    public static class ParameterizedTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        /**
         * @return
         */
        @Parameterized.Parameters
        public static Collection<?> parameters() {
            return Arrays
                    .asList(new Object[][]{ { 1, 1 }, { 2, 4 }, { 3, 9 } });
        }

        private int a;

        private int b;

        public ParameterizedTest(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Test
        public void aaa() {
            count++;
            log += a;
            log += b;
        }
    }

    @Test
    public void testParameterizedTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(ParameterizedTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(3, count);
        assertEquals("112439", log);
    }

    @RunWith(Parameterized.class)
    public static class ParameterizedS2JUnitOrderTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        @BeforeClass
        public static void a() {
            log += "a";
        }

        @AfterClass
        public static void b() {
            log += "b";
        }

        @Before
        public void c() {
            log += "c";
        }

        @After
        public void d() {
            log += "d";
        }

        @Parameterized.Parameters
        public static Collection<?> parameters() {
            return Arrays
                    .asList(new Object[][]{ { 1, 1 }, { 2, 4 }, { 3, 9 } });
        }

        private int a;

        private int b;

        public ParameterizedS2JUnitOrderTest(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Test
        public void aaa() {
            log += String.format("(%s:%s,%s)", count, a, b);
            count++;
        }
    }

    @Test
    public void testParameterizedS2JUnitOrder() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(ParameterizedS2JUnitOrderTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(3, count);
        assertEquals("@RunWith(Parameterized.class)と同じ結果になるはず", "ac(0:1,1)dc(1:2,4)dc(2:3,9)db", log);
    }

    public static class EachBeforeAndEachAfterTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        public void beforeAaa() {
            log += "a";
        }

        @Test
        public void aaa() {
            log += "b";
        }

        public void afterAaa() {
            log += "c";
        }
    }

    // テスト命名規約でのテストメソッド判定はサポートしないことにする。
    // ただしbeforeXxx, afterXxxは、代替アノテーションが存在しないため移植対象とするかも。
    @Ignore
    @Test
    public void testEachBeforeAndEachAfterTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(EachBeforeAndEachAfterTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, result.getRunCount());
        assertEquals("abc", log);
    }

    public static class FieldBindingTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private static List<Object> values = new ArrayList<Object>();

        private S2Container container;

        private InternalTestContext internalContext;

        private TestContext context;

        public void beforeAaa() {
            set();
        }

        @Test
        public void aaa() {
            set();
            assertThat(container, is(notNullValue()));
            assertThat(internalContext, is(notNullValue()));
            assertThat(context, is(notNullValue()));
        }

        public void afterAaa() {
            set();
        }

        private void set() {
            values.add(container);
            values.add(internalContext);
            values.add(context);
        }
    }

    // テスト命名規約でのテストメソッド判定はサポートしないことにする。
    // ただしbeforeXxx, afterXxxは、代替アノテーションが存在しないため移植対象とするかも。
    @Ignore
    @Test
    public void testFieldBindingTest() {
        FieldBindingTest.values = new ArrayList<Object>();
        JUnitCore core = new JUnitCore();
        Result result = core.run(FieldBindingTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        List<Object> values = FieldBindingTest.values;
        assertEquals(9, values.size());
        assertNull(values.get(0));
        assertNull(values.get(1));
        assertNotNull(values.get(2));
        assertNotNull(values.get(3));
        assertNotNull(values.get(4));
        assertNotNull(values.get(5));
        assertNull(values.get(6));
        assertNull(values.get(7));
        assertNull(values.get(8));
    }

    public static class AutoPreparingTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        static int aaa_size;

        static int bbb_size;

        static int ccc_size;

        DataAccessor da;

        @Test
        public void aaa() {
            aaa_size = da.readDbByTable("EMP").getRowSize();
        }

        @Test
        public void bbb() {
            bbb_size = da.readDbByTable("EMP").getRowSize();
        }

        @Test
        public void ccc() {
            ccc_size = da.readDbByTable("EMP").getRowSize();
        }
    }

    @Test
    public void testAutoPreparingTest() {
        AutoPreparingTest.aaa_size = 0;
        AutoPreparingTest.bbb_size = 0;
        AutoPreparingTest.ccc_size = 0;
        JUnitCore core = new JUnitCore();
        Result result = core.run(AutoPreparingTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(15, AutoPreparingTest.aaa_size);
        assertEquals(16, AutoPreparingTest.bbb_size);
        assertEquals(16, AutoPreparingTest.ccc_size);
    }

    public static class PreparationTypeTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        TestContext context;

        DataAccessor da;

        @Test
        public void defaultSetting() {
            int size = da.readDbByTable("EMP").getRowSize();
            assertEquals(16, size);
        }

        public void beforeNone() {
            context.setPreparationType(PreparationType.NONE);
        }

        @Test
        public void none() {
            int size = da.readDbByTable("EMP").getRowSize();
            assertEquals(14, size);
            log += "a";
        }

        public void beforeWrite() {
            context.setPreparationType(PreparationType.WRITE);
        }

        @Test
        public void write() {
            int size = da.readDbByTable("EMP").getRowSize();
            assertEquals(16, size);
            log += "b";
        }

        public void beforeReplace() {
            context.setPreparationType(PreparationType.REPLACE);
        }

        @Test
        public void replace() {
            int size = da.readDbByTable("EMP").getRowSize();
            assertEquals(15, size);
            log += "c";
        }

        public void beforeAllReplace() {
            context.setPreparationType(PreparationType.ALL_REPLACE);
        }

        @Test
        public void allReplace() {
            int size = da.readDbByTable("EMP").getRowSize();
            assertEquals(2, size);
            log += "d";
        }

    }

    // テスト命名規約でのテストメソッド判定はサポートしないことにする。
    // ただしbeforeXxx, afterXxxは、代替アノテーションが存在しないため移植対象とするかも。
    @Ignore
    @Test
    public void testPreparationTypeTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(PreparationTypeTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertTrue(log.contains("a"));
        assertTrue(log.contains("b"));
        assertTrue(log.contains("c"));
        assertTrue(log.contains("d"));
    }

    /**
     *
     */
    @RunWith(Seasar2.class)
    public static class TrimStringTest {

        private TestContext context;

        private DataAccessor accessor;

        /**
         *
         */
        public void before() {
            context.setTrimString(false);
        }

        /**
         *
         */
        public void aaa() {
            DataTable table = accessor.readDbByTable("EMP", "ENAME = ' '");
            assertEquals(1, table.getRowSize());

            DataSet dataSet = context.getExpected();
            String value = (String) dataSet.getTable("EMP").getRow(0).getValue(
                    "ENAME");
            assertEquals(" ", value);

            count++;
        }
    }

    /**
     *
     */
    public void testTrimStringTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(TrimStringTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, count);
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

    @Test
    public void javaMethodName() {
        assertThat(Character.isJavaIdentifierPart('a'), is(true));
        assertThat(Character.isJavaIdentifierPart('2'), is(true));
        assertThat(Character.isJavaIdentifierPart('['), is(false));
        assertThat(Character.isJavaIdentifierPart(']'), is(false));

        final S2TestRule o = new S2TestRule("dummy");
        assertThat(o.javaMethodName("aaa"), is("aaa"));
        assertThat(o.javaMethodName("aaa[0]"), is("aaa"));
    }

}
