package jp.sourceforge.hotchpotch.step;

/**
 * @author manhole
 */
public abstract class StepSkeleton implements Step {

    @Override
    public void step(final Completion completion) throws Throwable {
        before();
        try {
            execute(completion);
        } finally {
            after();
        }
    }

    protected void execute(final Completion completion) throws Throwable {
        completion.complete();
    }

    abstract protected void before() throws Throwable;

    abstract protected void after();

}
