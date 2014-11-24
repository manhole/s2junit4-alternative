package jp.sourceforge.hotchpotch.step;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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


}
