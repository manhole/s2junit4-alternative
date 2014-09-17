package jp.sourceforge.hotchpotch.s2junit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.Statement;
import org.seasar.framework.beans.BeanDesc;
import org.seasar.framework.beans.factory.BeanDescFactory;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.S2ContainerFactory;
import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.container.impl.S2ContainerBehavior;
import org.seasar.framework.convention.NamingConvention;
import org.seasar.framework.convention.impl.NamingConventionImpl;
import org.seasar.framework.unit.EasyMockSupport;
import org.seasar.framework.unit.InternalTestContext;
import org.seasar.framework.unit.S2TestIntrospector;
import org.seasar.framework.unit.UnitClassLoader;
import org.seasar.framework.unit.annotation.PublishedTestContext;
import org.seasar.framework.unit.impl.ConventionTestIntrospector;
import org.seasar.framework.util.DisposableUtil;
import org.seasar.framework.util.StringUtil;
import org.seasar.framework.util.tiger.CollectionsUtil;
import org.seasar.framework.util.tiger.ReflectionUtil;

/**
 * @author manhole
 */
public class S2TestRule implements TestRule {

    private final BeanDesc beanDesc;

    public S2TestRule(final Object testInstance) {
        this.test = testInstance;
        this.testClass = testInstance.getClass();
        beanDesc = BeanDescFactory.getBeanDesc(testClass);
    }

