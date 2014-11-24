package jp.sourceforge.hotchpotch.step;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author manhole
 */
public class StepsTest {

    /**
     * 基本形
     */
    @Test
    public void test1() throws Throwable {
        // ## Arrange ##
        final Steps steps = new Steps();
        final List<String> seq = new ArrayList<String>();

        // ## Act ##
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("1");
            }

            @Override
            protected void after() {
                seq.add("4");
            }
        });
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("2");
            }

            @Override
            protected void after() {
                seq.add("3");
            }
        });
        steps.run();

        // ## Assert ##
        assertThat(seq, is(Arrays.asList("1", "2", "3", "4")));
    }

    /**
     * Completionを呼ばない場合は、次のStepへ進まないこと。
     */
    @Test
    public void test_noCompletion() throws Throwable {
        // ## Arrange ##
        final Steps steps = new Steps();
        final List<String> seq = new ArrayList<String>();

        // ## Act ##
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("1");
            }

            @Override
            protected void after() {
                seq.add("5");
            }
        });
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("2");
            }

            @Override
            protected void execute(final Completion completion) throws Throwable {
                seq.add("3");
            }

            @Override
            protected void after() {
                seq.add("4");
            }
        });
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("X");
            }

            @Override
            protected void after() {
                seq.add("X");
            }
        });
        steps.run();

        // ## Assert ##
        assertThat(seq, is(Arrays.asList("1", "2", "3", "4", "5")));
    }

    /**
     * 例外が発生したら、先のStepへ進まないこと。
     * またafterを必ず呼ぶこと。
     */
    @Test
    public void test_exception1() throws Throwable {
        // ## Arrange ##
        final Steps steps = new Steps();
        final List<String> seq = new ArrayList<String>();

        // ## Act ##
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("1");
            }

            @Override
            protected void after() {
                seq.add("6");
            }
        });
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("2");
            }

            @Override
            protected void after() {
                seq.add("5");
            }
        });
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("3");
            }

            @Override
            protected void execute(final Completion completion) throws Throwable {
                throw new RuntimeException("e1");
            }

            @Override
            protected void after() {
                seq.add("4");
            }
        });
        steps.enqueue(new StepSkeleton() {
            @Override
            public void before() throws Throwable {
                seq.add("X");
            }

            @Override
            public void after() {
                seq.add("X");
            }
        });
        try {
            steps.run();
            fail();
        } catch (final RuntimeException e) {
            assertThat(e.getMessage(), is("e1"));
        }

        // ## Assert ##
        assertThat(seq, is(Arrays.asList("1", "2", "3", "4", "5", "6")));
    }

    /**
     * finally処理で例外が出ると、前の例外は失われてしまう。
     * afterは必ず呼ぶこと。
     */
    @Test
    public void test_exception2() throws Throwable {
        // ## Arrange ##
        final Steps steps = new Steps();
        final List<String> seq = new ArrayList<String>();

        // ## Act ##
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("1");
            }

            @Override
            protected void after() {
                seq.add("6");
                throw new RuntimeException("e4");
            }
        });
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("2");
            }

            @Override
            protected void after() {
                seq.add("5");
                throw new RuntimeException("e3");
            }
        });
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                seq.add("3");
            }

            @Override
            protected void execute(final Completion completion) throws Throwable {
                throw new RuntimeException("e1");
            }

            @Override
            protected void after() {
                seq.add("4");
                throw new RuntimeException("e2");
            }
        });

        try {
            steps.run();
            fail();
        } catch (final RuntimeException e) {
            assertThat(e.getMessage(), is("e4"));
        }

        // ## Assert ##
        assertThat(seq, is(Arrays.asList("1", "2", "3", "4", "5", "6")));
    }

}
