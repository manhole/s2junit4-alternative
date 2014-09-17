package jp.sourceforge.hotchpotch.s2junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.seasar.framework.unit.S2TestMethodRunner;
import org.seasar.framework.unit.Seasar2;

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
        Seasar2.configure();
    }

    @After
    public void tearDown() {
        Seasar2.dispose();
        // TODO packageが異なるので呼べない...
        //S2TestMethodRunner.s2junit4Path = S2TestMethodRunner.DEFAULT_S2JUNIT4_PATH;
    }

    public static class FilterTest {

        @Rule
        public S2TestRule testRule = new S2TestRule();

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

    /**
     *
     */
    @RunWith(Seasar2.class)
    public static class SortTest {

        /**
         *
         */
        public void aaa() {
            log += "a";
        }

        /**
         *
         */
        public void bbb() {
            log += "b";
        }

        /**
         *
         */
        public void ccc() {
            log += "c";
        }

    }

    /**
     * @throws Exception
     */
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
