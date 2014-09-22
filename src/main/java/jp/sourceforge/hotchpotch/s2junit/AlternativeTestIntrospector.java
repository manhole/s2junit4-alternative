package jp.sourceforge.hotchpotch.s2junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.seasar.framework.unit.annotation.PostBindFields;
import org.seasar.framework.unit.impl.AnnotationTestIntrospector;

/**
 * @author manhole
 */
public class AlternativeTestIntrospector extends AnnotationTestIntrospector {

    public AlternativeTestIntrospector() {
        /*
         * S2TestRuleからは、@Before,@Afterを実行しないようにする。
         * @Before,@AfterはJUnitから呼ばれるので、S2TestRuleからも呼んでしまうと2回実行されてしまうため。
         *
         * S2TestRuleでは、@Before,@After の代わりに @BeforeTest,@AfterTest を使用する。
         */
        setBeforeAnnotation(BeforeTest.class);
        setAfterAnnotation(AfterTest.class);
    }

    @Override
    public List<Method> getBeforeMethods(final Class<?> clazz) {
        final List<Method> methods = super.getBeforeMethods(clazz);
        for (final Iterator<Method> it = methods.iterator(); it.hasNext(); ) {
            final Method m = it.next();
            final BeforeTest ann = m.getAnnotation(BeforeTest.class);
            if (!ann.value().equals("")) {
                it.remove();
            }
        }
        return methods;
    }

    @Override
    public List<Method> getAfterMethods(final Class<?> clazz) {
        final List<Method> methods = super.getAfterMethods(clazz);
        for (final Iterator<Method> it = methods.iterator(); it.hasNext(); ) {
            final Method m = it.next();
            final AfterTest ann = m.getAnnotation(AfterTest.class);
            if (!ann.value().equals("")) {
                it.remove();
            }
        }
        return methods;
    }

    public Method getEachBeforeMethod(final Class<?> clazz, final Method method) {
        final List<Method> annotatedMethods = getAnnotatedMethods(clazz, BeforeTest.class);
        Method candidate = null;
        for (final Method m : annotatedMethods) {
            BeforeTest ann = m.getAnnotation(BeforeTest.class);
            final String methodName = method.getName();
            if (ann.value().equals(methodName)) {
                if (candidate != null) {
                    // 1つだけ許可する
                    // TODO エラー内容
                    throw new AssertionError(String.format("only 1 @BeforeTest(\"%s\") is allowed", methodName));
                }
                candidate = m;
            }
        }
        return candidate;
    }

    public Method getEachAfterMethod(final Class<?> clazz, final Method method) {
        final List<Method> annotatedMethods = getAnnotatedMethods(clazz, AfterTest.class);
        Method candidate = null;
        for (final Method m : annotatedMethods) {
            AfterTest ann = m.getAnnotation(AfterTest.class);
            final String methodName = method.getName();
            if (ann.value().equals(methodName)) {
                if (candidate != null) {
                    // 1つだけ許可する
                    // TODO エラー内容
                    throw new AssertionError(String.format("only 1 @AfterTest(\"%s\") is allowed", methodName));
                }
                candidate = m;
            }
        }
        return candidate;
    }

    protected boolean runsTopToBottom(Class<? extends Annotation> annotation) {
        return super.runsTopToBottom(annotation) || annotation.equals(BeforeTest.class)
                || annotation.equals(PostBindFields.class);
    }

}
