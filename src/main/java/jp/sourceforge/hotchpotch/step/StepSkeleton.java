package jp.sourceforge.hotchpotch.step;

/**
 * @author manhole
 */
public abstract class StepSkeleton implements Step {

    @Override
    public void step(final Completion completion) throws Throwable {
        before();
        try {
            completion.complete();
        } finally {
            after();
        }
    }

    abstract protected void before() throws Throwable;

    abstract protected void after();

}
