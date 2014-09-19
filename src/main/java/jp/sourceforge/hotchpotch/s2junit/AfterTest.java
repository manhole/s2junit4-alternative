package jp.sourceforge.hotchpotch.s2junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * S2JUnit4での after(), afterXxxx() メソッドの代わり。
 * S2Container破棄〜TestContext破棄の間に実行されます。
 *
 * すべての@Testに対して仕掛けたいときは @AfterTest と、
 * 特定の@Testに対して仕掛けたいときは  @AfterTest("xxxx") としてください。
 *
 * @author manhole
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterTest {
    String value() default "";
}
