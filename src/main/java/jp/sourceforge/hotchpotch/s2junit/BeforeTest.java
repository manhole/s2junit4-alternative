package jp.sourceforge.hotchpotch.s2junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * S2JUnit4での before(), beforeXxxx() メソッドの代わり。
 * TestContext作成〜S2Container初期化の間に実行されます。
 *
 * TestContextをセットアップするタイミングとして利用する想定です。
 *
 * すべての@Testに対して仕掛けたいときは @BeforeTest と、
 * 特定の@Testに対して仕掛けたいときは  @BeforeTest("xxxx") としてください。
 *
 * @author manhole
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BeforeTest {
    String value() default "";
}
