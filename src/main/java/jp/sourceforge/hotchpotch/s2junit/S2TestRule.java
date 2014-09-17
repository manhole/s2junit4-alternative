package jp.sourceforge.hotchpotch.s2junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author manhole
 */
public class S2TestRule implements TestRule {

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    S2TestRule.this.evaluate(base);
                } finally {
                    after();
                }
            }
        };
    }

    protected void evaluate(final Statement base) throws Throwable {
        base.evaluate();
    }

    protected void before() {
    }

    protected void after() {
    }

}
