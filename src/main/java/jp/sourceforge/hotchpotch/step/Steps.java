package jp.sourceforge.hotchpotch.step;

import java.util.ArrayList;
import java.util.List;

/**
 * @author manhole
 */
public class Steps {

    private List<Step> steps = new ArrayList<Step>();

    public void enqueue(final Step step) {
        steps.add(step);
    }

    public void run() throws Throwable {
        final DefaultCompletion completion = new DefaultCompletion();
        final List<Step> dones = new ArrayList<Step>();
        Throwable ex = null;
        try {
            for (final Step step : steps) {
                step.before();
                dones.add(step);

                step.execute(completion);
                if (completion.called) {
                    completion.called = false;
                } else {
                    break;
                }

            }
        } catch (final Throwable e) {
            ex = e;
        } finally {
            final int size = dones.size();
            for (int i = size - 1; 0 <= i; i--) {
                final Step step = dones.get(i);
                try {
                    step.after();
                } catch (final Throwable e) {
                    ex = e;
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    static class DefaultCompletion implements Completion {

        boolean called;

        @Override
        public void complete() throws Throwable {
            called = true;
        }
    }

}
