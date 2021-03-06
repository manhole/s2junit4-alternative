package jp.sourceforge.hotchpotch.s2junit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.ejb.EJB;
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
import org.seasar.extension.dataset.types.ColumnTypes;
import org.seasar.extension.unit.BeanReader;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.convention.NamingConvention;
import org.seasar.framework.env.Env;
import org.seasar.framework.mock.servlet.MockHttpServletRequest;
import org.seasar.framework.unit.Bar;
import org.seasar.framework.unit.DataAccessor;
import org.seasar.framework.unit.Foo;
import org.seasar.framework.unit.Hoge;
import org.seasar.framework.unit.Hoge2;
import org.seasar.framework.unit.IBar;
import org.seasar.framework.unit.IHoge;
import org.seasar.framework.unit.InternalTestContext;
import org.seasar.framework.unit.PreparationType;
import org.seasar.framework.unit.Seasar2;
import org.seasar.framework.unit.Seasar2Test;
import org.seasar.framework.unit.TestContext;
import org.seasar.framework.unit.annotation.PostBindFields;
import org.seasar.framework.unit.annotation.PreUnbindFields;
import org.seasar.framework.unit.annotation.RegisterNamingConvention;
import org.seasar.framework.unit.annotation.RootDicon;
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
        S2TestRule.s2junit4Path = S2TestRule.DEFAULT_S2JUNIT4_PATH;
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

        @BeforeTest
        public void iii() {
            log += "i";
        }

        @AfterTest
        public void jjj() {
            log += "j";
        }

        @BeforeTest("eee")
        public void kkk() {
            log += "k";
        }

        @AfterTest("eee")
        public void lll() {
            log += "l";
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
        assertEquals("aikgcedhljb", log);
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

    /*
     * メソッド命名規約はサポートしない。それそれJUnit4アノテーションを使うこと。
     * beforeClass -> @BeforeClass
     * afterClass -> @AfterClass
     * before -> @BeforeTest (@Beforeではない)
     * after -> @AfterTest (@Afterではない)
     * postBindFields -> @PostBindFields (結果として@Beforeが同じ状態になっている)
     * preUnbindFields -> @PreUnbindFields (結果として@Afterが同じ状態になっている)
     * その他 -> @Test
     * beforeXxxx -> @BeforeTest("xxxx")
     * afterXxxx -> @AfterTest("xxxx")
     *
     */
    public static class ConventionTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private Seasar2Test.Hello hello;

        public static void beforeClass() {
            log += "a";
        }

        public static void afterClass() {
            log += "b";
        }

        public void before() {
            log += "c";
        }

        public void after() {
            log += "d";
        }

        @Test
        public void aaa() {
            log += "e";
        }

        @Ignore
        public void bbb() {
            log = "f";
        }

        public void postBindFields() {
            assertNotNull(hello);
            log += "g";
        }

        public void preUnbindFields() {
            assertNotNull(hello);
            log += "h";
        }
    }

    @Test
    public void testConventionTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(ConventionTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        //命名規約は無効。アノテーションを付けていないのでbefore()などは呼ばれない。
        //assertEquals("acgehdb", log);
        assertEquals("e", log);
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

        @BeforeTest("aaa")
        public void beforeAaa() {
            log += "a";
        }

        @Test
        public void aaa() {
            log += "b";
        }

        @AfterTest("aaa")
        public void afterAaa() {
            log += "c";
        }
    }

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

        @BeforeTest("aaa")
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

        @AfterTest("aaa")
        public void afterAaa() {
            set();
        }

        private void set() {
            values.add(container);
            values.add(internalContext);
            values.add(context);
        }
    }

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

        @BeforeTest("none")
        public void beforeNone() {
            context.setPreparationType(PreparationType.NONE);
        }

        @Test
        public void none() {
            int size = da.readDbByTable("EMP").getRowSize();
            assertEquals(14, size);
            log += "a";
        }

        @BeforeTest("write")
        public void beforeWrite() {
            context.setPreparationType(PreparationType.WRITE);
        }

        @Test
        public void write() {
            int size = da.readDbByTable("EMP").getRowSize();
            assertEquals(16, size);
            log += "b";
        }

        @BeforeTest("replace")
        public void beforeReplace() {
            context.setPreparationType(PreparationType.REPLACE);
        }

        @Test
        public void replace() {
            int size = da.readDbByTable("EMP").getRowSize();
            assertEquals(15, size);
            log += "c";
        }

        @BeforeTest("allReplace")
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

    public static class TrimStringTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private TestContext context;

        private DataAccessor accessor;

        @BeforeTest
        public void before() {
            context.setTrimString(false);
        }

        @Test
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

    @Test
    public void testTrimStringTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(TrimStringTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, count);
    }

    public static class TrimStringBeanReaderTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private TestContext context;

        @BeforeTest("aaa")
        public void beforeAaa() {
            context.setTrimString(true);
        }

        @Test
        public void aaa() {
            DataSet dataSet = new BeanReader(new Emp("hoge   ")).read();
            String actual = (String) dataSet.getTable(0).getRow(0).getValue(0);
            assertEquals("hoge", actual);
            log += "a";
        }

        @BeforeTest("bbb")
        public void beforeBbb() {
            context.setTrimString(false);
        }

        @Test
        public void bbb() {
            DataSet dataSet = new BeanReader(new Emp("hoge   ")).read();
            Object actual = dataSet.getTable(0).getRow(0).getValue(0);
            assertEquals("hoge   ", actual);
            log += "b";
        }

        @BeforeTest("ccc")
        public void beforeCcc() {
            context.setTrimString(false);

            assertEquals(ColumnTypes.STRING, ColumnTypes
                    .getColumnType(String.class));
            assertEquals(ColumnTypes.STRING, ColumnTypes
                    .getColumnType(Types.CHAR));
            assertEquals(ColumnTypes.STRING, ColumnTypes
                    .getColumnType(Types.LONGVARCHAR));
            assertEquals(ColumnTypes.STRING, ColumnTypes
                    .getColumnType(Types.VARCHAR));
            log += "c";
        }

        @Test
        public void ccc() {
            assertEquals(ColumnTypes.NOT_TRIM_STRING, ColumnTypes
                    .getColumnType(String.class));
            assertEquals(ColumnTypes.NOT_TRIM_STRING, ColumnTypes
                    .getColumnType(Types.CHAR));
            assertEquals(ColumnTypes.NOT_TRIM_STRING, ColumnTypes
                    .getColumnType(Types.LONGVARCHAR));
            assertEquals(ColumnTypes.NOT_TRIM_STRING, ColumnTypes
                    .getColumnType(Types.VARCHAR));
            log += "d";
        }

        @AfterTest("ccc")
        public void afterCcc() {
            assertEquals(ColumnTypes.STRING, ColumnTypes
                    .getColumnType(String.class));
            assertEquals(ColumnTypes.STRING, ColumnTypes
                    .getColumnType(Types.CHAR));
            assertEquals(ColumnTypes.STRING, ColumnTypes
                    .getColumnType(Types.LONGVARCHAR));
            assertEquals(ColumnTypes.STRING, ColumnTypes
                    .getColumnType(Types.VARCHAR));
            log += "e";
        }
    }

    public static class Emp {

        private String name;

        public Emp(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    public void testTrimStringBeanReaderTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(TrimStringBeanReaderTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertTrue(log.contains("a"));
        assertTrue(log.contains("b"));
        assertTrue(log.contains("c"));
        assertTrue(log.contains("d"));
        assertTrue(log.contains("e"));
    }

    public static class NonAutoPreparingTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        static int aaa_size;

        static int bbb_size;

        static int ccc_size;

        private TestContext ctx;

        private DataAccessor da;

        @BeforeTest
        @SuppressWarnings("deprecation")
        public void before() {
            ctx.setAutoPreparing(false);
        }

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
    public void testNonAutoPreparingTest() {
        NonAutoPreparingTest.aaa_size = 0;
        NonAutoPreparingTest.bbb_size = 0;
        NonAutoPreparingTest.ccc_size = 0;
        JUnitCore core = new JUnitCore();
        Result result = core.run(NonAutoPreparingTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(14, NonAutoPreparingTest.aaa_size);
        assertEquals(14, NonAutoPreparingTest.bbb_size);
        assertEquals(14, NonAutoPreparingTest.ccc_size);
    }

    public static class AutoIncludingTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private S2Container container;

        @Test
        public void aaa() {
            log += container.getComponent("hoge").toString();
        }
    }

    @Test
    public void testAutoIncludingTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(AutoIncludingTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals("aaa", log);
    }

    @TxBehavior(TxBehaviorType.NONE)
    public static class NonAutoIncludingTest {

        // RuleはfieldだけでなくメソッドでJUnit4へ返すことも可能。
        @Rule
        public S2TestRule createRule() {
            final S2TestRule rule = S2TestRule.create(this);
            return rule;
        }

        private TestContext ctx;

        private S2Container container;

        /*
         * NOTE: beforeのタイミングが後ろに変わったため、ここでsetAutoIncludingしても遅い。
         *
         * S2JUnit4(S2TestMethodRunner)では、@Before/before() → initContainerという順序だったが、
         * TestRuleベースの実装に変えたことで initContainer → @Before という順序に変わった。
         * setAutoIncludingはinitContainerの時点で必要。
         *
         * → initContainerの前でTestContextをセットアップするタイミングが欲しかったので、before() 機能を
         * 復活させた。
         * これで before() → initContainer → @Before という処理順になる。
         *
         * TODO: before()に@Beforeが付いていたらエラーにするのが良いかな。
         */
        @BeforeTest
        public void before() {
            ctx.setAutoIncluding(false);
        }

        @Test
        public void aaa() {
            log += container.hasComponentDef("hoge");
            log += "-";
            log += container.hasComponentDef(TransactionManager.class);
        }

    }

    @Test
    public void testNonAutoIncludingTest() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(NonAutoIncludingTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals("false-false", log);
    }

    public static class GetExpectedTest {

        private TestContext ctx;

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        @Test
        public void aaa() {
            log += (ctx.getExpected() != null);
            log += "-";
        }

        @Test
        public void bbb() {
            log += (ctx.getExpected() == null);
            log += "-";
        }
    }

    @Test
    public void testGetExpected() throws Exception {
        JUnitCore core = new JUnitCore();
        Result result = core.run(GetExpectedTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals("true-true-", log);
    }

    public static class BindEJB3ByFieldNameTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        TestContext testContext;

        @EJB
        private IHoge hoge;

        /*
         * S2JUnit4では呼ばれていたが、
         * alternativeでは呼ばれない。
         */
        @BeforeTest
        public void before() {
            testContext.register(Hoge.class);
            testContext.register(Foo.class);
        }

        @Test
        public void aaa() {
            log += (hoge != null) + "-";
            log += (hoge.aaa() != null);
        }
    }

    @Test
    public void testBindEJB3ByFieldName() throws Exception {
        JUnitCore core = new JUnitCore();
        Result result = core.run(BindEJB3ByFieldNameTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, result.getRunCount());
        assertEquals("true-true", log);
    }

    public static class BindEJB3ByBeanNameTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        TestContext testContext;

        @EJB(beanName = "xxx")
        private IHoge yyy;

        @BeforeTest
        public void before() {
            testContext.register(Hoge2.class);
        }

        @Test
        public void aaa() {
            log += (yyy != null);
        }

    }

    @Test
    public void testBindEJB3ByBeanNameTest() throws Exception {
        JUnitCore core = new JUnitCore();
        Result result = core.run(BindEJB3ByBeanNameTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, result.getRunCount());
        assertEquals("true", log);
    }

    public static class BindEJB3ByTypeTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        TestContext testContext;

        @EJB
        private IBar zzz;

        @BeforeTest
        public void before() {
            testContext.register(Bar.class);
        }

        @Test
        public void aaa() {
            log += (zzz != null);
        }
    }

    @Test
    public void testBindEJB3ByType() throws Exception {
        JUnitCore core = new JUnitCore();
        Result result = core.run(BindEJB3ByTypeTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, result.getRunCount());
        assertEquals("true", log);
    }

    // TODO Prerequisiteテスト群

    public static class EnvTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        @Test
        public void env() {
            log += Env.getFilePath();
            log += Env.getValue();
        }
    }

    @Test
    public void testEnv() throws Exception {
        JUnitCore core = new JUnitCore();
        Result result = core.run(EnvTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals("env_ut.txtut", log);
    }


    // TODO WarmDeployテスト群


    // TODO Mockテスト群


    @RootDicon("aop.dicon")
    public static class RootDiconTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private S2Container container;

        @Test
        public void aaa() {
            assertEquals(container.getRoot(), container);
            assertEquals("aop.dicon", container.getPath());
            log += "a";
        }

        @RootDicon("jdbc.dicon")
        @Test
        public void bbb() {
            assertEquals(container.getRoot(), container);
            assertEquals("jdbc.dicon", container.getPath());
            log += "b";
        }
    }

    @Test
    public void testRootDicon() throws Exception {
        JUnitCore core = new JUnitCore();
        Result result = core.run(RootDiconTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertTrue(log, log.contains("a"));
        assertTrue(log, log.contains("b"));
    }

    public static class SimpleInternalTestContextTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private S2Container container;

        @Test
        public void aaa() {
            assertNotNull(container);
            count++;
        }
    }

    @Test
    public void testSimpleInternalTestContext() throws Exception {
        S2TestRule.s2junit4Path = SimpleInternalTestContextTest.class
                .getName().replace(".", "/")
                + ".s2junit4.dicon";
        JUnitCore core = new JUnitCore();
        Result result = core.run(SimpleInternalTestContextTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, count);
    }

    public static class NamingConventionTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private TestContext context;

        private NamingConvention namingConvention;

        @Test
        @RootDicon("jp/sourceforge/hotchpotch/s2junit/S2TestRuleTest_convention.dicon")
        public void aaa() {
            assertEquals("/", namingConvention.getViewRootPath());
            assertEquals(".htm", namingConvention.getViewExtension());
            log += "a";
        }

        @BeforeTest("bbb")
        public void beforeBbb() {
            context.setAutoIncluding(false);
        }

        @Test
        @RegisterNamingConvention(false)
        public void bbb() {
            assertNull(namingConvention);
            context.register(HashMap.class);
            log += "b";
        }
    }

    @Test
    public void testNamingConvention() throws Exception {
        JUnitCore core = new JUnitCore();
        Result result = core.run(NamingConventionTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(2, log.length());
        assertTrue(log.contains("a"));
        assertTrue(log.contains("b"));
    }

    public static class PublishedTestContextTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private Seasar2Test.MyTestContext context;

        @BeforeTest
        public void before() {
            log += context != null;
        }

        @Test
        public void aaa() {
        }
    }

    @Test
    public void testPublishedTestContext() throws Exception {
        S2TestRule.s2junit4Path = PublishedTestContextTest.class
                .getName().replace(".", "/")
                + ".s2junit4.dicon";
        JUnitCore core = new JUnitCore();
        Result result = core.run(PublishedTestContextTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals("true", log);
    }

    public static class MockHttpServletRequestTest {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        private MockHttpServletRequest request;

        @Test
        public void aaa() {
            assertNotNull(request);
            log += "a";
        }
    }

    @Test
    public void testMockHttpServletRequest() throws Exception {
        JUnitCore core = new JUnitCore();
        Result result = core.run(MockHttpServletRequestTest.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals("a", log);
    }


    // TODO configure,Seasar2をカスタマイズするテスト群

    public static class CustomizeSeasar2Test {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

        public void aaa() {
            log += "a";
            count++;
        }

        public void beforeBbb() {
            log += "b";
            count++;
        }

        /**
         *
         */
        @Test
        public void bbb() {
            log += "c";
            count++;
        }
    }

    /*
     * Seasar2.delegate(Runner)を差し替えているが、
     * TestRuleベースではその差し替えはしないので、Ignoreする。
     *
     */
    @Ignore
    @Test
    public void testCustomizeSeasar2Test() {
        configure("Seasar2.dicon");
        JUnitCore core = new JUnitCore();
        Result result = core.run(CustomizeSeasar2Test.class);
        printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        assertEquals(1, count);
        assertEquals("c", log);
    }

    // "s2junit4config.dicon" を差し替える用途かな?
    private void configure(String name) {
        String path = getClass().getName().replace('.', '/') + "." + name;
        Seasar2.configure(path);
    }

    public static void printFailures(List<Failure> failures) {
        for (final Failure failure : failures) {
            System.out.println(">>> failure >>>");
            System.out.println(failure.getTestHeader());
            System.out.println(failure.getMessage());
            System.out.println(failure.getTrace());
            System.out.println("<<< failure <<<");
        }
    }

}
