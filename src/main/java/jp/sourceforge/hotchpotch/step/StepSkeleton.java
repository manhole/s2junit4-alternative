package jp.sourceforge.hotchpotch.step;

/**
 * @author manhole
 */
public abstract class StepSkeleton implements Step {

    public void execute(final Completion completion) throws Throwable {
        completion.complete();
    }

    @Override
    public void before() throws Throwable {
    }

    @Override
    public void after() {
    }

}
