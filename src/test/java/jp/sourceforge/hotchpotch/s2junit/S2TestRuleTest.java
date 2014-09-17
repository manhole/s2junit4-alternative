package jp.sourceforge.hotchpotch.s2junit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
