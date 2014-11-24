package jp.sourceforge.hotchpotch.step;

import java.util.ArrayList;
import java.util.List;

/**
 * @author manhole
 */
public class Steps {

    private List<Step> steps = new ArrayList<Step>();
    private int position = -1;
    private final Completion completion = new Completion() {
        @Override
        public void complete() throws Throwable {
            nextStep();
        }
    };

    public void enqueue(final Step step) {
        steps.add(step);
    }

    public void run() throws Throwable {
        nextStep();
    }

    protected void nextStep() throws Throwable {
        position++;
        if (position < steps.size()) {
            final Step step = steps.get(position);
            step.step(completion);
        }
    }

}
