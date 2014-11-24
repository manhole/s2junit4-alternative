package jp.sourceforge.hotchpotch.step;

/**
 * @author manhole
 */
public interface Step {

    void step(Completion completion) throws Throwable;

}