    public static S2TestRule create(final Object testInstance) {
        return new S2TestRule(testInstance);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        introspector = new ConventionTestIntrospector();
        this.method = findMethod(description.getMethodName());

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    S2TestRule.this.evaluate(base);
                } finally {
                    after();
                }
            }
        };
    }

    private Method findMethod(final String methodName) {
        Method found = null;
        final Method[] methods = beanDesc.getMethods(methodName);
        for (final Method m : methods) {
            if (m.getAnnotation(Test.class) != null) {
                if (found != null) {
                    // 同名の@Testメソッドが複数あったらエラー
                    throw new AssertionError();
                }
                found = m;
            }
        }
        return found;
    }


    protected void evaluate(final Statement base) throws Throwable {
        base.evaluate();
    }

    protected void before() throws Throwable {
        try {
            setUpTestContext();
        } catch (final Throwable th) {
            throw new AssertionError(th);
        }
        initContainer();
        bindFields();
    }

    protected void after() {
        try {
            unbindFields();
        } catch (final Throwable th) {
            throw new AssertionError(th);
        }

        testContext.destroyContainer();
        try {
            tearDownTestContext();
        } catch (final Throwable th) {
            throw new AssertionError(th);
        }
    }

    /**
     * S2JUnit4のデフォルトの設定ファイルのパス
     */
    protected static final String DEFAULT_S2JUNIT4_PATH = "s2junit4.dicon";

    /**
     * S2JUnit4の設定ファイルのパス
     */
    protected static String s2junit4Path = DEFAULT_S2JUNIT4_PATH;

    /**
     * テストオブジェクト
     */
    protected Object test;

    /**
     * テストクラス
     */
    protected Class<?> testClass;

    /**
     * テストメソッド
     */
    protected Method method;

    /**
     * ノティファイアー
     */
    protected RunNotifier notifier;

    /**
     * テストのディスクリプション
     */
    protected Description description;

    /**
     * テストクラスのイントロスペクター
     */
    protected S2TestIntrospector introspector;

    /**
     * {@link #unitClassLoader テストで使用するクラスローダー}で置き換えられる前のオリジナルのクラスローダー
     */
    protected ClassLoader originalClassLoader;

    /**
     * テストで使用するクラスローダー
     */
    protected UnitClassLoader unitClassLoader;

    /**
     * S2JUnit4の内部的なテストコンテキスト
     */
    protected InternalTestContext testContext;

    /**
     * バインディングが行われたフィールドのリスト
     */
    private List<Field> boundFields = CollectionsUtil.newArrayList();

    /**
     * EasyMockとの対話をサポートするオブジェクト
     */
    protected EasyMockSupport easyMockSupport = new EasyMockSupport();

    /**
     * テストが失敗したことを表すフラグ
     */
    protected boolean testFailed;

    // copy from: S2TestMethodRunner
    protected void setUpTestContext() throws Throwable {
        originalClassLoader = getOriginalClassLoader();
        unitClassLoader = new UnitClassLoader(originalClassLoader);
        Thread.currentThread().setContextClassLoader(unitClassLoader);
        // TODO warmDeployは不要で良いかな
//        if (needsWarmDeploy()) {
//            S2ContainerFactory.configure("warmdeploy.dicon");
//        }
        final S2Container container = createRootContainer();
        SingletonS2ContainerFactory.setContainer(container);
        testContext = InternalTestContext.class.cast(container
                .getComponent(InternalTestContext.class));
        testContext.setTestClass(testClass);
        testContext.setTestMethod(method);
        if (!testContext.hasComponentDef(NamingConvention.class)
                && introspector.isRegisterNamingConvention(testClass, method)) {
            final NamingConvention namingConvention = new NamingConventionImpl();
            testContext.register(namingConvention);
            testContext.setNamingConvention(namingConvention);
        }

        for (Class<?> clazz = testClass; clazz != Object.class; clazz = clazz
                .getSuperclass()) {

            final Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                final Field field = fields[i];
                final Class<?> fieldClass = field.getType();
                if (isAutoBindable(field)
                        && fieldClass.isAssignableFrom(testContext.getClass())
                        && fieldClass
                        .isAnnotationPresent(PublishedTestContext.class)) {
                    field.setAccessible(true);
                    if (ReflectionUtil.getValue(field, test) != null) {
                        continue;
                    }
                    bindField(field, testContext);
                }
            }
        }
    }

    protected void tearDownTestContext() throws Throwable {
        testContext = null;
        DisposableUtil.dispose();
        S2ContainerBehavior
                .setProvider(new S2ContainerBehavior.DefaultProvider());
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        unitClassLoader = null;
        originalClassLoader = null;
    }

    protected void initContainer() {
        testContext.include();
        introspector.createMock(method, test, testContext);
        testContext.initContainer();
    }

    protected void bindFields() throws Throwable {
        for (Class<?> clazz = testClass; clazz != Object.class; clazz = clazz
                .getSuperclass()) {
            final Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                bindField(fields[i]);
            }
        }
        List<Method> postBindFieldsMethods = introspector
                .getPostBindFieldsMethods(testClass);
        for (Method m : postBindFieldsMethods) {
            m.invoke(test);
        }
    }

    protected void bindField(final Field field) {
        if (isAutoBindable(field)) {
            field.setAccessible(true);
            if (ReflectionUtil.getValue(field, test) != null) {
                return;
            }
            final String name = resolveComponentName(field);
            Object component = null;
            if (testContext.hasComponentDef(name)) {
                component = testContext.getComponent(name);
                if (component != null) {
                    Class<?> componentClass = component.getClass();
                    if (!field.getType().isAssignableFrom(componentClass)) {
                        component = null;
                    }
                }
            }
            if (component == null
                    && testContext.hasComponentDef(field.getType())) {
                component = testContext.getComponent(field.getType());
            }
            if (component != null) {
                bindField(field, component);
            }
        }
    }

    protected void bindField(final Field field, final Object object) {
        ReflectionUtil.setValue(field, test, object);
        boundFields.add(field);
    }

    protected boolean isAutoBindable(final Field field) {
        final int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)
                && !field.getType().isPrimitive();
    }

    protected String resolveComponentName(final Field filed) {
//        if (testContext.isEjb3Enabled()) {
//            final EJB ejb = filed.getAnnotation(EJB.class);
//            if (ejb != null) {
//                if (!StringUtil.isEmpty(ejb.beanName())) {
//                    return ejb.beanName();
//                } else if (!StringUtil.isEmpty(ejb.name())) {
//                    return ejb.name();
//                }
//            }
//        }
        return normalizeName(filed.getName());
    }

    protected String normalizeName(final String name) {
        return StringUtil.replace(name, "_", "");
    }

    protected ClassLoader getOriginalClassLoader() {
        S2Container configurationContainer = S2ContainerFactory
                .getConfigurationContainer();
        if (configurationContainer != null
                && configurationContainer.hasComponentDef(ClassLoader.class)) {
            return ClassLoader.class.cast(configurationContainer
                    .getComponent(ClassLoader.class));
        }
        return Thread.currentThread().getContextClassLoader();
    }

    protected S2Container createRootContainer() {
        final String rootDicon = introspector.getRootDicon(testClass, method);
        if (StringUtil.isEmpty(rootDicon)) {
            return S2ContainerFactory.create(s2junit4Path);
        }
        S2Container container = S2ContainerFactory.create(rootDicon);
        S2ContainerFactory.include(container, s2junit4Path);
        return container;
    }

    protected void unbindFields() throws Throwable {
        List<Method> preUnbindFieldsMethods = introspector
                .getPreUnbindFieldsMethods(testClass);
        for (Method m : preUnbindFieldsMethods) {
            m.invoke(test);
        }
        for (final Field field : boundFields) {
            try {
                field.set(test, null);
            } catch (IllegalArgumentException e) {
                System.err.println(e);
            } catch (IllegalAccessException e) {
                System.err.println(e);
            }
        }
        boundFields = null;
    }

}
