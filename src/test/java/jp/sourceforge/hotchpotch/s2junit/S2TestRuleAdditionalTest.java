package jp.sourceforge.hotchpotch.s2junit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.seasar.framework.unit.annotation.PostBindFields;
import org.seasar.framework.unit.annotation.PreUnbindFields;

/**
 * @author manhole
 */
public class S2TestRuleAdditionalTest {

    private static String log;

    @Before
    public void setUp() {
        log = "";
    }

    @After
    public void tearDown() {
        S2TestRule.s2junit4Path = S2TestRule.DEFAULT_S2JUNIT4_PATH;
    }

    // 追加分
    public static class ExtendsTestParent {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

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

        @BeforeTest
        public void ooo1() {
            log += "o";
        }

        @AfterTest
        public void ppp1() {
            log += "p";
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

        @BeforeTest
        public void ooo2() {
            log += "O";
        }

        @AfterTest
        public void ppp2() {
            log += "P";
        }

    }

    // 追加分
    @Test
    public void testExtendsTestClass() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(ExtendsTest.class);
        S2TestRuleTest.printFailures(result.getFailures());
        assertTrue(result.wasSuccessful());
        /*
         * TestRuleの仕組みに乗るため、S2JUnit4とは@Before,@Afterの実行位置が変わる。
         * 代わりに@BeforeTest,@AfterTestが@Before,@Afterの位置で動作する。
         *
         * @BeforeTestと@PostBindFieldsを、@Before,@BeforeClassと同じように親クラスを先にする。
         * @AfterTestと@PreUnbindFieldsを、@After,@AfterClassと同じように子クラスを先にする。
         *
         */
        assertEquals("kKoOgGbBAEeHhPpLl", log);
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
