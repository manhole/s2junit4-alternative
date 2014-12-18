package jp.sourceforge.hotchpotch.s2junit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author manhole
 */
public class S2TestRuleAdditionalTest {

    private static final Logger logger = LoggerFactory.getLogger(S2TestRuleAdditionalTest.class);
    private static List<String> seq = new ArrayList<String>();

    @Before
    public void setUp() {
        seq.clear();
    }

    @After
    public void tearDown() {
        S2TestRule.s2junit4Path = S2TestRule.DEFAULT_S2JUNIT4_PATH;
    }

    static void seq(final String s) {
        seq.add(s);
        logger.debug("seq: {}", s);
    }

    // 追加分
    public static class ExtendsTestParent {

        @Rule
        public S2TestRule testRule = S2TestRule.create(this);

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

        @BeforeTest
        public void ooo1() {
            seq("@BeforeTest-p");
        }

        @AfterTest
        public void ppp1() {
            seq("@AfterTest-p");
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

        @BeforeTest
        public void ooo2() {
            seq("@BeforeTest-c");
        }

        @AfterTest
        public void ppp2() {
            seq("@AfterTest-c");
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
         * S2JUnit4と異なる点1:
         * TestRuleの仕組みに乗るため、S2JUnit4とは@Before,@Afterの実行位置が変わる。
         * 代わりに@BeforeTest,@AfterTestが 従来の@Before,@Afterの位置で動作する。
         *
         * S2JUnit4と異なる点2:
         * @BeforeTestと@PostBindFieldsを、@Before,@BeforeClassと同じように親クラスを先にする。
         * @AfterTestと@PreUnbindFieldsを、@After,@AfterClassと同じように子クラスを先にする。
         *
         */
        assertThat(seq, is(Arrays.asList(
                "@BeforeClass-p", "@BeforeClass-c",
                "@BeforeTest-p", "@BeforeTest-c",
                "@PostBindFields-p", "@PostBindFields-c",
                "@Before-p", "@Before-c",
                "@Test-c",
                "@After-c", "@After-p",
                "@PreUnbindFields-c", "@PreUnbindFields-p",
                "@AfterTest-c", "@AfterTest-p",
                "@AfterClass-c", "@AfterClass-p")));
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
