package jp.sourceforge.hotchpotch.step;

/**
 * @author manhole
 */
public interface Step {

    void execute(final Completion completion) throws Throwable;

    void before() throws Throwable;

    void after();

}
